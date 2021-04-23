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

import com.squareup.anvil.annotations.*
import com.squareup.anvil.annotations.compat.MergeInterfaces
import dagger.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import modulecheck.api.context.ImportName
import modulecheck.psi.DeclarationName
import net.swiftzer.semver.SemVer
import org.jetbrains.kotlin.name.FqName
import javax.inject.Inject
import javax.inject.Qualifier

data class AnvilGradlePlugin(
  val version: SemVer,
  val generateDaggerFactories: Boolean
)

data class AnvilAnnotatedType(
  val contributedTypeDeclaration: DeclarationName,
  val contributedScope: AnvilScopeName
)

data class RawAnvilAnnotatedType(
  val declarationName: DeclarationName,
  val anvilScopeNameEntry: AnvilScopeNameEntry
)

data class AnvilScopeName(val fqName: DeclarationName)
data class AnvilScopeNameEntry(val name: ImportName)

val anvilMergeComponentFqName = FqName(MergeComponent::class.java.canonicalName)
val anvilMergeSubcomponentFqName = FqName(MergeSubcomponent::class.java.canonicalName)
val anvilMergeInterfacesFqName = FqName(MergeInterfaces::class.java.canonicalName)
val anvilContributesToFqName = FqName(ContributesTo::class.java.canonicalName)
val anvilContributesBindingFqName = FqName(ContributesBinding::class.java.canonicalName)
val anvilContributesMultibindingFqName = FqName(ContributesMultibinding::class.java.canonicalName)

val daggerAssistedFactoryFqName = FqName(AssistedFactory::class.java.canonicalName)
val daggerAssistedFqName = FqName(Assisted::class.java.canonicalName)
val daggerAssistedInjectFqName = FqName(AssistedInject::class.java.canonicalName)
val daggerBindsFqName = FqName(Binds::class.java.canonicalName)
val daggerComponentFqName = FqName(Component::class.java.canonicalName)
val daggerInjectFqName = FqName(Inject::class.java.canonicalName)
val daggerLazyFqName = FqName(Lazy::class.java.canonicalName)
val daggerMapKeyFqName = FqName(MapKey::class.java.canonicalName)
val daggerModuleFqName = FqName(Module::class.java.canonicalName)
val daggerProvidesFqName = FqName(Provides::class.java.canonicalName)
val daggerQualifierFqName = FqName(Qualifier::class.java.canonicalName)
val daggerSubcomponentFqName = FqName(Subcomponent::class.java.canonicalName)

val injectFqName = FqName(Inject::class.java.canonicalName)

sealed class AnvilElement {
  abstract val scopeName: AnvilScopeName
  abstract val referencedDeclaration: DeclarationName

  /**
   * Added to Anvil using `@ContributesTo`.
   *
   * If the interface being annotated is used anywhere in the merged graph,
   * then the module with the merged component must depend upon this contributed interface's module.
   */
  data class AnvilContributedComponent(
    override val scopeName: AnvilScopeName,
    override val referencedDeclaration: DeclarationName
  ) : AnvilElement()

  /**
   * Added to Anvil using `@ContributesTo`.
   *
   * TODODOTODOEODODOTODODODODODOODD
   */
  data class AnvilContributedModule(
    override val scopeName: AnvilScopeName,
    override val referencedDeclaration: DeclarationName,
    val bindings: List<AnvilBinding>
  ) : AnvilElement()

  /**
   * Added to Anvil using `@ContributesBinding`.
   *
   * If the bound type is used anywhere in the merged graph,
   * then the module with the merged component must depend upon this contributed binding's module.
   */
  data class AnvilBinding(
    override val scopeName: AnvilScopeName,
    override val referencedDeclaration: DeclarationName
  ) : AnvilElement()

  /**
   * Added to Anvil using `@ContributesMultibinding`.
   *
   * TODODOTODOEODODOTODODODODODOODD
   */
  data class AnvilMultiBinding(
    override val scopeName: AnvilScopeName,
    override val referencedDeclaration: DeclarationName
  ) : AnvilElement()
}
