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

import io.github.monun.regions.api.Permission
import io.github.monun.regions.api.Role
import io.github.monun.regions.extensions.getValue
import io.github.monun.regions.extensions.weak
import org.bukkit.configuration.ConfigurationSection
import java.util.*

class RoleImpl(
    parent: AreaImpl,
    override val name: String,
    override val isPublic: Boolean = false
) : Role {
    override val parent: AreaImpl by weak(parent)

    override val permissions: Set<Permission>
        get() = Collections.unmodifiableSet(_permissions.clone())

    override var valid: Boolean = true
        private set

    internal val _permissions = PermissionSet()

    override fun hasPermission(permission: Permission): Boolean {
        return _permissions.contains(permission)
    }

    override fun hasPermissions(permissions: Collection<Permission>): Boolean {
        return _permissions.containsAll(permissions)
    }

    internal fun addPermissions(permissions: Collection<Permission>): Boolean {
        return _permissions.addAll(permissions)
    }

    internal fun removePermissions(permissions: Collection<Permission>): Boolean {
        return _permissions.removeAll(permissions)
    }

    companion object {
        private const val CFG_PERMISSIONS = "permissions"
    }

    internal fun save(config: ConfigurationSection) {
        val section = config.createSection(CFG_PERMISSIONS)

        for (permission in Permission.values()) {
            section.set(permission.key, permission in _permissions)
        }
    }

    internal fun load(config: ConfigurationSection) {
        config.getConfigurationSection(CFG_PERMISSIONS)?.let { section ->
            Permission.values().forEach { permission ->
                if (section.getBoolean(permission.key)) {
                    _permissions.add(permission)
                }
            }
        }
    }

    override fun delete() {
        checkState()
        require(!isPublic) { "Cannot delete public role" }

        parent.removeRole(name)
    }

    internal fun destroy() {
        valid = false
    }

    override fun toString(): String {
        return "$name ${permissions.joinToString(prefix = "[", postfix = "]") { it.key }}"
    }
}

internal fun Role.toImpl(): RoleImpl {
    return this as RoleImpl
}