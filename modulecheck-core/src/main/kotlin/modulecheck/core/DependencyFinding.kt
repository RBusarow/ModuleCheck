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

package modulecheck.core

import modulecheck.api.Config
import modulecheck.api.Finding
import modulecheck.api.Fixable
import modulecheck.api.Project2
import modulecheck.api.psi.PsiElementWithSurroundingText

abstract class DependencyFinding(
  override val problemName: String
) : Fixable,
  Finding {

  override val path get() = dependencyProject.path
  override val buildFile get() = dependencyProject.buildFile

  abstract val dependencyProject: Project2
  abstract val config: Config

  override fun elementOrNull(): PsiElementWithSurroundingText? {
    return MCP.from(dependencyProject)
      .psiElementIn(buildFile, config)
  }

  override fun positionOrNull(): Finding.Position? {
    return MCP.from(dependencyProject)
      .positionIn(buildFile, config)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DependencyFinding) return false

    if (problemName != other.problemName) return false
    if (dependencyProject != other.dependencyProject) return false
    if (config != other.config) return false

    return true
  }

  override fun hashCode(): Int {
    var result = problemName.hashCode()
    result = 31 * result + dependencyProject.hashCode()
    result = 31 * result + config.hashCode()
    return result
  }
}
