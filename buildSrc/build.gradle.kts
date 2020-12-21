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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  google()
  jcenter()
  maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

dependencies {

  compileOnly(gradleApi())

  val kotlinVersion = "1.4.21"

  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation(kotlin("stdlib", version = kotlinVersion))
  implementation(kotlin("stdlib-common", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk7", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
  implementation(kotlin("reflect", version = kotlinVersion))

  implementation("com.android.tools.build:gradle:4.1.0") // update Dependencies.kt as well
  implementation("com.jaredsburrows:gradle-spoon-plugin:1.5.0") // update Dependencies.kt as well
  implementation("com.squareup:kotlinpoet:1.6.0") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") // update Dependencies.kt as well
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9") // update Dependencies.kt as well
}

tasks.withType<KotlinCompile>()
  .configureEach {

    kotlinOptions {

      freeCompilerArgs = listOf(
        "-Xinline-classes",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
      )
    }
  }
