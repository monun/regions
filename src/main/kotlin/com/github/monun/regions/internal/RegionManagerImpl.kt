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

package com.github.monun.regions.internal

import com.destroystokyo.paper.profile.PlayerProfile
import com.github.monun.regions.api.*
import com.github.monun.regions.plugin.RegionPlugin
import com.google.common.collect.ImmutableList
import com.google.common.collect.MapMaker
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldInitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.io.File
import java.util.*

class RegionManagerImpl(plugin: RegionPlugin) : RegionManager {
    override val cachedUsers: List<User>
        get() = ImmutableList.copyOf(_usersByUniqueId.values)
    override val onlineUsers: Collection<User>
        get() = _onlineUsers
    override val worlds: List<RegionWorld>
        get() = ImmutableList.copyOf(_worldsByName.values)
    override val regions: List<Region>
        get() = ImmutableList.copyOf(_regionsByName.values)
    internal val worldsFolder: File
    internal val regionsFolder: File

    private val _usersByUniqueId = MapMaker().weakValues().makeMap<UUID, UserImpl>()
    private val _usersByPlayer = IdentityHashMap<Player, UserImpl>(Bukkit.getMaxPlayers())
    private val _onlineUsers: Collection<User> = Collections.unmodifiableCollection(_usersByPlayer.values)

    private val _worldsByName = TreeMap<String, RegionWorldImpl>(String.CASE_INSENSITIVE_ORDER)
    private val _worldsByBukkitWorld = IdentityHashMap<World, RegionWorldImpl>()

    private val _regionsByName = TreeMap<String, RegionImpl>(String.CASE_INSENSITIVE_ORDER)

    init {
        val dataFolder = plugin.dataFolder
        worldsFolder = File(dataFolder, "worlds").apply { mkdirs() }
        regionsFolder = File(dataFolder, "regions").apply { mkdirs() }

        loadUsers()
        loadWorlds()
        loadRegions()

        plugin.server.pluginManager.apply {
            registerEvents(InternalListener(), plugin)
        }
    }

    private fun loadUsers() {
        for (player in Bukkit.getOnlinePlayers()) {
            UserImpl(player.uniqueId, player.name).also {
                _usersByPlayer[player] = it
                _usersByUniqueId[it.uniqueId] = it
            }
        }
    }

    private fun loadWorlds() {
        worldsFolder.listFiles { file: File -> !file.isDirectory && file.name.endsWith(".yml") }?.let { files ->
            for (file in files) {
                RegionWorldImpl.runCatching {
                    load(file, this@RegionManagerImpl)
                }.onSuccess { regionWorld ->
                    _worldsByName[regionWorld.name] = regionWorld
                }.onFailure { exception ->
                    exception.printStackTrace()
                    warning("Failed to load world for [${file.name}]")
                }
            }
        }

        for (world in _worldsByName.values) {
            world.linkMembers()
        }

        for (world in Bukkit.getWorlds()) {
            getOrRegisterRegionWorld(world.name).let { regionWorld ->
                regionWorld.bukkitWorld = world
                _worldsByBukkitWorld[world] = regionWorld
            }
        }
    }

    private fun loadRegions() {
        regionsFolder.listFiles { file: File -> !file.isDirectory && file.name.endsWith(".yml") }?.let { files ->
            val loaders = ArrayList<RegionLoader>(files.count())
            for (file in files) {
                kotlin.runCatching {
                    loaders += RegionImpl.load(file, this).also { loader ->
                        val region = loader.region
                        val world = region.parent
                        world.checkOverlap(region.box)
                        world.placeRegion(region)
                        _regionsByName[region.name] = region
                    }
                }.onFailure { exception ->
                    exception.printStackTrace()
                    warning("Failed to load region for [${file.name}]")
                }
            }

            for (region in _regionsByName.values) {
                region.linkMembers()
            }
            //관계도 로드
            for (loader in loaders) {
                loader.complete()
            }
        }
    }

    override fun findUser(uniqueId: UUID): UserImpl? {
        val profile = Bukkit.createProfile(uniqueId)

        if (profile.complete())
            return getUser(profile)

        return null
    }

    override fun getUser(profile: PlayerProfile): UserImpl {
        val uuid = requireNotNull(profile.id)
        val name = requireNotNull(profile.name)

        return getOrCreateUser(uuid, name)
    }

    private fun getOrCreateUser(uniqueId: UUID, name: String): UserImpl {
        return _usersByUniqueId.computeIfAbsent(uniqueId) {
            UserImpl(it, name)
        }
    }

    override fun getUser(player: Player): UserImpl? {
        return _usersByPlayer[player]
    }

    override fun getRegionWorld(name: String): RegionWorldImpl? {
        return _worldsByName[name]
    }

    override fun getRegionWorld(bukkitWorld: World): RegionWorldImpl? {
        return _worldsByBukkitWorld[bukkitWorld]
    }

    internal fun getOrRegisterRegionWorld(name: String): RegionWorldImpl {
        return _worldsByName.computeIfAbsent(name) {
            RegionWorldImpl(this, it).apply {
                setMustBeSave()
            }
        }
    }

    override fun registerNewRegion(name: String, world: RegionWorld, box: RegionBox): RegionImpl {
        require(name !in _regionsByName) { "Name is already in use" }

        world.checkOverlap(box, null)
        val worldImpl = world.toImpl()

        return RegionImpl(this, name, worldImpl, box).apply {
            _regionsByName[name] = this
            worldImpl.placeRegion(this)
            setMustBeSave()
        }
    }

    override fun removeRegion(name: String): RegionImpl? {
        return _regionsByName.remove(name)?.also { region ->
            region.parent.removeRegion(region)
            region.destroy()
        }
    }

    override fun getRegion(name: String): RegionImpl? {
        return _regionsByName[name]
    }

    override fun regionAt(bukkitWorld: World, x: Int, y: Int, z: Int): RegionImpl? {
        return getRegionWorld(bukkitWorld)?.regionAt(x, y, z)
    }

    override fun areaAt(bukkitWorld: World, x: Int, y: Int, z: Int): AreaImpl? {
        return getRegionWorld(bukkitWorld)?.let { world ->
            world.regionAt(x, y, z) ?: world
        }
    }

    override fun saveAll() {
        for (world in _worldsByName.values) {
            world.save()
        }
        for (region in _regionsByName.values) {
            region.save()
        }
    }

    private inner class InternalListener : Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        fun onJoin(event: PlayerJoinEvent) {
            val player = event.player
            val user = getOrCreateUser(player.uniqueId, player.name).apply {
                bukkitPlayer = player
            }

            _usersByPlayer[player] = user
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onQuit(event: PlayerQuitEvent) {
            _usersByPlayer.remove(event.player)?.apply {
                bukkitPlayer = null
                previousLocation = null
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onWorldInit(event: WorldInitEvent) {
            val world = event.world
            getOrRegisterRegionWorld(world.name).apply {
                bukkitWorld = world
                _worldsByBukkitWorld[world] = this
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onWorldLoad(event: WorldLoadEvent) {
            val world = event.world
            getOrRegisterRegionWorld(world.name).apply {
                if (bukkitWorld == null) {
                    bukkitWorld = world
                    _worldsByBukkitWorld[world] = this
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onWorldUnload(event: WorldUnloadEvent) {
            val world = event.world
            _worldsByBukkitWorld.remove(world)?.apply {
                bukkitWorld = null
            }
        }
    }
}