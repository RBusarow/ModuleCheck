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

package modulecheck.api.psi

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry

internal val jvmSuppressWildcardsFqName = FqName(JvmSuppressWildcards::class.java.canonicalName)
internal val publishedApiFqName = FqName(PublishedApi::class.java.canonicalName)

private val kotlinAnnotations = listOf(jvmSuppressWildcardsFqName, publishedApiFqName)

internal fun KtAnnotated.hasAnnotation(fqName: FqName): Boolean {
  return findAnnotation(fqName) != null
}

internal fun KtAnnotated.findAnnotation(fqName: FqName): KtAnnotationEntry? {
  val annotationEntries = annotationEntries
  if (annotationEntries.isEmpty()) return null

  // Look first if it's a Kotlin annotation. These annotations are usually not imported and the
  // remaining checks would fail.
  if (fqName in kotlinAnnotations) {
    annotationEntries.firstOrNull { annotation ->
      val text = annotation.text
      text.startsWith("@${fqName.shortName()}") || text.startsWith("@$fqName")
    }?.let { return it }
  }

  // Check if the fully qualified name is used, e.g. `@dagger.Module`.
  val annotationEntry = annotationEntries.firstOrNull {
    it.text.startsWith("@${fqName.asString()}")
  }
  if (annotationEntry != null) return annotationEntry

  // Check if the simple name is used, e.g. `@Module`.
  val annotationEntryShort = annotationEntries
    .firstOrNull {
      it.shortName == fqName.shortName()
    }
    ?: return null

  val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

  // If the simple name is used, check that the annotation is imported.
  val hasImport = importPaths.any { it.fqName == fqName }
  if (hasImport) return annotationEntryShort

  // Look for star imports and make a guess.
  val hasStarImport = importPaths
    .filter { it.isAllUnder }
    .any {
      fqName.asString().startsWith(it.fqName.asString())
    }
  if (hasStarImport) return annotationEntryShort

  return null
}
