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

package com.github.monun.regions.command

import com.github.monun.kommand.KommandContext
import com.github.monun.kommand.KommandDispatcherBuilder
import com.github.monun.kommand.argument.KommandArgument
import com.github.monun.kommand.argument.KommandArgument.Companion.TOKEN
import com.github.monun.kommand.argument.map
import com.github.monun.kommand.argument.string
import com.github.monun.kommand.argument.suggest
import com.github.monun.kommand.sendFeedback
import com.github.monun.regions.api.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.util.*

object AreaArgument : KommandArgument<Area> {
    override val parseFailMessage: String
        get() = "$TOKEN 지역을 찾지 못했습니다."

    override fun parse(context: KommandContext, param: String): Area? {
        val colon = param.indexOf(':')

        if (colon >= 0) {
            val type = param.substring(0, colon)
            val name = param.substring(colon + 1)

            if (type == "world") {
                return Regions.manager.getRegionWorld(name)
            } else if (type == "region") {
                return Regions.manager.getRegion(name)
            }
        }

        return null
    }

    override fun suggest(context: KommandContext, target: String): Collection<String> {
        val worlds = Regions.manager.worlds
        val regions = Regions.manager.regions
        val list = ArrayList<String>()

        for (world in worlds) {
            val name = world.name
            val suggestion = "world:$name"

            if (name.startsWith(target, true) || suggestion.startsWith(target, true))
                list += suggestion
        }

        for (region in regions) {
            val name = region.name
            val suggestion = "region:$name"

            if (name.startsWith(target, true) || suggestion.startsWith(target, true))
                list += suggestion
        }

        return list
    }
}

class RoleArgument(
    val areaArgumentName: String,
    val withPublicRole: Boolean
) : KommandArgument<Role> {
    override val parseFailMessage: String
        get() = "$TOKEN 역할을 찾지 못했습니다."

    override fun parse(context: KommandContext, param: String): Role? {
        val area = context.parseOrNullArgument<Area>(areaArgumentName)

        return area?.getRole(param)?.takeIf {
            if (withPublicRole)
                true
            else
                !it.isPublic
        }
    }

    override fun suggest(context: KommandContext, target: String): Collection<String> {
        val area = context.parseOrNullArgument<Area>(areaArgumentName)

        if (area != null) {
            if (!withPublicRole)
                return area.roles.suggest(target) { it.name }

            val roles = area.roles
            val list = ArrayList<String>(roles.count() + 1)

            list += area.publicRole.name
            for (role in roles) {
                list += role.name
            }

            return list.suggest(target)
        }

        return emptyList()
    }
}

object UserArgument : KommandArgument<User> {
    override val parseFailMessage: String
        get() = "$TOKEN 유저를 찾지 못했습니다."

    override fun parse(context: KommandContext, param: String): User? {
        val profile = Bukkit.createProfile(param)

        if (profile.complete()) {
            return Regions.manager.getUser(profile)
        }

        return null
    }

    override fun suggest(context: KommandContext, target: String): Collection<String> {
        return Bukkit.getOnlinePlayers().suggest(target) { it.name }
    }
}

class MemberArgument(
    private val areaArgumentName: String
) : KommandArgument<Member> {
    override val parseFailMessage: String
        get() = "$TOKEN 구성원을 찾지 못했습니다."

    override fun parse(context: KommandContext, param: String): Member? {
        val area = context.parseOrNullArgument<Area>(areaArgumentName) ?: return null

        return area.getMember(param)
    }

    override fun suggest(context: KommandContext, target: String): Collection<String> {
        val area = context.parseOrNullArgument<Area>(areaArgumentName) ?: return emptyList()

        return area.members.suggest(target) { it.user.name }
    }
}

fun member(areaArgumentName: String): MemberArgument {
    return MemberArgument(areaArgumentName)
}

fun role(areaArgumentName: String, withPublicRole: Boolean): RoleArgument {
    return RoleArgument(areaArgumentName, withPublicRole)
}

