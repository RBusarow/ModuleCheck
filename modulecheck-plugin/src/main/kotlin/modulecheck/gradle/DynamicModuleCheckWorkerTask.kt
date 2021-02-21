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

import modulecheck.api.Finding
import modulecheck.api.Project2
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.concurrent.*
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class DynamicModuleCheckWorkerTask @Inject constructor(
  val rule: ModuleCheckRule<Finding>,
  val workerExecutor: WorkerExecutor
) : ModuleCheckTask() {

  init {
    description = rule.description
  }

  override fun List<Project2>.getFindings(): List<Finding> {

    val queue = workerExecutor.noIsolation()

    forEach { p ->
      projects[p.path] = p
    }

    rules[rule.id] = rule

    println(1)

    forEach { p ->

      println(p)

      queue.submit(RuleExecutor::class) {

        println(3)

        ruleId.set(this@DynamicModuleCheckWorkerTask.rule.id)
        projectPath.set(p.path)
      }
    }

    println(9)

    queue.await()

    println(10)

    return findings
  }
}

@Suppress("UnstableApiUsage")
interface RuleHolder : WorkParameters {
  val ruleId: Property<String>
  val projectPath: Property<String>
}

@Suppress("UnstableApiUsage")
abstract class RuleExecutor : WorkAction<RuleHolder> {

  override fun execute() {

    try {
      val project = projects[parameters.projectPath.get()]!!
      val rule = rules[parameters.ruleId.get()]!!

      println(4)

      val found = rule.check(project)

      println(5)

      synchronized(findings) {
        println(6)
        findings.addAll(found)
        println(7)
      }
      println(8)
    } catch (e: Exception) {
      println("caught --> $e")
    }
  }
}

internal val rules = ConcurrentHashMap<String, ModuleCheckRule<Finding>>()
internal val projects = ConcurrentHashMap<String, Project2>()
internal val findings = mutableListOf<Finding>()
