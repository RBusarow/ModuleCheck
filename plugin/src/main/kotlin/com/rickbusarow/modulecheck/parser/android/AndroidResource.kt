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

package com.rickbusarow.modulecheck.parser.android

sealed class AndroidResource(val prefix: kotlin.String) {

  abstract val name: kotlin.String

  data class Anim(override val name: kotlin.String) : AndroidResource("anim")
  data class Animator(override val name: kotlin.String) : AndroidResource("animator")
  data class Arrays(override val name: kotlin.String) : AndroidResource("arrays")
  data class Color(override val name: kotlin.String) : AndroidResource("color")
  data class Dimen(override val name: kotlin.String) : AndroidResource("dimen")
  data class Drawable(override val name: kotlin.String) : AndroidResource("drawable")
  data class Font(override val name: kotlin.String) : AndroidResource("font")
  data class Layout(override val name: kotlin.String) : AndroidResource("layout")
  data class Menu(override val name: kotlin.String) : AndroidResource("menu")
  data class Mipmap(override val name: kotlin.String) : AndroidResource("mipmap")
  data class Raw(override val name: kotlin.String) : AndroidResource("raw")
  data class String(override val name: kotlin.String) : AndroidResource("string")
  data class Style(override val name: kotlin.String) : AndroidResource("style")

  companion object {

    private val REGEX = """"@(.*)/(.*)"""".toRegex()

    @Suppress("ComplexMethod")
    fun fromString(str: kotlin.String): AndroidResource {
      val (prefix, name) = REGEX.find(str)!!.destructured

      return when (prefix) {
        "anim" -> Anim(name)
        "animator" -> Animator(name)
        "arrays" -> Arrays(name)
        "color" -> Color(name)
        "dimen" -> Dimen(name)
        "drawable" -> Drawable(name)
        "font" -> Font(name)
        "layout" -> Layout(name)
        "menu" -> Menu(name)
        "mipmap" -> Mipmap(name)
        "raw" -> Raw(name)
        "string" -> String(name)
        "style" -> Style(name)
        else -> throw IllegalArgumentException("unrecognized resource reference --> $str")
      }
    }
  }
}
