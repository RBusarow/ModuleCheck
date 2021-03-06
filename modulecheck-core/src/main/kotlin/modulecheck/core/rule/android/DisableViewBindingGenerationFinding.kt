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

package modulecheck.core.rule.android

import modulecheck.api.Finding
import modulecheck.api.Fixable
import modulecheck.core.kotlinBuildFileOrNull
import modulecheck.psi.AndroidBuildFeaturesVisitor
import modulecheck.psi.PsiElementWithSurroundingText
import java.io.File

data class DisableViewBindingGenerationFinding(
  override val dependentPath: String,
  override val buildFile: File
) : Finding, Fixable {

  override val problemName = "unused ViewBinding generation"

  override val dependencyIdentifier = ""

  override fun elementOrNull(): PsiElementWithSurroundingText? {
    val buildFile = kotlinBuildFileOrNull() ?: return null

    return AndroidBuildFeaturesVisitor().find(buildFile, "viewBinding")
  }

  override fun positionOrNull(): Finding.Position? {
    val ktFile = kotlinBuildFileOrNull() ?: return null

    return androidBlockParser.parse(ktFile)?.let { result ->

      val token = result
        .blockText
        .lines()
        .firstOrNull { it.isNotEmpty() } ?: return@let null

      val lines = ktFile.text.lines()

      val startRow = lines.indexOfFirst { it.matches(androidBlockRegex) }

      if (startRow == -1) return@let null

      val after = lines.subList(startRow, lines.lastIndex)

      val row = after.indexOfFirst { it.contains(token) }

      Finding.Position(row + startRow + 1, 0)
    }
  }

  override fun fix(): Boolean = synchronized(buildFile) {
    val ktFile = kotlinBuildFileOrNull() ?: return false

    val oldBlock = elementOrNull()?.toString() ?: return false

    if (!oldBlock.contains("viewBinding = true")) return false

    val newBlock = oldBlock.replace("viewBinding = true", "viewBinding = false")

    val oldText = ktFile.text

    buildFile.writeText(oldText.replace(oldBlock, newBlock))

    return true
  }
}
