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

import modulecheck.api.Project2
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal fun KtNamedDeclaration.requireFqName(): FqName = requireNotNull(fqName) {
  "fqName was null for $this, $nameAsSafeName"
}

internal fun PsiElement.requireFqName(
  module: Project2
): FqName {
  val containingKtFile = parentsWithSelf
    .filterIsInstance<KtPureElement>()
    .first()
    .containingKtFile

  fun failTypeHandling(): Nothing = throw Exception(
    "Don't know how to handle Psi element: $text"
  )

  val classReference = when (this) {
    // If a fully qualified name is used, then we're done and don't need to do anything further.
    // An inner class reference like Abc.Inner is also considered a KtDotQualifiedExpression in
    // some cases.
    is KtDotQualifiedExpression -> {
      module
        .resolveClassByFqName(FqName(text))
        ?.let { return it.fqNameSafe }
        ?: text
    }
    is KtNameReferenceExpression -> getReferencedName()
    is KtUserType -> {
      val isGenericType = children.any { it is KtTypeArgumentList }
      if (isGenericType) {
        // For an expression like Lazy<Abc> the qualifier will be null. If the qualifier exists,
        // then it may refer to the package and the referencedName refers to the class name, e.g.
        // a KtUserType "abc.def.GenericType<String>" has three children: a qualifier "abc.def",
        // the referencedName "GenericType" and the KtTypeArgumentList.
        val qualifierText = qualifier?.text
        val className = referencedName

        if (qualifierText != null) {
          // The generic might be fully qualified. Try to resolve it and return early.
          module
            .resolveClassByFqName(FqName("$qualifierText.$className"))
            ?.let { return it.fqNameSafe }

          // If the name isn't fully qualified, then it's something like "Outer.Inner".
          // We can't use `text` here because that includes the type parameter(s).
          "$qualifierText.$className"
        } else {
          className ?: failTypeHandling()
        }
      } else {
        val text = text

        // Sometimes a KtUserType is a fully qualified name. Give it a try and return early.
        if (text.contains(".") && text[0].isLowerCase()) {
          module
            .resolveClassByFqName(FqName(text))
            ?.let { return it.fqNameSafe }
        }

        // We can't use referencedName here. For inner classes like "Outer.Inner" it would only
        // return "Inner", whereas text returns "Outer.Inner", what we expect.
        text
      }
    }
    is KtTypeReference -> {
      val children = children
      if (children.size == 1) {
        try {
          // Could be a KtNullableType or KtUserType.
          return children[0].requireFqName(module)
        } catch (e: Exception) {
          // Fallback to the text representation.
          text
        }
      } else {
        text
      }
    }
    is KtNullableType -> return innerType?.requireFqName(module) ?: failTypeHandling()
    is KtAnnotationEntry -> return typeReference?.requireFqName(module) ?: failTypeHandling()
    else -> failTypeHandling()
  }

  // E.g. OuterClass.InnerClass
  val classReferenceOuter = classReference.substringBefore(".")

  val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

  // First look in the imports for the reference name. If the class is imported, then we know the
  // fully qualified name.
  importPaths
    .filter { it.alias == null && it.fqName.shortName().asString() == classReference }
    .also { matchingImportPaths ->
      when {
        matchingImportPaths.size == 1 ->
          return matchingImportPaths[0].fqName
        matchingImportPaths.size > 1 ->
          return matchingImportPaths.first { importPath ->
            module.resolveClassByFqName(importPath.fqName) != null
          }.fqName
      }
    }

  importPaths
    .filter { it.alias == null && it.fqName.shortName().asString() == classReferenceOuter }
    .also { matchingImportPaths ->
      when {
        matchingImportPaths.size == 1 ->
          return FqName("${matchingImportPaths[0].fqName.parent()}.$classReference")
        matchingImportPaths.size > 1 ->
          return matchingImportPaths.first { importPath ->
            val fqName = FqName("${importPath.fqName.parent()}.$classReference")
            module.resolveClassByFqName(fqName) != null
          }.fqName
      }
    }

  // If there is no import, then try to resolve the class with the same package as this file.
  module.findClassOrTypeAlias(containingKtFile.packageFqName, classReference)
    ?.let { return it.fqNameSafe }

  // If this doesn't work, then maybe a class from the Kotlin package is used.
  module.resolveClassByFqName(FqName("kotlin.$classReference"))
    ?.let { return it.fqNameSafe }

  // If this doesn't work, then maybe a class from the Kotlin collection package is used.
  module.resolveClassByFqName(FqName("kotlin.collections.$classReference"))
    ?.let { return it.fqNameSafe }

  // If this doesn't work, then maybe a class from the Kotlin jvm package is used.
  module.resolveClassByFqName(FqName("kotlin.jvm.$classReference"))
    ?.let { return it.fqNameSafe }

  // Or java.lang.
  module.resolveClassByFqName(FqName("java.lang.$classReference"))
    ?.let { return it.fqNameSafe }

  findFqNameInSuperTypes(module, classReference)
    ?.let { return it }

  containingKtFile.importDirectives
    .asSequence()
    .filter { it.isAllUnder }
    .mapNotNull {
      // This fqName is the everything in front of the star, e.g. for "import java.io.*" it
      // returns "java.io".
      it.importPath?.fqName
    }
    .forEach { importFqName ->
      module.findClassOrTypeAlias(importFqName, classReference)?.let { return it.fqNameSafe }
    }

  // Check if it's a named import.
  containingKtFile.importDirectives
    .firstOrNull { classReference == it.importPath?.importedName?.asString() }
    ?.importedFqName
    ?.let { return it }

  // Everything else isn't supported.
  throw  Exception("Couldn't resolve FqName $classReference for Psi element: $text")
}

