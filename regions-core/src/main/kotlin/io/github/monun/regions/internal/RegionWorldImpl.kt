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

package io.github.monun.regions.internal

import io.github.monun.regions.api.Area
import io.github.monun.regions.api.Region
import io.github.monun.regions.api.RegionBox
import io.github.monun.regions.api.RegionWorld
import io.github.monun.regions.util.ChunkCoordIntPair
import com.google.common.collect.ImmutableList
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

class RegionWorldImpl(
    manager: RegionManagerImpl,
    name: String
) : AreaImpl(manager, name), RegionWorld {
    override val file: File
        get() = File(manager.worldsFolder, "$name.yml")

    override val type: Area.Type
        get() = Area.Type.WORLD

    override var bukkitWorld: World? = null
        internal set

    override val regions: List<Region>
        get() = ImmutableList.copyOf(_regions)

    private val _regions = TreeSet<RegionImpl> { o1, o2 -> o1.name.compareTo(o2.name) }
    private val _chunks = Long2ObjectOpenHashMap<RegionChunkImpl>()

    internal fun placeRegion(region: RegionImpl, box: RegionBox = region.box) {
        _regions.add(region)

        box.forEach { chunkX, chunkZ ->
            getOrGenerateChunk(chunkX, chunkZ).addRegion(region)
        }
    }

    private fun getOrGenerateChunk(chunkX: Int, chunkZ: Int): RegionChunkImpl {
        val chunks = _chunks
        val key = ChunkCoordIntPair.pair(chunkX, chunkZ)

        var chunk = chunks.get(key)

        if (chunk == null) {
            chunk = RegionChunkImpl(this, chunkX, chunkZ).also {
                chunks.put(key, it)
            }
        }

        return chunk
    }

    internal fun removeRegion(region: RegionImpl) {
        _regions.remove(region)

        region.box.forEach { chunkX, chunkZ ->
            chunkAt(chunkX, chunkZ)?.removeRegion(region)
        }
    }

    override fun chunkAt(chunkX: Int, chunkZ: Int): RegionChunkImpl? {
        return _chunks[ChunkCoordIntPair.pair(chunkX, chunkZ)]
    }

    override fun regionAt(x: Int, y: Int, z: Int): RegionImpl? {
        return chunkAt(x.toChunk(), z.toChunk())?.regionAt(x, y, z)
    }

    override fun getOverlapRegions(box: RegionBox, except: Region?): Set<Region> {
        val overlaps = LinkedHashSet<RegionImpl>(0)

        box.forEach { chunkX, chunkZ ->
            chunkAt(chunkX, chunkZ)?.let { chunk ->
                chunk._regions.forEach { region ->
                    if (region !in overlaps && box.overlaps(region.box)) {
                        overlaps += region
                    }
                }
            }
        }

        return overlaps
    }

    companion object {
        internal fun load(file: File, manager: RegionManagerImpl): RegionWorldImpl {
            val name = file.name.removeSuffix(".yml")
            val config = YamlConfiguration.loadConfiguration(file)

            return RegionWorldImpl(manager, name).apply {
                load(config)
            }
        }
    }
}

fun RegionWorld.toImpl(): RegionWorldImpl {
    return this as RegionWorldImpl
}

fun Int.toChunk(): Int {
    return this shr 4
}

inline fun RegionBox.forEach(action: (chunkX: Int, chunkZ: Int) -> Unit) {
    val chunkMinX = minX.toChunk()
    val chunkMinZ = minZ.toChunk()
    val chunkMaxX = maxX.toChunk()
    val chunkMaxZ = maxZ.toChunk()

    for (chunkX in chunkMinX..chunkMaxX) {
        for (chunkZ in chunkMinZ..chunkMaxZ) {
            action(chunkX, chunkZ)
        }
    }
}