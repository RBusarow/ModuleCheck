/*
 * Copyright (C) 2021 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modulecheck.gradle

import com.android.Version
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.squareup.anvil.plugin.AnvilExtension
import modulecheck.api.*
import modulecheck.api.anvil.AnvilGradlePlugin
import modulecheck.core.android.AndroidManifestParser
import modulecheck.core.rule.KAPT_PLUGIN_ID
import modulecheck.gradle.internal.existingFiles
import modulecheck.gradle.internal.srcRoot
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.ExternalDependencyDeclarationVisitor
import modulecheck.psi.internal.asKtFile
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.NONE

class GradleProjectProvider(
  private val rootGradleProject: GradleProject,
  override val projectCache: ConcurrentHashMap<String, Project2>
) : ProjectProvider {

  private val gradleProjects = rootGradleProject.allprojects.associateBy { it.path }

  private val agpVersion: SemVer by lazy(NONE) { SemVer.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION) }

  override fun get(path: String): Project2 {
    return projectCache.getOrPut(path) {
      createProject(path)
    }
  }

  @Suppress("UnstableApiUsage")
  private fun createProject(path: String): Project2 {
    val gradleProject = gradleProjects.getValue(path)

    val configurations = gradleProject.configurations()

    val projectDependencies = gradleProject.projectDependencies()
    val hasKapt = gradleProject
      .plugins
      .hasPlugin(KAPT_PLUGIN_ID)
    val sourceSets = gradleProject
      .jvmSourceSets()

    val testedExtension = gradleProject
      .extensions
      .findByType<LibraryExtension>()
      ?: gradleProject
        .extensions
        .findByType<AppExtension>()

    val isAndroid = testedExtension != null

    val libraryExtension by lazy(NONE) {
      gradleProject
        .extensions
        .findByType<LibraryExtension>()
    }

    return if (isAndroid) {
      AndroidProject2Impl(
        path = path,
        projectDir = gradleProject.projectDir,
        buildFile = gradleProject.buildFile,
        configurations = configurations,
        projectDependencies = projectDependencies,
        hasKapt = hasKapt,
        sourceSets = gradleProject.androidSourceSets(),
        projectCache = projectCache,
        anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
        agpVersion = agpVersion,
        androidResourcesEnabled = libraryExtension?.buildFeatures?.androidResources != false,
        viewBindingEnabled = testedExtension?.buildFeatures?.viewBinding == true,
        resourceFiles = gradleProject.androidResourceFiles(),
        androidPackageOrNull = gradleProject.androidPackageOrNull()
      )
    } else {
      Project2Impl(
        path = path,
        projectDir = gradleProject.projectDir,
        buildFile = gradleProject.buildFile,
        configurations = configurations,
        projectDependencies = projectDependencies,
        hasKapt = hasKapt,
        sourceSets = sourceSets,
        projectCache = projectCache,
        anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull()
      )
    }
  }

  private fun GradleProject.configurations(): Map<ConfigurationName, Config> {
    val externalDependencyCache = mutableMapOf<String, Set<ExternalDependency>>()

    fun Configuration.foldConfigs(): Set<Configuration> {
      return extendsFrom + extendsFrom.flatMap { it.foldConfigs() }
    }

    fun Configuration.toConfig(): Config {
      val external = externalDependencyCache.getOrPut(name) {
        externalDependencies(this)
      }

      val configs = foldConfigs()
        .map { it.toConfig() }
        .toSet()

      return Config(name.asConfigurationName(), external, configs)
    }
    return configurations
      .filterNot { it.name == ScriptHandler.CLASSPATH_CONFIGURATION }
      .associate { configuration ->

        val config = configuration.toConfig()

        configuration.name.asConfigurationName() to config
      }
  }

  private fun GradleProject.externalDependencies(configuration: Configuration) =
    configuration.dependencies
      .filterNot { it is ProjectDependency }
      .map { dep ->
        val psi = lazy psiLazy@{
          val parsed = DslBlockVisitor("dependencies")
            .parse(buildFile.asKtFile())
            ?: return@psiLazy null

          parsed
            .elements
            .firstOrNull { element ->

              val p = ExternalDependencyDeclarationVisitor(
                configuration = configuration.name,
                group = dep.group,
                name = dep.name,
                version = dep.version
              )

              p.find(element.psiElement as KtCallExpression)
            }
        }
        ExternalDependency(
          configurationName = configuration.name.asConfigurationName(),
          group = dep.group,
          moduleName = dep.name,
          version = dep.version,
          psiElementWithSurroundingText = psi
        )
      }
      .toSet()

  private fun GradleProject.projectDependencies(): Lazy<ProjectDependencies> =
    lazy {
      val map = configurations
        .map { config ->
          config.name.asConfigurationName() to config.dependencies.withType(ProjectDependency::class.java)
            .map {
              ConfiguredProjectDependency(
                configurationName = config.name.asConfigurationName(),
                project = get(it.dependencyProject.path)
              )
            }
        }
        .toMap()
      ProjectDependencies(map)
    }

  private fun GradleProject.jvmSourceSets(): Map<SourceSetName, SourceSet> = convention
    .findPlugin(JavaPluginConvention::class)
    ?.sourceSets
    ?.map {
      val jvmFiles = (
        (it as? HasConvention)
          ?.convention
          ?.plugins
          ?.get("kotlin") as? KotlinSourceSet
        )
        ?.kotlin
        ?.sourceDirectories
        ?.files
        ?: it.allJava.files

      SourceSet(
        name = it.name.toSourceSetName(),
        classpathFiles = it.compileClasspath.existingFiles().files,
        outputFiles = it.output.classesDirs.existingFiles().files,
        jvmFiles = jvmFiles,
        resourceFiles = it.resources.sourceDirectories.files
      )
    }
    ?.associateBy { it.name }
    .orEmpty()

  private fun GradleProject.anvilGradlePluginOrNull(): AnvilGradlePlugin? {
    /*
    Before Kotlin 1.5.0, Anvil was applied to the `kotlinCompilerPluginClasspath` config.

    In 1.5.0+, it's applied to individual source sets, such as
    `kotlinCompilerPluginClasspathMain`, `kotlinCompilerPluginClasspathTest`, etc.
     */
    val version = configurations
      .filter { it.name.startsWith("kotlinCompilerPluginClasspath") }
      .asSequence()
      .flatMap { it.dependencies }
      .firstOrNull { it.group == "com.squareup.anvil" }
      ?.version
      ?.let { versionString -> SemVer.parse(versionString) }
      ?: return null

    val enabled = extensions
      .findByType<AnvilExtension>()
      ?.generateDaggerFactories == true

    return AnvilGradlePlugin(version, enabled)
  }

  private fun GradleProject.androidPackageOrNull(): String? {
    val manifest = File("$srcRoot/main/AndroidManifest.xml".replace("/", File.separator))

    if (!manifest.exists()) return null

    return AndroidManifestParser.parse(manifest)["package"]
  }

  private fun GradleProject.androidResourceFiles(): Set<File> {
    val testedExtension =
      extensions.findByType<LibraryExtension>()
        ?: extensions.findByType<AppExtension>()

    return testedExtension
      ?.sourceSets
      ?.flatMap { sourceSet ->
        sourceSet.res.getSourceFiles().toList()
      }
      .orEmpty()
      .toSet()
  }

  private val BaseExtension.variants: DomainObjectSet<out BaseVariant>?
    get() = when (this) {
      is AppExtension -> applicationVariants
      is LibraryExtension -> libraryVariants
      is TestExtension -> applicationVariants
      else -> null
    }

  private val BaseVariant.testVariants: List<BaseVariant>
    get() = when (this) {
      is TestedVariant -> listOfNotNull(testVariant, unitTestVariant)
      else -> emptyList()
    }

  private fun GradleProject.androidSourceSets(): Map<SourceSetName, SourceSet> {
    return extensions
      .findByType<BaseExtension>()
      ?.variants
      ?.flatMap { variant ->

        val testSourceSets = variant
          .testVariants
          .flatMap { it.sourceSets }

        val mainSourceSets = variant.sourceSets

        (testSourceSets + mainSourceSets)
          .distinctBy { it.name }
          .map { sourceProvider ->

            val jvmFiles = sourceProvider
              .javaDirectories
              .flatMap { it.listFiles().orEmpty().toList() }
              .toSet()

            // val bootClasspath = project.files(baseExtension!!.bootClasspath)
            // val classPath = variant
            //   .getCompileClasspath(null)
            //   .filter { it.exists() }
            //   .plus(bootClasspath)
            //   .toSet()

            val resourceFiles = sourceProvider
              .resDirectories
              .flatMap { it.listFiles().orEmpty().toList() }
              .flatMap { it.listFiles().orEmpty().toList() }
              .toSet()

            val layoutFiles = resourceFiles
              .filter {
                it.isFile && it.path
                  .replace(File.separator, "/") // replace `\` from Windows paths with `/`.
                  .contains("""/res/layout.*/.*.xml""".toRegex())
              }
              .toSet()

            SourceSet(
              name = sourceProvider.name.toSourceSetName(),
              classpathFiles = emptySet(),
              outputFiles = setOf(), // TODO
              jvmFiles = jvmFiles,
              resourceFiles = resourceFiles,
              layoutFiles = layoutFiles
            )
          }
      }

      ?.associateBy { it.name }
      .orEmpty()
  }
}
