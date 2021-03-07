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

package modulecheck.api

import modulecheck.api.context.*
import net.swiftzer.semver.SemVer
import java.io.File
import java.util.concurrent.*
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
interface Project2 :
  ProjectContext,
  Comparable<Project2>,
  ProjectsAware {

  val path: String

  val rootProject: Project2
  val allprojects: List<Project2>
  val projectDir: File
  val buildFile: File

  val configurations: Map<String, Config>

  val projectDependencies: Map<ConfigurationName, List<ConfiguredProjectDependency>>

  val hasKapt: Boolean

  val sourceSets: Map<SourceSetName, SourceSet>

  fun allPublicClassPathDependencyDeclarations(): Set<ConfiguredProjectDependency>
  fun sourceOf(dependencyProject: ConfiguredProjectDependency, apiOnly: Boolean = false): Project2?
}

fun Project2.isAndroid(): Boolean {
  contract {
    returns(true) implies (this@isAndroid is AndroidProject2)
  }
  return this is AndroidProject2
}

interface AndroidProject2 : Project2 {
  val agpVersion: SemVer
  val androidResourcesEnabled: Boolean
  val viewBindingEnabled: Boolean
  val resourceFiles: Set<File>
  val androidPackageOrNull: String?
  fun androidResourceDeclarationsForSourceSetName(
    sourceSetName: SourceSetName
  ): Set<DeclarationName>
}
