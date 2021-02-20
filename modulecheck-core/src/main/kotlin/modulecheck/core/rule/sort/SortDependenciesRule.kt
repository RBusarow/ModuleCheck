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

package modulecheck.core.rule.sort

import modulecheck.api.Project2
import modulecheck.api.psi.PsiElementWithSurroundingText
import modulecheck.core.rule.AbstractRule
import modulecheck.psi.DslBlockVisitor
import java.util.*

class SortDependenciesRule(
  project: Project2,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>,
  val visitor: DslBlockVisitor,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : AbstractRule<SortDependenciesFinding>(
  project, alwaysIgnore, ignoreAll
) {
  override fun check(): List<SortDependenciesFinding> {
    val kotlinBuildFile = kotlinBuildFileOrNull() ?: return emptyList()

    val result = visitor.parse(kotlinBuildFile) ?: return emptyList()

    val sorted = result
      .elements
      .grouped()
      .joinToString("\n\n") { list ->
        list
          .sortedWith(comparator)
          .joinToString("\n")
      }
      .trim()

    return if (result.blockText == sorted) {
      emptyList()
    } else {
      listOf(SortDependenciesFinding(project.buildFile, visitor, comparator))
    }
  }

  companion object {
    val patterns = listOf(
      """id\("com\.android.*"\)""",
      """id\("android-.*"\)""",
      """id\("java-library"\)""",
      """kotlin\("jvm"\)""",
      """android.*""",
      """javaLibrary.*""",
      """kotlin.*""",
      """id.*"""
    )
  }
}
