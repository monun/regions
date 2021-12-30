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

import io.github.monun.regions.api.Member
import io.github.monun.regions.api.Permission
import io.github.monun.regions.api.Role
import com.google.common.collect.ImmutableList
import io.github.monun.regions.extensions.getValue
import io.github.monun.regions.extensions.weak
import org.bukkit.configuration.ConfigurationSection
import java.util.*

class MemberImpl(
    parent: AreaImpl,
    user: UserImpl
) : Member {
    override val parent: AreaImpl by weak(parent)

    override val user: UserImpl by weak(user)

    override val roles: List<RoleImpl>
        get() = ImmutableList.copyOf(_roles)

    override val permissions: Set<Permission>
        get() = Collections.unmodifiableSet(_permissions.clone())

    override var valid: Boolean = true
        private set

    private val _roles = TreeSet<RoleImpl> { o1, o2 -> o1.name.compareTo(o2.name) }

    private var _mustBeUpdatePermissions = false
    internal val _permissions = PermissionSet()
        get() {
            if (_mustBeUpdatePermissions) {
                field.clear()

                _roles.forEachIndexed { index, role ->
                    if (index == 0)
                        field.or(role._permissions)
                    else
                        field.and(role._permissions)
                }
            }

            return field
        }


    override fun hasRole(role: Role): Boolean {
        return _roles.contains(role)
    }

    override fun hasRoles(roles: Collection<Role>): Boolean {
        return _roles.containsAll(roles)
    }

    internal fun addRole(role: Role): Boolean {
        return _roles.add(role.toImpl()).also {
            if (it) {
                _mustBeUpdatePermissions = true
            }
        }
    }

    internal fun removeRole(role: Role): Boolean {
        return _roles.remove(role).also {
            if (it) {
                _mustBeUpdatePermissions = true
            }
        }
    }

    override fun hasPermission(permission: Permission): Boolean {
        return _permissions.contains(permission)
    }

    override fun hasPermissions(permissions: Collection<Permission>): Boolean {
        return _permissions.containsAll(permissions)
    }

    companion object {
        private const val CFG_ROLES = "roles"
    }

    internal fun load(config: ConfigurationSection) {
        val roleNames = config.getStringList(CFG_ROLES)

        for (roleName in roleNames) {
            parent.getRole(roleName)?.takeIf { !it.isPublic }?.also { role ->
                _roles.add(role)
            } ?: user.manager.logger.warning("Unknown [$roleName] role in [${user.name}] at ${parent.type} [${parent.name}]")
        }
    }

    internal fun save(config: ConfigurationSection) {
        config["name"] = user.name
        config[CFG_ROLES] = _roles.map { it.name }
    }

    override fun delete() {
        checkState()

        parent.removeMember(user)
    }

    internal fun destroy() {
        valid = false
    }
}

internal fun Member.toImpl(): MemberImpl {
    return this as MemberImpl
}