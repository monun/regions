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

import com.destroystokyo.paper.profile.PlayerProfile
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.lang.IllegalArgumentException
import java.util.*
import java.util.logging.Logger

object Regions : RegionManager {
    internal var internalInstance: RegionManager? = null
        set(value) {
            if (value != null && field != null) throw IllegalArgumentException("Cannot redefine internal instance")

            field = value
        }

    val instance: RegionManager
        get() = requireNotNull(internalInstance)

    override val logger: Logger
        get() = instance.logger
    override val cachedUsers: List<User>
        get() = instance.cachedUsers
    override val onlineUsers: Collection<User>
        get() = instance.onlineUsers
    override val worlds: List<RegionWorld>
        get() = instance.worlds
    override val worldMap: Map<String, RegionWorld>
        get() = instance.worldMap
    override val regions: List<Region>
        get() = instance.regions
    override val regionMap: Map<String, Region>
        get() = instance.regionMap

    override fun findUser(uniqueId: UUID): User? {
        return instance.findUser(uniqueId)
    }

    override fun getUser(profile: PlayerProfile): User {
        return instance.getUser(profile)
    }

    override fun getUser(player: Player): User? {
        return instance.getUser(player)
    }

    override fun getRegionWorld(bukkitWorld: World): RegionWorld? {
        return instance.getRegionWorld(bukkitWorld)
    }

    override fun getRegionWorld(name: String): RegionWorld? {
        return instance.getRegionWorld(name)
    }

    override fun getRegion(name: String): Region? {
        return instance.getRegion(name)
    }

    override fun registerNewRegion(name: String, world: RegionWorld, box: RegionBox): Region {
        return instance.registerNewRegion(name, world, box)
    }

    override fun removeRegion(name: String): Region? {
        return instance.removeRegion(name)
    }

    override fun regionAt(bukkitWorld: World, x: Int, y: Int, z: Int): Region? {
        return instance.regionAt(bukkitWorld, x, y, z)
    }

    override fun areaAt(bukkitWorld: World, x: Int, y: Int, z: Int): Area? {
        return instance.areaAt(bukkitWorld, x, y, z)
    }

    override fun saveAll() {
        return instance.saveAll()
    }
}

object RegionsInternal {
    fun register(instance: RegionManager) {
        Regions.internalInstance = instance
    }

    fun unregister() {
        Regions.internalInstance = null
    }
}


val Player.regionUser: User
    get() = requireNotNull(Regions.getUser(this)) { "$name is unregistered bukkit player" }

val World.regionWorld: RegionWorld
    get() = requireNotNull(Regions.getRegionWorld(this)) { "$name is unregistered bukkit world" }

val Block.regionArea: Area
    get() = requireNotNull(Regions.areaAt(world, x, y, z)) {
        "Failed to fetch area at ${world.name} $x $y $z"
    }

val Block.region: Region?
    get() = Regions.regionAt(world, x, y, z)

val Location.regionArea: Area
    get() = requireNotNull(Regions.areaAt(world, blockX, blockY, blockZ)) {
        "Failed to fetch area at ${world.name} $blockX $blockY $blockZ"
    }

val Location.region: Region?
    get() = Regions.regionAt(world, blockX, blockY, blockZ)

val Entity.regionArea: Area
    get() = location.regionArea

val Entity.region: Region?
    get() = location.region