private fun PsiElement.findFqNameInSuperTypes(
  module: Project2,
  classReference: String
): FqName? {
  fun tryToResolveClassFqName(outerClass: FqName): FqName? =
    module
      .resolveClassByFqName(FqName("$outerClass.$classReference"))
      ?.fqNameSafe

  return parents.filterIsInstance<KtClassOrObject>()
    .flatMap { clazz ->
      tryToResolveClassFqName(clazz.requireFqName())?.let { return@flatMap sequenceOf(it) }

      // At this point we can't work with Psi APIs anymore. We need to resolve the super types
      // and try to find inner class in them.
      val descriptor = clazz.requireClassDescriptor(module)
      listOf(descriptor.defaultType).getAllSuperTypes()
        .mapNotNull { tryToResolveClassFqName(it) }
    }
    .firstOrNull()
}

internal fun List<KotlinType>.getAllSuperTypes(): Sequence<FqName> =
  generateSequence(this) { kotlinTypes ->
    kotlinTypes.ifEmpty { null }?.flatMap { it.supertypes() }
  }
    .flatMap { it.asSequence() }
    .map { it.classDescriptorForType().fqNameSafe }

internal fun KotlinType.classDescriptorForType() = DescriptorUtils.getClassDescriptorForType(this)

fun KtClassOrObject.requireClassDescriptor(module: Project2): ClassDescriptor {
  return module.resolveClassByFqName(requireFqName())
    ?: throw  Exception(
      "Couldn't resolve class for ${requireFqName()}."
    )
}

fun FqName.requireClassDescriptor(module: Project2): ClassDescriptor {
  return module.resolveClassByFqName(this)
    ?: throw Exception("Couldn't resolve class for $this.")
}

internal fun Project2.findClassOrTypeAlias(
  packageName: FqName,
  className: String
): ClassifierDescriptorWithTypeParameters? {
  resolveClassByFqName(FqName("${packageName.safePackageString()}$className"))
    ?.let { return it }

  findTypeAliasAcrossModuleDependencies(ClassId(packageName, Name.identifier(className)))
    ?.let { return it }

  return null
}

internal fun Project2.resolveClassByFqName(
  fqName: FqName
): ClassDescriptor? {
  return null
}

internal fun Project2.findTypeAliasAcrossModuleDependencies(
  classId: ClassId
): ClassDescriptor? {
  return null
}

/**
 * This function should only be used for package names. If the FqName is the root (no package at
 * all), then this function returns an empty string whereas `toString()` would return "<root>". For
 * a more convenient string concatenation the returned result can be prefixed and suffixed with an
 * additional dot. The root package never will use a prefix or suffix.
 */
internal fun FqName.safePackageString(
  dotPrefix: Boolean = false,
  dotSuffix: Boolean = true
): String =
  if (isRoot) {
    ""
  } else {
    val prefix = if (dotPrefix) "." else ""
    val suffix = if (dotSuffix) "." else ""
    "$prefix$this$suffix"
  }
