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

import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

typealias ImportName = String

data class Imports(
  internal val delegate: ConcurrentMap<SourceSetName, Set<ImportName>>
) : ConcurrentMap<SourceSetName, Set<ImportName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<Imports>
    get() = Key

  companion object Key : ProjectContext.Key<Imports> {
    override operator fun invoke(project: Project2): Imports {
      val ss = project.sourceSets

      val map = ss
        .mapValues { (name, _) ->

          val jvm = project[JvmFiles][name]
            .orEmpty()
            .flatMap { it.imports }
            .toSet()
          val layout = project[LayoutFiles][name]
            .orEmpty()
            .flatMap { it.customViews }
            .toSet()

          val combined = jvm + layout

          combined
        }

      return Imports(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.imports: Imports get() = get(Imports)
fun ProjectContext.importsForSourceSetName(sourceSetName: SourceSetName): Set<ImportName> {
  return imports[sourceSetName].orEmpty()
}
