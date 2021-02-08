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

package com.rickbusarow.modulecheck.kapt

import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.Position
import org.gradle.api.Project

interface UnusedKaptFinding : Finding, Fixable

data class UnusedKaptPluginFinding(
  override val dependentProject: Project
) : UnusedKaptFinding {

  override val dependencyIdentifier = KAPT_PLUGIN_ID

  override val problemName = "unused kapt plugin"

  override fun positionOrNull(): Position? {
    val text = dependentProject
      .buildFile
      .readText()

    val lines = text.lines()

    val row = lines
      .indexOfFirst { line ->
        line.contains("id(\"$KAPT_PLUGIN_ID\")") ||
          line.contains(KAPT_PLUGIN_FUN) ||
          line.contains("plugin = \"$KAPT_PLUGIN_ID\")")
      }

    if (row < 0) return null

    val col = lines[row]
      .indexOfFirst { it != ' ' }

    return Position(row + 1, col + 1)
  }
}
