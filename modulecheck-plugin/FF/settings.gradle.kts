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

pluginManagement {
  repositories {
    gradlePluginPortal()
    jcenter()
    google()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("com.android")) {
        useModule("com.android.tools.build:gradle:7.0.0-alpha12")
      }
      if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
        useVersion("1.5.0-M2")
      }
    }
  }
}

include(
  ":lib1",
  ":lib2"
)
