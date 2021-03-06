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
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ClassPaths(
  internal val delegate: ConcurrentMap<SourceSetName, Set<File>>
) : ConcurrentMap<SourceSetName, Set<File>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ClassPaths>
    get() = Key

  companion object Key : ProjectContext.Key<ClassPaths> {
    override operator fun invoke(project: Project2): ClassPaths {
      val map = project
        .sourceSets
        .values
        .map { sourceSet ->
          sourceSet.name to (sourceSet.classpathFiles + sourceSet.outputFiles).toSet()
        }
        .toMap()

      return ClassPaths(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.classPaths: ClassPaths get() = get(ClassPaths)
fun ProjectContext.classpathForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  classPaths[sourceSetName].orEmpty()
