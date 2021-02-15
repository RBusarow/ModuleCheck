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

import com.rickbusarow.modulecheck.specs.ProjectBuildSpec
import com.rickbusarow.modulecheck.specs.ProjectSettingsSpec
import com.rickbusarow.modulecheck.specs.ProjectSpec
import com.rickbusarow.modulecheck.specs.ProjectSpecBuilder
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class Android : BaseTest() {

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  val projectSpecBuilder = ProjectSpecBuilder("project") {
    addSettingsSpec(
      ProjectSettingsSpec {
        addInclude("app")
      }
    )
    addBuildSpec(
      ProjectBuildSpec {
        addPlugin("id(\"com.rickbusarow.module-check\")")
        buildScript()
      }
    )
  }

  @Test
  fun `configurations should be grouped and sorted`() {
    projectSpecBuilder
      .addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("""id("com.android.library")""")
              android = true
            }
          )
        }
      )

    projectSpecBuilder
      .build()
      .writeIn(testProjectDir.toPath())

    val result = gradleRunner
      .withArguments("moduleCheckSortDependencies")
      .build()

    result.task(":moduleCheckSortDependencies")?.outcome shouldBe TaskOutcome.SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
      |  id("com.android.library")
      |}
      |
      |android {
      |  compileSdkVersion(30)
      |
      |  defaultConfig {
      |    minSdkVersion(23)
      |    targetSdkVersion(30)
      |    versionCode = 1
      |    versionName = "1.0"
      |  }
      |
      |  buildTypes {
      |    getByName("release") {
      |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      |    }
      |  }
      |}
      |
      |""".trimMargin()
  }
}
