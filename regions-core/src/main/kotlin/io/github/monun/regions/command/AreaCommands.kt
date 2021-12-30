package io.github.monun.regions.command

import io.github.monun.kommand.KommandArgument
import io.github.monun.kommand.KommandSource
import io.github.monun.kommand.PluginKommand
import io.github.monun.regions.api.*
import net.kyori.adventure.text.Component.space
import net.kyori.adventure.text.Component.text
import org.bukkit.configuration.file.YamlConfiguration

object AreaCommands {
    fun register(kommand: PluginKommand) {
        val protectionArgument = KommandArgument.dynamicByMap(Protection.values().associateBy { it.key })
        val permissionArgument = KommandArgument.dynamicByMap(Permission.values().associateBy { it.key })

        kommand.register("area") {
            then("modify") {
                then("area" to RegionsArgument.area()) {
                    then("protection") {
                        then("add", "protection" to protectionArgument) {
                            executes { areaModifyProtectionAdd(it["area"], it["protection"]) }
                        }
                        then("remove", "protection" to protectionArgument) {
                            executes { areaModifyProtectionRemove(it["area"], it["protection"]) }
                        }
                    }
                    then("role") {
                        then("add", "name" to string()) {
                            executes { areaModifyRoleAdd(it["area"], it["role"]) }
                        }
                        then("remove", "role" to RegionsArgument.role("area", false)) {
                            executes { areaModifyRoleRemove(it["area"], it["role"]) }
                        }
                        then("permission") {
                            then("role" to RegionsArgument.role("area", true)) {
                                then("add", "permission" to permissionArgument) {
                                    executes { areaModifyRolePermissionAdd(it["area"], it["role"], it["permission"]) }
                                }
                                then("remove", "permission" to permissionArgument) {
                                    executes {
                                        areaModifyRolePermissionRemove(it["area"], it["role"], it["permissions"])
                                    }
                                }
                            }
                        }
                    }
                    then("member") {
                        then("add", "user" to RegionsArgument.user()) {
                            executes { areaModifyMemberAdd(it["area"], it["user"]) }
                        }
                        then("remove", "member" to RegionsArgument.member("area")) {
                            executes { areaModifyMemberRemove(it["area"], it["member"]) }
                        }
                        then("role", "member" to RegionsArgument.member("area")) {
                            then("add", "role" to RegionsArgument.role("area", false)) {
                                executes { areaModifyMemberRoleAdd(it["area"], it["member"], it["role"]) }
                            }
                            then("remove", "role" to RegionsArgument.role("area", false)) {
                                executes { areaModifyMemberRoleRemove(it["area"], it["member"], it["role"]) }
                            }
                        }
                    }
                }
            }
            then("info", "area" to RegionsArgument.area()) {
                executes { info(it["area"]) }
            }
        }
    }

    private fun KommandSource.areaModifyProtectionAdd(area: Area, protection: Protection) {
        area.addProtections(protection)
        area.save()
        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(protection.key))
                .append(text("] 보호를 추가했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyProtectionRemove(area: Area, protection: Protection) {
        area.removeProtections(protection)
        area.save()
        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(protection.key))
                .append(text("] 보호를 제거했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyRoleAdd(area: Area, role: Role) {
        area.runCatching {
            registerNewRole(name)
        }.onSuccess {
            area.save()
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(area.type.name))
                    .append(text(":"))
                    .append(text(area.name))
                    .append(text("] 지역에 ["))
                    .append(text(role.name))
                    .append(text("] 역할을 추가했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(area.type.name))
                    .append(text(":"))
                    .append(text(area.name))
                    .append(text("] 지역에 ["))
                    .append(text(role.name))
                    .append(text("] 역할 추가에 실패했습니다"))
            }.build())
        }
    }

    private fun KommandSource.areaModifyRoleRemove(area: Area, role: Role) {
        role.delete()
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(role.name))
                .append(text("] 역할을 제거했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyRolePermissionAdd(area: Area, role: Role, perm: Permission) {
        area.addPermissionsToRole(role, perm)
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역의 ["))
                .append(text(role.name))
                .append(text("] 역할에 ["))
                .append(text(perm.key))
                .append(text("] 권한을 추가했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyRolePermissionRemove(area: Area, role: Role, perm: Permission) {
        area.removePermissionsFromRole(role, perm)
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역의 ["))
                .append(text(role.name))
                .append(text("] 역할에 ["))
                .append(text(perm.key))
                .append(text("] 권한을 제거했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyMemberAdd(area: Area, user: User) {
        area.runCatching {
            addMember(user)
        }.onSuccess {
            area.save()

            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(area.type.name))
                    .append(text(":"))
                    .append(text(area.name))
                    .append(text("] 지역에 ["))
                    .append(text(user.name))
                    .append(text("] 구성원을 추가했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(area.type.name))
                    .append(text(":"))
                    .append(text(area.name))
                    .append(text("] 지역에 ["))
                    .append(text(user.name))
                    .append(text("] 구성원 추가를 실패했습니다"))
            }.build())
        }
    }

    private fun KommandSource.areaModifyMemberRemove(area: Area, member: Member) {
        member.delete()
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(member.user.name))
                .append(text("] 구성원을 제거했습니다"))
        }.build())
    }

    private fun KommandSource.areaModifyMemberRoleAdd(area: Area, member: Member, role: Role) {
        area.addRoleToMember(member, role)
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(member.user.name))
                .append(text("] 구성원에게 ["))
                .append(text(role.name))
                .append(text("] 역할을 추가했습니다"))
        }.build())
    }


    private fun KommandSource.areaModifyMemberRoleRemove(area: Area, member: Member, role: Role) {
        area.removeRoleFromMember(member, role)
        area.save()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(area.type.name))
                .append(text(":"))
                .append(text(area.name))
                .append(text("] 지역에 ["))
                .append(text(member.user.name))
                .append(text("] 구성원에게서 ["))
                .append(text(role.name))
                .append(text("] 역할을 제거했습니다"))
        }.build())
    }

    private fun KommandSource.info(area: Area) {
        val config = YamlConfiguration()
        area.save(config)

        sender.sendMessage(text().also { text ->
            text.append(text("\n---- "))
                .append(text(area.type.name))
                .append(space())
                .append(text(area.name))
                .append(text(" Information --------\n"))
                .append(text(config.saveToString()))
        }.build())
    }
}