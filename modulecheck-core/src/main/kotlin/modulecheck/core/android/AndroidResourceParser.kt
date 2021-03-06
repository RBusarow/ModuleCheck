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

package modulecheck.core.android

import groovy.util.Node
import groovy.xml.XmlParser
import modulecheck.psi.DeclarationName
import modulecheck.psi.asDeclaractionName
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

object AndroidResourceParser {
  private val parser = XmlParser()

  fun parseFile(resDir: File): Set<DeclarationName> {
    val values = mutableSetOf<AndroidResource>()

    val resources = resDir
      .walkTopDown()
      .filter { it.isFile }
      .filter { it.extension == "xml" }

      .onEach { file ->
        val parsed = parser.parse(file)

        if (parsed.name() == "resources") {
          val t = parsed.children().cast<List<Node>>()

          t.forEach { node ->

            AndroidResource.fromValuePair(
              node.name()
                .toString(),
              node.attributes().values.first()?.toString() ?: ""
            )?.also { values.add(it) }
          }
        }
      }

      .mapNotNull { file -> AndroidResource.fromFile(file) }
      .toSet() + values

    return resources
      .map { "R.${it.prefix}.${it.name}".asDeclaractionName() }
      .toSet()
  }
}
