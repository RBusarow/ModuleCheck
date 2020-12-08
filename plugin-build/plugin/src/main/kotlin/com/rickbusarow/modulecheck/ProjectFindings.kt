package com.rickbusarow.modulecheck

import kotlin.reflect.KProperty1

class ProjectFindings(val project: ModuleCheckProject) {

  fun getMainDepth(): Int {
    val all =
      project.dependencies.compileOnlyDependencies + project.dependencies.apiDependencies + project.dependencies.implementationDependencies

    return if (all.isEmpty()) 0
    else all.map { ModuleCheckProject.from(it.project).findings.getMainDepth() }.max()!! + 1
  }

  fun getTestDepth(): Int = if (project.dependencies.testImplementationDependencies.isEmpty()) {
    0
  } else {
    project.dependencies.testImplementationDependencies.map {
      ModuleCheckProject.from(it.project).findings.getMainDepth()
    }.max()!! + 1
  }

  val androidTestDepth: Int
    get() = if (project.dependencies.androidTestImplementationDependencies.isEmpty()) {
      0
    } else {
      project.dependencies.androidTestImplementationDependencies
        .map { ModuleCheckProject.from(it.project).findings.getMainDepth() }
        .max()!! + 1
    }

  fun unusedAndroidTest() = findUnused(
    project.androidTestImports,
    ProjectDependencies::androidTestImplementationDependencies,
    "androidTest"
  )

  fun unusedApi() = findUnused(
    project.mainImports, ProjectDependencies::apiDependencies, "api"
  )

  fun unusedCompileOnly() = findUnused(
    project.mainImports, ProjectDependencies::compileOnlyDependencies, "compileOnly"
  )

  fun unusedImplementation() = findUnused(
    project.mainImports, ProjectDependencies::implementationDependencies, "implementation"
  )

  fun unusedTestImplementation() = findUnused(
    project.testImports,
    ProjectDependencies::testImplementationDependencies,
    "testImplementation"
  )

  private fun findUnused(
    imports: Set<String>,
    dependencyKProp: KProperty1<ProjectDependencies, List<ModuleCheckProject>>,
    configurationName: String
  ): List<DependencyFinding.UnusedDependency> =
    dependencyKProp(project.dependencies)
      // .filterNot { alwaysIgnore.contains(it.project.path)}
      .mapNotNull { projectDependency ->

        val used = imports.any { importString ->
          when {
            importString in projectDependency.mainPackages -> true
            else ->
              dependencyKProp(project.dependencies).any { childProjectDependency ->

                projectDependency in dependencyKProp(childProjectDependency.dependencies)
              }
          }
        }

        if (!used) {
          DependencyFinding.UnusedDependency(
            project.project,
            projectDependency.project,
            projectDependency.project.path,
            configurationName
          )
        } else null
      }

  fun overshotApiDependencies(): List<DependencyFinding.OverShotDependency> {

    val allBad = (unusedApi() + unusedCompileOnly() + unusedImplementation())
      .map { ModuleCheckProject.from(it.dependencyProject) }.toSet()

    return project.allPublicClassPathDependencyDeclarations()
      .asSequence()
      .filterNot { it in allBad }
      .filterNot { it in project.dependencies.mainDependencies }
      .filter { inheritedNewProject ->
        inheritedNewProject.mainPackages.any { newProjectPackage ->
          newProjectPackage in project.mainImports
        }
      }
      .groupBy { it.project }
      .map { (overshot, _) ->

        val source = project.dependencies.sourceOf(ModuleCheckProject.from(overshot))

        DependencyFinding.OverShotDependency(
          project.project,
          overshot,
          overshot.path,
          "api",
          source
        )
      }
      .toList()
  }


  fun overshotImplementationDependencies(): List<DependencyFinding.OverShotDependency> {

    val allBad = (unusedApi() + unusedCompileOnly() + unusedImplementation())
      .map { ModuleCheckProject.from(it.dependencyProject) }.toSet()

    return project.allPublicClassPathDependencyDeclarations()
      .asSequence()
      .filterNot { it in allBad }
      .filterNot { it in project.dependencies.mainDependencies }
      .filter { inheritedNewProject ->
        inheritedNewProject.mainPackages.any { newProjectPackage ->
          newProjectPackage in project.mainImports
        }
      }
      .groupBy { it.project }
      .map { (overshot, _) ->

        val source = project.dependencies.sourceOf(ModuleCheckProject.from(overshot))

        DependencyFinding.OverShotDependency(
          project.project,
          overshot,
          overshot.path,
          "implementation",
          source
        )
      }
      .toList()
  }

  fun redundantAndroidTest(): List<DependencyFinding.RedundantDependency> {

    val inheritedDependencyProjects = project.dependencies.androidTestImplementationDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      }

    return project.dependencies.androidTestImplementationDependencies
      .filter { it.project in inheritedDependencyProjects }
      .map {
        val from = project.dependencies.androidTestImplementationDependencies
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "androidTest",
          from
        )
      }
      .distinctBy { it.position() }
  }

  fun redundantApi(): List<DependencyFinding.RedundantDependency> {

    val allMain = project.dependencies.apiDependencies.toSet()

    val inheritedDependencyProjects = project.dependencies.apiDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      } + project.dependencies.mainDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      }


    return allMain
      .filter { it.project in inheritedDependencyProjects }
      .map {

        val from = allMain
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "api",
          from
        )
      }
  }

  fun redundantCompileOnly(): List<DependencyFinding.RedundantDependency> {

    val allMain = project.dependencies.compileOnlyDependencies.toSet()

    val inheritedDependencyProjects = project.dependencies.compileOnlyDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      } + project.dependencies.mainDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      }


    return allMain
      .filter { it.project in inheritedDependencyProjects }
      .map {

        val from = allMain
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "compileOnly",
          from
        )
      }
  }

  fun redundantImplementation(): List<DependencyFinding.RedundantDependency> {

    val allMain = project.dependencies.implementationDependencies.toSet()

    val inheritedDependencyProjects = project.dependencies.implementationDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      } + project.dependencies.mainDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      }

    return allMain
      .filter { it.project in inheritedDependencyProjects }
      .map {

        val from = allMain
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "implementation",
          from
        )
      }
  }

  fun redundantTest(): List<DependencyFinding.RedundantDependency> {
    val inheritedDependencyProjects = project.dependencies.testImplementationDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      } + project.dependencies.mainDependencies
      .flatMap {
        it.inheritedMainDependencyProjects()
          .map { it.project }
          .toSet()
      }


    return project.dependencies.testImplementationDependencies
      .filter { it.project in inheritedDependencyProjects }
      .map {
        val from = project.dependencies.testImplementationDependencies
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }
        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "testImplementation",
          from
        )
      }
  }

}