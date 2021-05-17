/*
 * Copyright (c) 2020 monun
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.monun.regions

import com.github.monun.regions.api.RegionBox
import org.bukkit.configuration.ConfigurationSection

fun ConfigurationSection.getBox(path: String): RegionBox {
    val config = getSection(path)
    val min = config.getSection("min")
    val max = config.getSection("max")

    return RegionBox(
        min.getInt("x"),
        min.getInt("y"),
        min.getInt("z"),
        max.getInt("x"),
        max.getInt("y"),
        max.getInt("z")
    )
}

fun ConfigurationSection.setBox(path: String, box: RegionBox) {
    val section = createSection(path)

    section.createSection("min").let { min ->
        min["x"] = box.minX
        min["y"] = box.minY
        min["z"] = box.minZ
    }
    section.createSection("max").let { max ->
        max["x"] = box.maxX
        max["y"] = box.maxY
        max["z"] = box.maxZ
    }
}

fun ConfigurationSection.getSection(path: String): ConfigurationSection {
    return requireNotNull(getConfigurationSection(path)) { "Not found path $path" }
}

fun ConfigurationSection.getStringValue(path: String): String {
    return requireNotNull(getString(path)) { "Undefined $path" }
}

