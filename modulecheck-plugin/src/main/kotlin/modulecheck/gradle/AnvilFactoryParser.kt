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

import modulecheck.api.Project2
import modulecheck.api.anvil.anvilMergeComponentFqName
import modulecheck.api.anvil.daggerComponentFqName
import modulecheck.api.anvil.daggerModuleFqName
import modulecheck.api.anvil.injectFqName
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.context.possibleReferencesForSourceSetName
import modulecheck.api.files.JavaFile
import modulecheck.api.files.KotlinFile
import modulecheck.api.toSourceSetName
import modulecheck.core.CouldUseAnvilFinding
import net.swiftzer.semver.SemVer
import kotlin.LazyThreadSafetyMode.NONE

object AnvilFactoryParser {

  @Suppress("MagicNumber")
  private val minimumAnvilVersion = SemVer(2, 0, 11)

  @Suppress("ComplexMethod")
  fun parse(project: Project2): List<CouldUseAnvilFinding> {
    val anvil = project.anvilGradlePlugin ?: return emptyList()

    if (anvil.generateDaggerFactories) return emptyList()

    val anvilVersion = anvil.version

    val hasAnvil = anvilVersion >= minimumAnvilVersion

    if (!hasAnvil) return emptyList()

    val allImports = project.importsForSourceSetName("main".toSourceSetName()) +
      project.importsForSourceSetName("androidTest".toSourceSetName()) +
      project.importsForSourceSetName("test".toSourceSetName())

    val maybeExtra by lazy(NONE) {
      project.possibleReferencesForSourceSetName("androidTest".toSourceSetName()) +
        project.possibleReferencesForSourceSetName("main".toSourceSetName()) +
        project.possibleReferencesForSourceSetName("test".toSourceSetName())
    }

    val createsComponent = allImports.contains(daggerComponentFqName) ||
      allImports.contains(anvilMergeComponentFqName) ||
      maybeExtra.contains(daggerComponentFqName) ||
      maybeExtra.contains(anvilMergeComponentFqName)

    if (createsComponent) return emptyList()

    val usesDaggerInJava = project
      .jvmFilesForSourceSetName("main".toSourceSetName())
      .filterIsInstance<JavaFile>()
      .any { file ->
        file.imports.contains(injectFqName) ||
          file.imports.contains(daggerModuleFqName) ||
          file.maybeExtraReferences.contains(injectFqName) ||
          file.maybeExtraReferences.contains(daggerModuleFqName)
      }

    if (usesDaggerInJava) return emptyList()

    val usesDaggerInKotlin = project
      .jvmFilesForSourceSetName("main".toSourceSetName())
      .filterIsInstance<KotlinFile>()
      .any { file ->
        file.imports.contains(injectFqName) ||
          file.imports.contains(daggerModuleFqName) ||
          file.maybeExtraReferences.contains(injectFqName) ||
          file.maybeExtraReferences.contains(daggerModuleFqName)
      }

    if (!usesDaggerInKotlin) return emptyList()

    val couldBeAnvil =
      !allImports.contains(daggerComponentFqName) && !maybeExtra.contains(daggerComponentFqName)

    return if (couldBeAnvil) {
      listOf(CouldUseAnvilFinding(project.buildFile, project.path))
    } else {
      listOf()
    }
  }
}
