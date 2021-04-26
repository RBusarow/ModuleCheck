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

package modulecheck.api.anvil

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.context.ProjectContext
import modulecheck.api.context.declarations
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.files.KotlinFile
import modulecheck.api.psi.hasAnnotation
import modulecheck.psi.DeclarationName
import modulecheck.psi.asDeclaractionName
import modulecheck.psi.internal.getByNameOrIndex
import modulecheck.psi.internal.toFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class AnvilGraph(
  val project: Project2,
  val scopeContributions: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>,
  val scopeMerges: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : ProjectContext.Element {
  override val key: ProjectContext.Key<AnvilGraph>
    get() = Key

  override fun toString(): String {
    return """ProjectAnvilGraph(
      | project=$project,
      | scopeContributions=$scopeContributions,
      | scopeMerges=$scopeMerges)""".trimMargin()
  }

  companion object Key : ProjectContext.Key<AnvilGraph> {

    override operator fun invoke(project: Project2): AnvilGraph {
      if (project.anvilGradlePlugin == null) return AnvilGraph(
        project = project,
        scopeContributions = emptyMap(),
        scopeMerges = emptyMap()
      )

      val mergeAnnotations = mergeAnnotations()
      val allAnnotations = mergeAnnotations + contributeAnnotations()

      val mergedMap = mutableMapOf<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>()
      val contributedMap = mutableMapOf<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>()

      val done = mutableSetOf<CompoundKey>()

      project.sourceSets
        .keys
        .forEach { sourceSetName ->

          val key = CompoundKey.of(project, sourceSetName)

          if (!done.contains(key)) {
            val (merged, contributed) = project.declarationsForScopeName(
              sourceSetName = sourceSetName,
              allAnnotations = allAnnotations,
              mergeAnnotations = mergeAnnotations
            )

            mergedMap[sourceSetName] = merged
            contributedMap[sourceSetName] = contributed

            done.add(key)
          }
        }

      return AnvilGraph(
        project = project,
        scopeContributions = contributedMap,
        scopeMerges = mergedMap
      )
    }

    private fun contributeAnnotations(): Set<FqName> = setOf(
      anvilContributesToFqName,
      anvilContributesBindingFqName,
      anvilContributesMultibindingFqName
    )

    private fun mergeAnnotations(): Set<FqName> = setOf(
      anvilMergeComponentFqName,
      anvilMergeSubcomponentFqName,
      anvilMergeInterfacesFqName
    )

    private fun Project2.declarationsForScopeName(
      sourceSetName: SourceSetName,
      allAnnotations: Set<FqName>,
      mergeAnnotations: Set<FqName>
    ): Pair<Map<AnvilScopeName, Set<DeclarationName>>, Map<AnvilScopeName, Set<DeclarationName>>> {
      val mergedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()
      val contributedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()

      jvmFilesForSourceSetName(sourceSetName)
        .asSequence()
        // Anvil only works with Kotlin, so no point in trying to parse Java files
        .filterIsInstance<KotlinFile>()
        .onEach { kotlinFile ->

          kotlinFile.ktFile.getChildrenOfType<KtAnnotated>()
            .forEach { annotated ->

              contributeAnnotations()
                .forEach { ca ->

                  val hasIt = annotated.hasAnnotation(ca)

                  if (hasIt) {
                    println(
                      """ -------------------------------------------------------
                          |annotated --> ${annotated.text}
                          |ca        --> ${ca.asString()}
                          |has it    --> $hasIt
                           """.trimMargin()
                    )
                  }
                }
            }
          println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    file --> ${kotlinFile.name}   source set --> ${sourceSetName.value}")
        }
        // only re-visit files which have Anvil annotations
        .filter { kotlinFile ->
          kotlinFile.imports.any { it in allAnnotations } ||
            kotlinFile.maybeExtraReferences.any { it in allAnnotations }
        }
        .forEach { kotlinFile ->

          val (merged, contributed) = kotlinFile
            .getScopeArguments(allAnnotations, mergeAnnotations)

          merged
            .forEach { rawAnvilAnnotatedType ->

              val scopeName = getAnvilScopeName(
                scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                sourceSetName = sourceSetName,
                kotlinFile = kotlinFile
              )

              val declarationNames = mergedMap.getOrPut(scopeName) { mutableSetOf() }

              declarationNames.add(rawAnvilAnnotatedType.declarationName)

              mergedMap[scopeName] = declarationNames
            }
          contributed
            .forEach { rawAnvilAnnotatedType ->

              val scopeName = getAnvilScopeName(
                scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                sourceSetName = sourceSetName,
                kotlinFile = kotlinFile
              )

              val declarationNames = contributedMap.getOrPut(scopeName) { mutableSetOf() }

              declarationNames.add(rawAnvilAnnotatedType.declarationName)

              contributedMap[scopeName] = declarationNames
            }
        }

      return mergedMap to contributedMap
    }

    private fun Project2.contributesElements(
      sourceSetName: SourceSetName,
      allAnnotations: Set<FqName>,
      mergeAnnotations: Set<FqName>
    ): Pair<Map<AnvilScopeName, Set<DeclarationName>>, Map<AnvilScopeName, Set<DeclarationName>>> {
      val mergedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()
      val contributedMap = mutableMapOf<AnvilScopeName, MutableSet<DeclarationName>>()

      // Anvil only works with Kotlin, so no point in trying to parse Java files
      val files = jvmFilesForSourceSetName(sourceSetName)
        .asSequence()

        .filterIsInstance<KotlinFile>()

      files.forEach { kotlinFile ->

        kotlinFile
          .ktFile
          .getChildrenOfType<KtAnnotated>()
          .forEach { annotated ->

            when {
              // contributed module
              annotated.hasAnnotation(anvilContributesToFqName) &&
                annotated.hasAnnotation(daggerModuleFqName) -> {
              }
              // contributed component
              annotated.hasAnnotation(anvilContributesToFqName) -> { }
              // contributed binding
              annotated.hasAnnotation(anvilContributesBindingFqName) -> { }
              // contributed multibinding
              annotated.hasAnnotation(anvilContributesMultibindingFqName) -> { }
            }

          }
      }
        // only re-visit files which have Anvil annotations
        .filter { kotlinFile ->
          kotlinFile.imports.any { it in allAnnotations } ||
            kotlinFile.maybeExtraReferences.any { it in allAnnotations }
        }
        .forEach { kotlinFile ->

          val (merged, contributed) = kotlinFile
            .getScopeArguments(allAnnotations, mergeAnnotations)

          merged
            .forEach { rawAnvilAnnotatedType ->

              val scopeName = getAnvilScopeName(
                scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                sourceSetName = sourceSetName,
                kotlinFile = kotlinFile
              )

              val declarationNames = mergedMap.getOrPut(scopeName) { mutableSetOf() }

              declarationNames.add(rawAnvilAnnotatedType.declarationName)

              mergedMap[scopeName] = declarationNames
            }
          contributed
            .forEach { rawAnvilAnnotatedType ->

              val scopeName = getAnvilScopeName(
                scopeNameEntry = rawAnvilAnnotatedType.anvilScopeNameEntry,
                sourceSetName = sourceSetName,
                kotlinFile = kotlinFile
              )

              val declarationNames = contributedMap.getOrPut(scopeName) { mutableSetOf() }

              declarationNames.add(rawAnvilAnnotatedType.declarationName)

              contributedMap[scopeName] = declarationNames
            }
        }

      return mergedMap to contributedMap
    }

    private fun KotlinFile.getScopeArguments(
      allAnnotations: Set<FqName>,
      mergeAnnotations: Set<FqName>
    ): ScopeArgumentParseResult {
      val mergeArguments = mutableSetOf<RawAnvilAnnotatedType>()
      val contributeArguments = mutableSetOf<RawAnvilAnnotatedType>()

      val visitor = classOrObjectRecursiveVisitor { classOrObject ->

        val typeFqName = classOrObject.fqName ?: return@classOrObjectRecursiveVisitor
        val annotated = classOrObject.safeAs<KtAnnotated>() ?: return@classOrObjectRecursiveVisitor

        annotated
          .annotationEntries
          .filter { annotationEntry ->
            val typeRef = annotationEntry.typeReference?.text ?: return@filter false

            allAnnotations.any { it.asString().endsWith(typeRef) }
          }
          .forEach { annotationEntry ->
            val typeRef = annotationEntry.typeReference!!.text

            val raw = annotationEntry.toRawAnvilAnnotatedType(typeFqName) ?: return@forEach

            if (mergeAnnotations.any { it.asString().endsWith(typeRef) }) {
              mergeArguments.add(raw)
            } else {
              contributeArguments.add(raw)
            }
          }
      }

      ktFile.accept(visitor)

      return ScopeArgumentParseResult(
        mergeArguments = mergeArguments,
        contributeArguments = contributeArguments
      )
    }

    internal data class ScopeArgumentParseResult(
      val mergeArguments: Set<RawAnvilAnnotatedType>,
      val contributeArguments: Set<RawAnvilAnnotatedType>
    )

    fun KtAnnotationEntry.toRawAnvilAnnotatedType(typeFqName: FqName): RawAnvilAnnotatedType? {
      val valueArgument = valueArgumentList
        ?.getByNameOrIndex(0, "scope")
        ?: return null

      val entryText = valueArgument
        .text
        .replace(".+[=]+".toRegex(), "") // remove named arguments
        .replace("::class", "")
        .trim()
        .toFqName()

      return RawAnvilAnnotatedType(
        declarationName = typeFqName.asDeclaractionName(),
        anvilScopeNameEntry = AnvilScopeNameEntry(entryText)
      )
    }

    private fun Project2.getAnvilScopeName(
      scopeNameEntry: AnvilScopeNameEntry,
      sourceSetName: SourceSetName,
      kotlinFile: KotlinFile
    ): AnvilScopeName {
      val dependenciesBySourceSetName = dependenciesBySourceSetName()

      // if scope is directly imported (most likely),
      // then use that fully qualified import
      val rawScopeName = kotlinFile.imports.firstOrNull { import ->
        import.shortName() == scopeNameEntry.name.shortName()
      }
        ?.asDeclaractionName()
      // if the scope is wildcard-imported
        ?: dependenciesBySourceSetName[sourceSetName]
          .orEmpty()
          .asSequence()
          .flatMap { cpd ->
            cpd.project
              .declarations[SourceSetName.MAIN]
              .orEmpty()
          }
          .filter { dn ->
            dn.fqName in kotlinFile.maybeExtraReferences
          }
          .firstOrNull { dn ->
            dn.fqName.shortName() == scopeNameEntry.name.shortName()
          } // Scope must be defined in this same module
        ?: kotlinFile
          .maybeExtraReferences
          .firstOrNull { maybeExtra ->
            maybeExtra
              .asString()
              .startsWith(kotlinFile.packageFqName.asString()) &&
              maybeExtra
                .asString()
                .endsWith(scopeNameEntry.name.asString())
          }
          ?.asDeclaractionName()
        // Scope must be defined in this same package
        ?: "${kotlinFile.packageFqName}.${scopeNameEntry.name}".asDeclaractionName()

      return AnvilScopeName(rawScopeName)
    }

    private fun Project2.dependenciesBySourceSetName(): Map<SourceSetName, List<ConfiguredProjectDependency>> {
      return configurations
        .map { (configurationName, _) ->
          configurationName.toSourceSetName() to projectDependencies.value[configurationName].orEmpty()
        }
        .groupBy { it.first }
        .map { it.key to it.value.flatMap { it.second } }
        .toMap()
    }
  }
}

val ProjectContext.anvilGraph: AnvilGraph
  get() = get(AnvilGraph)

data class CompoundKey(val value: Int) {

  companion object {

    fun of(vararg key: Any?): CompoundKey = CompoundKey(
      key.fold(0) { acc, any ->
        (31 * acc) + (any?.hashCode() ?: 0)
      }
    )
  }
}
