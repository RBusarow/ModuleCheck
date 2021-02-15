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

package modulecheck.gradle.task

import modulecheck.*
import modulecheck.api.*
import modulecheck.core.kapt.UnusedKaptRule
import modulecheck.core.kapt.kaptMatchers
import modulecheck.core.mcp
import modulecheck.core.overshot.OvershotRule
import modulecheck.core.rule.AnvilFactoryRule
import modulecheck.core.rule.MustBeApiRule
import modulecheck.core.rule.RedundantRule
import modulecheck.core.rule.UnusedRule
import modulecheck.core.rule.android.DisableAndroidResourcesRule
import modulecheck.core.rule.android.DisableViewBindingRule
import modulecheck.core.rule.sort.SortDependenciesRule
import modulecheck.core.rule.sort.SortPluginsRule
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.gradle.project2
import modulecheck.psi.DslBlockVisitor
import org.gradle.kotlin.dsl.getByType

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @Suppress("LongMethod", "ComplexMethod")
  override fun getFindings(): List<Finding> {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val checks = extension.checks.get()

    val findings = mutableListOf<Finding>()

    // use a mutable list and with(findings) { ... }
    // because buildList { ... } requires Kotlin 1.4.0, which means Gradle 6.8+
    with(findings) {
      measured {
        project
          .project2()
          .allprojects
          .filter { it.buildFile.exists() }
          .sortedByDescending { it.mcp().getMainDepth() }
          .forEach { proj ->

            if (checks.overshot) {
              addAll(
                OvershotRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.redundant)  {
              addAll(
                RedundantRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.unused) {
              addAll(
                UnusedRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.mustBeApi) {
              addAll(
                MustBeApiRule(proj, alwaysIgnore, ignoreAll).check()
                  .distinctBy { it.dependencyProject.path }
              )
            }

            if (checks.sortDependencies) {
              val visitor = DslBlockVisitor("dependencies")

              addAll(
                SortDependenciesRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  visitor = visitor,
                  comparator = dependencyComparator
                )
                  .check()
              )
            }

            if (checks.sortPlugins) {
              val visitor = DslBlockVisitor("plugins")

              addAll(
                SortPluginsRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  visitor = visitor,
                  comparator = pluginComparator
                )
                  .check()
              )
            }

            if (checks.kapt) {
              val additionalKaptMatchers = project.extensions
                .getByType<ModuleCheckExtension>()
                .additionalKaptMatchers

              addAll(
                UnusedKaptRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll,
                  kaptMatchers = kaptMatchers + additionalKaptMatchers.get()
                ).check()
              )
            }

            if (checks.anvilFactories) {
              addAll(
                AnvilFactoryRule(
                  project = proj,
                  alwaysIgnore = alwaysIgnore,
                  ignoreAll = ignoreAll
                ).check()
              )
            }

            if (checks.disableAndroidResources && proj is AndroidProject2) {
              addAll(DisableAndroidResourcesRule(proj, alwaysIgnore, ignoreAll).check())
            }

            if (checks.disableViewBinding && proj is AndroidProject2) {
              addAll(DisableViewBindingRule(proj, alwaysIgnore, ignoreAll).check())
            }
          }
      }
    }

    return findings
  }
}
