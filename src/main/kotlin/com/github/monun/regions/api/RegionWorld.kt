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

package com.github.monun.regions.api

import org.bukkit.World

interface RegionWorld : Area {
    val bukkitWorld: World?

    val regions: List<Region>

    fun chunkAt(chunkX: Int, chunkZ: Int): RegionChunk?

    fun regionAt(x: Int, y: Int, z: Int): Region?

    fun getOverlapRegions(box: RegionBox, except: Region? = null): Set<Region>

    fun checkOverlap(box: RegionBox, except: Region? = null) {
        getOverlapRegions(box, except).let { overlaps ->
            require(overlaps.isEmpty()) {
                "Overlap with ${overlaps.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { it.name }}"
            }
        }
    }
}