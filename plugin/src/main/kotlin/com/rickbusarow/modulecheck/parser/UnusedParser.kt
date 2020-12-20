package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.DependencyFinding
import com.rickbusarow.modulecheck.MCP

object UnusedParser : Parser<DependencyFinding.UnusedDependency>() {

  override fun parse(mcp: MCP): MCP.Parsed<DependencyFinding.UnusedDependency> {

    val dependencies = mcp.dependencies

    val unusedHere = dependencies
      .all()
      .filter { cpp ->
        !cpp.usedIn(mcp)
      }

    val dependents = mcp.dependents()

    val unusedMain = dependencies
      .main()
      .filter { it !in mcp.resolvedMainDependencies }

    /*
    If a module doesn't use a dependency,
    but it's an api dependency,
    and ALL dependents of that module use it,
    then ignore the fact that it's unused in the current module.
     */
    val unusedInAtLeastOneDependent = unusedHere
      .filter { cpp ->
        cpp.config != Config.Api || dependents.any { dependent ->
          !cpp.usedIn(dependent)
        }
      }

    val grouped = unusedInAtLeastOneDependent.map { cpp ->

      DependencyFinding.UnusedDependency(
        mcp.project,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    val newGrouped = unusedMain.map { cpp ->

      DependencyFinding.UnusedDependency(
        mcp.project,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    return MCP.Parsed(
      grouped.getOrDefault(Config.AndroidTest, mutableSetOf()),
      newGrouped.getOrDefault(Config.Api, mutableSetOf()),
      newGrouped.getOrDefault(Config.CompileOnly, mutableSetOf()),
      newGrouped.getOrDefault(Config.Implementation, mutableSetOf()),
      newGrouped.getOrDefault(Config.RuntimeOnly, mutableSetOf()),
      grouped.getOrDefault(Config.TestApi, mutableSetOf()),
      grouped.getOrDefault(Config.TestImplementation, mutableSetOf())
    )
  }
}
