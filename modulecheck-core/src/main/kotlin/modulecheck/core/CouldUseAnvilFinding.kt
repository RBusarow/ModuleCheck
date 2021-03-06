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

import modulecheck.api.Finding
import modulecheck.api.Fixable
import modulecheck.api.asConfigurationName
import modulecheck.api.files.existsOrNull
import modulecheck.core.internal.positionOf
import modulecheck.psi.PsiElementWithSurroundingText
import java.io.File

data class CouldUseAnvilFinding(
  override val buildFile: File,
  override val dependentPath: String
) : Finding, Fixable {

  override val dependencyIdentifier = "com.google.dagger:dagger-compiler"
  override val problemName = "could use Anvil factory generator"

  override fun elementOrNull(): PsiElementWithSurroundingText? = null

  override fun positionOrNull(): Finding.Position? {
    val element = elementOrNull() ?: return null

    return buildFile
      .existsOrNull()
      ?.readText()
      ?.lines()
      ?.positionOf(element.toString(), "kapt".asConfigurationName())
  }
}
