/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.files.JavaFile
import com.rickbusarow.modulecheck.files.KotlinFile
import com.rickbusarow.modulecheck.internal.asKtFile
import com.rickbusarow.modulecheck.internal.files
import com.rickbusarow.modulecheck.task.*
import com.rickbusarow.modulecheck.task.android.DisableAndroidResourcesTask
import com.rickbusarow.modulecheck.task.android.DisableViewBindingTask
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.extensions.create("moduleCheck", ModuleCheckExtension::class.java)
    target.tasks.register("moduleCheck", ModuleCheckTask::class.java)
    target.tasks.register("moduleCheckOverShot", OverShotTask::class.java)
    target.tasks.register("moduleCheckRedundant", RedundantTask::class.java)
    target.tasks.register("moduleCheckUnused", UnusedTask::class.java)
    target.tasks.register("moduleCheckUsed", UsedTask::class.java)
    target.tasks.register("moduleCheckSortDependencies", SortDependenciesTask::class.java)
    target.tasks.register("moduleCheckSortPlugins", SortPluginsTask::class.java)
    target.tasks.register("moduleCheckKapt", UnusedKaptTask::class.java)
    target.tasks.register("moduleCheckDisableAndroidResources", DisableAndroidResourcesTask::class.java)
    target.tasks.register("moduleCheckDisableViewBinding", DisableViewBindingTask::class.java)
  }
}

internal fun List<String>.positionOf(
  project: Project,
  configuration: Config
): Position? {
  val reg = """.*${configuration.name}\(project[(]?(?:path =\s*)"${project.path}".*""".toRegex()

  val row = indexOfFirst { it.trim().matches(reg) }

  if (row < 0) return null

  val col = get(row).indexOfFirst { it != ' ' }

  return Position(row + 1, col + 1)
}

internal fun List<String>.positionOf(
  path: String,
  configuration: Config
): Position? {
  val reg = """.*${configuration.name}\(project[(]?(?:path =\s*)"$path".*""".toRegex()

  val row = indexOfFirst { it.trim().matches(reg) }

  if (row < 0) return null

  val col = get(row).indexOfFirst { it != ' ' }

  return Position(row + 1, col + 1)
}

fun File.jvmFiles() = walkTopDown()
  .files()
  .mapNotNull { file ->
    when {
      file.name.endsWith(".kt") -> KotlinFile(file.asKtFile())
      file.name.endsWith(".java") -> JavaFile(file)
      else -> null
    }
  }.toList()
