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

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
  javaLibrary
}

kotlin {
  explicitApi = ExplicitApiMode.Strict
}

dependencies {

  compileOnly(gradleApi())

  implementation(Libs.javaParser)
  implementation(Libs.Kotlin.compiler)
  implementation(Libs.Kotlin.gradlePlugin)
  implementation(Libs.Kotlin.reflect)
  implementation(Libs.Square.KotlinPoet.core)

  testImplementation(Libs.JUnit.api)
  testImplementation(Libs.JUnit.engine)
  testImplementation(Libs.JUnit.params)
  testImplementation(Libs.Kotest.assertions)
  testImplementation(Libs.Kotest.properties)
  testImplementation(Libs.Kotest.runner)
  testImplementation(projects.modulecheckInternalTesting)
}
