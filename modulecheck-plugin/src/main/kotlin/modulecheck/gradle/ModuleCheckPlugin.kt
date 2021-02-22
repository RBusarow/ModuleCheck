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

package modulecheck.gradle

import com.android.build.gradle.internal.tasks.factory.dependsOn
import modulecheck.api.Finding
import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckExtension
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.gradle.task.ModuleCheckAllTask
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import java.util.*
import javax.inject.Inject

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions.create("moduleCheck", ModuleCheckExtension::class.java)

    if (!target.buildFile.exists()) return

    val factory = ModuleCheckRuleFactory()

    val findings = mutableListOf<Finding>()

    val rules = factory.create(settings)

    val t2 = rules
      .map { rule ->
        target.tasks.register(
          "moduleCheck${rule.id}",
          DynamicModuleCheckTask::class,
          rule,
          findings
        )
      }

    val mcAll =
      target.tasks.register("moduleCheck", ModuleCheckAllTask::class.java, rules, findings)

    mcAll {
      mustRunAfter(t2)
    }

    val shared = UUID.randomUUID().toString()

    val t = (0..15).map { itr ->
      target.tasks.register("foo$itr", MyClass::class, shared)
    }

    val all = target.tasks.register("allMC")

    all.dependsOn(t)
  }
}

abstract class DynamicModuleCheckTask<T : Finding> @Inject constructor(
  val rule: ModuleCheckRule<T>,
  val findings: MutableList<Finding>
) : ModuleCheckTask() {

  init {
    description = rule.description
  }

  override fun List<Project2>.getFindings(): List<T> {
    val f = flatMap { project ->
      rule.check(project)
    }

    synchronized(findings) {
      findings.addAll(f)
    }

    return f
  }
}

abstract class MyClass @Inject constructor(
  val shared: String
) : DefaultTask() {

  @TaskAction
  fun execute() {
    println("shared --> $shared")
  }
}