object AreaCommands {
    fun register(builder: KommandDispatcherBuilder) {
        builder.register("area") {
            val protectionArg = map(
                Protection.values()
                    .associateByTo(TreeMap<String, Protection>(String.CASE_INSENSITIVE_ORDER)) { it.key })
            val permissionArg = map(
                Permission.values().associateByTo(TreeMap<String, Permission>(String.CASE_INSENSITIVE_ORDER)) { it.key }
            )

            then("modify") {
                then("area" to AreaArgument) {
                    then("protection") {
                        then("add") {
                            then("protection" to protectionArg) {
                                executes {
                                    addProtection(
                                        it.sender,
                                        it.parseArgument("area"),
                                        it.parseArgument("protection")
                                    )
                                }
                            }
                        }
                        then("remove") {
                            then("protection" to protectionArg) {
                                executes {
                                    removeProtection(
                                        it.sender,
                                        it.parseArgument("area"),
                                        it.parseArgument("protection")
                                    )
                                }
                            }
                        }
                    }
                    then("role") {
                        then("add") {
                            then("name" to string()) {
                                executes {
                                    addRole(
                                        it.sender,
                                        it.parseArgument("area"),
                                        it.getArgument("name")
                                    )
                                }
                            }
                        }
                        then("remove") {
                            then("role" to role("area", false)) {
                                executes {
                                    removeRole(it.sender, it.parseArgument("role"))
                                }
                            }
                        }
                        then("permission") {
                            then("role" to role("area", true)) {
                                then("add") {
                                    then("perm" to permissionArg) {
                                        executes {
                                            addPermissionToRole(
                                                it.sender,
                                                it.parseArgument("role"),
                                                it.parseArgument("perm")
                                            )
                                        }
                                    }
                                }
                                then("remove") {
                                    then("perm" to permissionArg) {
                                        executes {
                                            removePermissionFromRole(
                                                it.sender,
                                                it.parseArgument("role"),
                                                it.parseArgument("perm")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    then("member") {
                        then("add") {
                            then("user" to UserArgument) {
                                executes {
                                    addMember(
                                        it.sender,
                                        it.parseArgument("area"),
                                        it.parseArgument("user")
                                    )
                                }
                            }
                        }
                        then("remove") {
                            then("member" to member("area")) {
                                executes {
                                    removeMember(it.sender, it.parseArgument("member"))
                                }
                            }
                        }
                        then("role") {
                            then("member" to member("area")) {
                                then("add") {
                                    then("role" to role("area", false)) {
                                        executes {
                                            addRoleToMember(
                                                it.sender,
                                                it.parseArgument("member"),
                                                it.parseArgument("role")
                                            )
                                        }
                                    }
                                }
                                then("remove") {
                                    then("role" to role("area", false)) {
                                        executes {
                                            removeRoleFromMember(
                                                it.sender,
                                                it.parseArgument("member"),
                                                it.parseArgument("role")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            then("info") {
                then("area" to AreaArgument) {
                    executes {
                        printInfo(it.sender, it.parseArgument("area"))
                    }
                }
            }
        }
    }

    private fun addProtection(sender: CommandSender, area: Area, protection: Protection) {
        area.addProtections(protection)
        area.save()
        sender.sendFeedback("[${area.type}:${area.name}] 지역에 [${protection.key}] 보호를 추가했습니다.")
    }

    private fun removeProtection(sender: CommandSender, area: Area, protection: Protection) {
        area.removeProtections(protection)
        area.save()
        sender.sendFeedback("[${area.type}:${area.name}] 지역에서 [${protection.key}] 보호를 제거했습니다.")
    }

    private fun addRole(sender: CommandSender, area: Area, name: String) {
        area.runCatching {
            registerNewRole(name)
        }.onSuccess {
            area.save()
            sender.sendFeedback("[${area.type}:${area.name}] 지역에 [$name] 역할을 등록했습니다.")
        }.onFailure {
            sender.sendFeedback("${ChatColor.RED}[${area.type}:${area.name}] 지역에 [$name] 역할 등록을 실패했습니다. ${it.message}")
        }
    }

    private fun removeRole(sender: CommandSender, role: Role) {
        val area = role.parent
        role.delete()
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역에서 [${role.name}] 역할을 제거했습니다.")
    }

    private fun addPermissionToRole(sender: CommandSender, role: Role, perm: Permission) {
        val area = role.parent
        area.addPermissionsToRole(role, perm)
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역의 [${role.name}] 역할에 [${perm.key}] 권한을 추가했습니다.")
    }

    private fun removePermissionFromRole(sender: CommandSender, role: Role, perm: Permission) {
        val area = role.parent
        area.removePermissionsFromRole(role, perm)
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역의 [${role.name}] 역할에서 [${perm.key}] 권한을 제거했습니다.")
    }

    private fun addMember(sender: CommandSender, area: Area, user: User) {
        area.runCatching {
            addMember(user)
        }.onSuccess {
            area.save()
            sender.sendFeedback("[${area.type}:${area.name}] 지역에 [${user.name}] 구성원을 추가했습니다.")
        }.onFailure {
            sender.sendFeedback("${ChatColor.RED}[${area.type}:${area.name}] 지역에 [${user.name}] 구성원 추가를 실패했습니다. ${it.message}")
        }
    }

    private fun removeMember(sender: CommandSender, member: Member) {
        val area = member.parent
        member.delete()
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역에서 [${member.user.name}] 구성원을 제거했습니다.")
    }

    private fun addRoleToMember(sender: CommandSender, member: Member, role: Role) {
        val area = member.parent
        area.addRoleToMember(member, role)
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역에 [${member.user.name}] 구성원에게 [${role.name}] 역할을 추가했습니다.")
    }

    private fun removeRoleFromMember(sender: CommandSender, member: Member, role: Role) {
        val area = member.parent
        area.removeRoleFromMember(member, role)
        area.save()

        sender.sendFeedback("[${area.type}:${area.name}] 지역에 [${member.user.name}] 구성원에게서 [${role.name}] 역할을 제거했습니다.")
    }

    private fun printInfo(sender: CommandSender, area: Area) {
        val config = YamlConfiguration()
        area.save(config)

        sender.sendMessage("\n---- ${area.type} ${area.name} Information --------\n${config.saveToString()}")
    }
}