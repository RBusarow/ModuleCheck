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

package com.rickbusarow.modulecheck.kapt

import com.rickbusarow.modulecheck.MCP
import org.gradle.api.artifacts.Dependency

object KaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<KaptProcessor>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<KaptProcessor> {
    val grouped = mcp.project
      .configurations
      .groupBy { it.name }
      .mapValues { (_, configurations) ->
        configurations.flatMap { config ->
          config
            .dependencies
            .map { dep: Dependency ->

              val comb = dep.group + ":" + dep.name

              KaptProcessor(comb)
            }
        }.toSet()
      }

    return MCP.ParsedKapt(
      grouped.getOrDefault("kaptAndroidTest", setOf()),
      grouped.getOrDefault("kapt", setOf()),
      grouped.getOrDefault("kaptTest", setOf())
    )
  }
}
