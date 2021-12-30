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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.github.monun.regions.api.*
import io.github.monun.regions.extensions.getBox
import io.github.monun.regions.extensions.getStringValue
import io.github.monun.regions.extensions.setBox
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

class RegionImpl(
    manager: RegionManagerImpl,
    name: String,
    parent: RegionWorldImpl,
    box: RegionBox
) : AreaImpl(manager, name), Region, Checkable {
    override val file: File
        get() = File(manager.regionsFolder, "$name.yml")
    override val type: Area.Type
        get() = Area.Type.REGION
    override var parent: RegionWorldImpl = parent
        private set
    override var box: RegionBox = box
        private set

    override val parents: List<RegionImpl>
        get() = ImmutableList.copyOf(_parents)
    override val children: List<RegionImpl>
        get() = ImmutableList.copyOf(_children)

    override var valid: Boolean = true
        private set

    private val _parents = LinkedHashSet<RegionImpl>(0)
    private val _children = LinkedHashSet<RegionImpl>(0)

    override fun addProtections(protections: Collection<Protection>): Boolean {
        checkState()
        return super<AreaImpl>.addProtections(protections)
    }

    override fun removeProtections(protections: Collection<Protection>): Boolean {
        checkState()
        return super<AreaImpl>.removeProtections(protections)
    }

    override fun registerNewRole(name: String): Role {
        checkState()
        return super.registerNewRole(name)
    }

    override fun addPermissionsToRole(role: Role, permissions: Collection<Permission>): Boolean {
        checkState()
        return super<AreaImpl>.addPermissionsToRole(role, permissions)
    }

    override fun removePermissionsFromRole(role: Role, permissions: Collection<Permission>): Boolean {
        checkState()
        return super<AreaImpl>.removePermissionsFromRole(role, permissions)
    }

    override fun addMember(user: User): Member {
        checkState()
        return super.addMember(user)
    }

    override fun addOrGetMember(user: User): Member {
        checkState()
        return super.addOrGetMember(user)
    }

    override fun addRoleToMember(member: Member, role: Role): Boolean {
        checkState()
        return super.addRoleToMember(member, role)
    }

    override fun removeRoleFromMember(member: Member, role: Role): Boolean {
        checkState()
        return super.removeRoleFromMember(member, role)
    }

    override fun relocate(newWorld: RegionWorld, newBox: RegionBox) {
        checkState()

        val world = newWorld.toImpl()

        world.checkOverlap(newBox, this)
        world.removeRegion(this)
        world.placeRegion(this, newBox)

        parent = world
        box = newBox
    }

    override fun getDirectAncestors(): Set<RegionImpl> {
        return ImmutableSet.copyOf(
            HashMap<RegionImpl, Boolean>().also { map ->
                computeDirectAncestors(map)
            }.keys
        )
    }

    private fun computeDirectAncestors(map: MutableMap<RegionImpl, Boolean>) {
        for (other in _parents) {
            val result = map.compute(other) { _, value ->
                value == null
            }!!

            if (result) {
                other.computeDirectAncestors(map)
            }
        }
    }

    override fun getAllDescendants(): Set<RegionImpl> {
        return HashSet<RegionImpl>().apply {
            computeAllDescendants(this)
        }
    }

    private fun computeAllDescendants(set: MutableSet<RegionImpl>) {
        for (child in _children) {
            if (set.add(child)) {
                child.computeAllDescendants(set)
            }
        }
    }

    private fun checkCircularRelationship(region: Region) {
        require(region !in getAllDescendants()) { "Circular relationship" }
    }

    override fun addParent(region: Region): Boolean {
        checkCircularRelationship(region)

        val regionImpl = region.toImpl()

        return _parents.add(regionImpl).also {
            if (it) {
                regionImpl._children.add(this)
                setMustBeSave()
                resetAllPlayerPermissions()
            }
        }
    }

    override fun resetPlayerPermissions(player: Player) {
        super.resetPlayerPermissions(player)

        for (child in getAllDescendants()) {
            child.resetPlayerPermissions(player)
        }
    }

    override fun resetAllPlayerPermissions() {
        super.resetAllPlayerPermissions()

        for (child in getAllDescendants()) {
            child.resetAllPlayerPermissions()
        }
    }

    override fun removeParent(region: Region): Boolean {
        checkState()

        return _parents.remove(region).also {
            region.toImpl()._children.remove(this)
            setMustBeSave()
            resetAllPlayerPermissions()
        }
    }

    override fun computePlayerPermissions(player: Player): PermissionSet {
        return computePlayerPermissions(manager.getUser(player)!!, player)
    }

    private fun computePlayerPermissions(user: UserImpl, player: Player): PermissionSet {
        val ancestors = getDirectAncestors()
        val permissions = PermissionSet()

        ancestors.forEachIndexed { index, region ->
            val parent = region.getOrComputePlayerPermissions(user, player)

            if (index == 0)
                permissions.or(parent)
            else
                permissions.and(parent)
        }

        val member = getMember(user)
        val memberPermissions = member?._permissions ?: publicRole._permissions

        permissions.or(memberPermissions)

        return permissions
    }

    private fun getOrComputePlayerPermissions(user: UserImpl, player: Player): PermissionSet {
        return playerPermissions.computeIfAbsent(player) {
            computePlayerPermissions(user, player)
        }
    }

    override fun init() {
        parent.let { world ->
            addProtections(world.protections)
            publicRole.addPermissions(world.publicRole.permissions)
        }
    }

    override fun save(): Boolean {
        checkState()

        return super.save()
    }

    companion object {
        private const val CFG_WORLD = "world"
        private const val CFG_BOX = "box"
        private const val CFG_PARENTS = "parents"

        internal fun load(file: File, manager: RegionManagerImpl): RegionLoader {
            val name = file.name.removeSuffix(".yml")
            val config = YamlConfiguration.loadConfiguration(file)
            val world = manager.getOrRegisterRegionWorld(config.getStringValue(CFG_WORLD))
            val box = config.getBox(CFG_BOX)

            val region = RegionImpl(manager, name, world, box).apply {
                load(config)
            }

            val parentNames = config.getStringList(CFG_PARENTS).toSortedSet(String.CASE_INSENSITIVE_ORDER)

            return RegionLoader(region, parentNames)
        }
    }

    override fun save(config: ConfigurationSection) {
        config[CFG_WORLD] = parent.name
        config.setBox(CFG_BOX, box)
        config[CFG_PARENTS] = _parents.map { it.name }
        super.save(config)
    }

    internal fun linkParents(parentNames: Set<String>) {
        val descendants = getAllDescendants()

        for (parentName in parentNames) {
            manager.getRegion(parentName)?.let { parent ->
                when {
                    parent === this -> {
                        manager.logger.warning("Self parent $name")
                    }
                    parent in descendants -> {
                        manager.logger.warning("Detect circular relationship [${parent.name}] -> [$name] -> [${parent.name}]")
                    }
                    else -> {
                        _parents += parent
                        parent._children.add(this)
                    }
                }
            }
        }
    }

    override fun delete() {
        checkState()

        manager.removeRegion(name)
    }

    internal fun destroy() {
        valid = false
        file.delete()
    }
}

internal fun Region.toImpl(): RegionImpl {
    return this as RegionImpl
}

class RegionLoader(val region: RegionImpl, private val parentNames: Set<String>) {
    fun complete() {
        region.linkParents(parentNames)
    }
}