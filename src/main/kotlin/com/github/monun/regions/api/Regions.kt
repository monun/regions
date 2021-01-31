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

import com.github.monun.regions.internal.RegionManagerImpl
import com.github.monun.regions.plugin.RegionPlugin
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.logging.Logger

object Regions {
    lateinit var manager: RegionManager
        private set

    internal fun initialize(plugin: RegionPlugin) {
        Logger = plugin.logger
        manager = RegionManagerImpl(plugin)
    }
}

internal lateinit var Logger: Logger

internal fun warning(name: String) {
    Logger.warning(name)
}

internal fun info(name: String) {
    Logger.info(name)
}

val Player.regionUser: User
    get() = requireNotNull(Regions.manager.getUser(this)) { "$name is unregistered bukkit player" }

val World.regionWorld: RegionWorld
    get() = requireNotNull(Regions.manager.getRegionWorld(this)) { "$name is unregistered bukkit world" }

val Block.regionArea: Area
    get() = requireNotNull(Regions.manager.areaAt(world, x, y, z)) {
        "Failed to fetch area at ${world.name} $x $y $z"
    }

val Block.region: Region?
    get() = Regions.manager.regionAt(world, x, y, z)

val Location.regionArea: Area
    get() = requireNotNull(Regions.manager.areaAt(world, blockX, blockY, blockZ)) {
        "Failed to fetch area at ${world.name} $blockX $blockY $blockZ"
    }

val Location.region: Region?
    get() = Regions.manager.regionAt(world, blockX, blockY, blockZ)

val Entity.regionArea: Area
    get() = location.regionArea

val Entity.region: Region?
    get() = location.region