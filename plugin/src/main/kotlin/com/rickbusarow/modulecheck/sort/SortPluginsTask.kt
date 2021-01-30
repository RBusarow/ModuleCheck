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

package com.rickbusarow.modulecheck.sort

import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.Position
import com.rickbusarow.modulecheck.internal.asKtFile
import com.rickbusarow.modulecheck.parser.DslBlockParser
import com.rickbusarow.modulecheck.parser.PsiElementWithSurroundingText
import com.rickbusarow.modulecheck.rule.AbstractRule
import com.rickbusarow.modulecheck.sort.SortPluginsRule.Companion.patterns
import com.rickbusarow.modulecheck.task.AbstractModuleCheckTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class SortPluginsTask : AbstractModuleCheckTask() {

/*  @TaskAction
  fun run() {
    val parser = DslBlockParser("plugins")

    val comparables: Array<(PsiElementWithSurroundingText) -> Comparable<*>> = patterns
      .map { it.toRegex() }
      .map { regex ->
        { str: String -> !str.matches(regex) }
      }
      .map { booleanLambda ->
        { psi: PsiElementWithSurroundingText ->

          booleanLambda.invoke(psi.psiElement.text)
        }
      }.toTypedArray()

    @Suppress("SpreadOperator")
    val comparator: Comparator<PsiElementWithSurroundingText> = compareBy(*comparables)

    project
      .allprojects
      .filter { it.buildFile.exists() }
      .forEach { sub ->

        SortPluginsRule(
          project = sub,
          alwaysIgnore = alwaysIgnore.get(),
          ignoreAll = ignoreAll.get(),
          parser = parser,
          comparator = comparator
        )
          .check()
      }
  }*/
}

class SortPluginsFinding(
  override val dependentProject: Project,
  val parser: DslBlockParser,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : Finding, Fixable {
  override val problemName = "unsorted plugins"

  override val dependencyIdentifier = ""

  override fun position(): Position? = null

  override fun fix() {

    val result = parser.parse(dependentProject.buildFile.asKtFile()) ?: return

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    val allText = dependentProject.buildFile.readText()

    val newText = allText.replace(result.blockText, sorted)

    dependentProject.buildFile.writeText(newText)
  }
}

class SortPluginsRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>,
  val parser: DslBlockParser,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : AbstractRule<SortPluginsFinding>(
  project, alwaysIgnore, ignoreAll
) {
  override fun check(): List<SortPluginsFinding> {

    val result = parser.parse(project.buildFile.asKtFile()) ?: return emptyList()

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    return if (result.blockText == sorted)
      emptyList()
    else
      listOf(SortPluginsFinding(project, parser, comparator))
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
