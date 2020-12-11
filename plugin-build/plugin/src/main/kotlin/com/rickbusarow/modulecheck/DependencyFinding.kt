package com.rickbusarow.modulecheck

import org.gradle.api.Project

sealed class DependencyFinding(val problemName: String) {
  abstract val dependentProject: Project
  abstract val dependencyProject: Project
  abstract val dependencyPath: String
  abstract val config: Config

  data class UnusedDependency(
    override val dependentProject: Project,
    override val dependencyProject: Project,
    override val dependencyPath: String,
    override val config: Config
  ) : DependencyFinding("unused") {
    fun cpp() = CPP(config, dependencyProject)
  }

  data class OverShotDependency(
    override val dependentProject: Project,
    override val dependencyProject: Project,
    override val dependencyPath: String,
    override val config: Config,
    val from: MCP?
  ) : DependencyFinding("over-shot") {

    override fun position(): MCP.Position {
      return from?.positionIn(dependentProject.project, config.name)
        ?: MCP.Position(-1, -1)
    }

    override fun logString(): String = super.logString() + " from: ${from?.path}"

    override fun fix() {

      val text = dependentProject.buildFile.readText()

      val row = position().row - 1

      val lines = text.lines().toMutableList()

      if (row > 0 && from != null) {

        val existingPath = from.path

        val existingLine = lines[row]

        lines[row] = existingLine + "\n" + existingLine.replace(existingPath, dependencyPath)

        val newText = lines.joinToString("\n")

        dependentProject.buildFile.writeText(newText)
      }
    }
  }

  data class RedundantDependency(
    override val dependentProject: Project,
    override val dependencyProject: Project,
    override val dependencyPath: String,
    override val config: Config,
    val from: List<Project>
  ) : DependencyFinding("redundant") {
    override fun logString(): String = super.logString() + " from: ${from.joinToString { it.path }}"

  }

  open fun position(): MCP.Position {
    return MCP.from(dependencyProject)
      .positionIn(dependentProject, config.name)
  }

  open fun logString(): String {

    val pos = if (position().row == 0 || position().column == 0) {
      ""
    } else {
      "(${position().row}, ${position().column}): "
    }

    return "${dependentProject.buildFile.path}: $pos${dependencyPath}"
  }

  open fun fix() {

    val text = dependentProject.buildFile.readText()

    val row = position().row - 1

    val lines = text.lines().toMutableList()

    if (row > 0) {

      lines[row] = "//" + lines[row]

      val newText = lines.joinToString("\n")

      dependentProject.buildFile.writeText(newText)
    }
  }
}
