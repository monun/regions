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

package io.github.monun.regions.api

import org.bukkit.Location
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

class RegionBox(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int) {
    val minX: Int = min(minX, maxX)
    val minY: Int = min(minY, maxY)
    val minZ: Int = min(minZ, maxZ)
    val maxX: Int = max(minX, maxX)
    val maxY: Int = max(minY, maxY)
    val maxZ: Int = max(minZ, maxZ)

    fun expand(x: Int, y: Int, z: Int): RegionBox {
        return RegionBox(
            minX - x,
            minY - y,
            minZ - z,
            maxX + x,
            maxY + y,
            maxZ + z
        )
    }

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun contains(loc: Location): Boolean {
        return contains(loc.blockX, loc.blockY, loc.blockZ)
    }

    fun overlaps(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ): Boolean {
        return this.minX <= maxX && this.maxX >= minX && this.minY <= maxY && this.maxY >= minY && this.minZ <= maxZ && this.maxZ >= minZ
    }

    fun overlaps(box: RegionBox): Boolean {
        return box.run {
            this@RegionBox.overlaps(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    fun toBoundingBox(): BoundingBox {
        return BoundingBox.of(Vector(minX, minY, minZ), Vector(maxX + 1, maxY + 1, maxZ + 1))
    }

    override fun toString(): String {
        return "[$minX, $minY, $minZ - $maxX, $maxY, $maxZ]"
    }

}