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

package com.rickbusarow.modulecheck.overshot

import com.rickbusarow.modulecheck.mcp
import com.rickbusarow.modulecheck.rule.AbstractRule
import org.gradle.api.Project

class OvershotRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : AbstractRule<OverShotDependencyFinding>(
  project, alwaysIgnore, ignoreAll
) {

  override fun check(): List<OverShotDependencyFinding> {
    if (project.path in ignoreAll) return emptyList()

    return project.mcp().overshot
      .all()
      .distinctBy { it.dependencyIdentifier }
  }
}
