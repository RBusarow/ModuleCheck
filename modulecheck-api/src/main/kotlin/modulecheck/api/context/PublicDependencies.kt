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

package modulecheck.api.context

import modulecheck.api.ConfigurationName
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2

data class PublicDependencies(
  internal val delegate: Set<ConfiguredProjectDependency>
) : Set<ConfiguredProjectDependency> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<PublicDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<PublicDependencies> {
    override operator fun invoke(project: Project2): PublicDependencies {
      return PublicDependencies(
        project.allPublicClassPathDependencyDeclarations()
      )
    }

    private fun Project2.allPublicClassPathDependencyDeclarations(
      includePrivate: Boolean = true
    ): Set<ConfiguredProjectDependency> {
      val privateDependencies = if (includePrivate) {
        projectDependencies.value.main()
      } else {
        emptyList()
      }

      val combined = privateDependencies + projectDependencies
        .value[ConfigurationName.api]
        .orEmpty()

      val inherited = combined
        .flatMap { cpd ->
          cpd
            .project
            .allPublicClassPathDependencyDeclarations(false)
        }

      return inherited
        .plus(combined)
        .toSet()
    }
  }
}
