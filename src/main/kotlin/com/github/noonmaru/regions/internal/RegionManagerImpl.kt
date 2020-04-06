package com.github.noonmaru.regions.internal

import com.github.noonmaru.regions.api.*
import com.github.noonmaru.regions.plugin.RegionPlugin
import com.github.noonmaru.tap.mojang.MojangProfile
import com.github.noonmaru.tap.mojang.getProfile
import com.google.common.collect.ImmutableList
import com.google.common.collect.MapMaker
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class RegionManagerImpl(plugin: RegionPlugin) : RegionManager {
    override val cachedUsers: List<User>
        get() = ImmutableList.copyOf(_usersByUniqueId.values)
    override val onlineUsers: List<User>
        get() = ImmutableList.copyOf(_usersByPlayer.values)
    override val worlds: List<RegionWorld>
        get() = ImmutableList.copyOf(_worldsByName.values)
    override val regions: List<Region>
        get() = ImmutableList.copyOf(_regionsByName.values)
    internal val worldsFolder: File
    internal val regionsFolder: File

    private val _usersByUniqueId = MapMaker().weakValues().makeMap<UUID, UserImpl>()
    private val _usersByPlayer = IdentityHashMap<Player, UserImpl>(Bukkit.getMaxPlayers())

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
                kotlin.runCatching {
                    RegionWorldImpl.load(file, this)
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
        return getProfile(uniqueId)?.let { getUser(it) }
    }

    override fun getUser(profile: MojangProfile): UserImpl {
        return _usersByUniqueId.computeIfAbsent(profile.uniqueId) {
            UserImpl(it, profile.name)
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
            RegionWorldImpl(this, it)
        }
    }

    override fun registerNewRegion(name: String, world: RegionWorld, box: RegionBox): RegionImpl {
        require(name !in _regionsByName) { "Name is already in use" }

        world.checkOverlap(box, null)
        val worldImpl = world.toImpl()

        return RegionImpl(this, name, worldImpl, box).also {
            _regionsByName[name] = it
            worldImpl.placeRegion(it)
            it.setMustBeSave()
        }
    }

    override fun removeRegion(name: String): RegionImpl? {
        return _regionsByName.remove(name)?.also {
            it.destroy()
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
}