package io.github.monun.regions.command

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import io.github.monun.kommand.KommandArgument
import io.github.monun.kommand.KommandSource
import io.github.monun.kommand.PluginKommand
import io.github.monun.kommand.node.KommandNode
import io.github.monun.kommand.wrapper.Position3D
import io.github.monun.regions.api.*
import io.github.monun.regions.internal.RegionManagerImpl
import net.kyori.adventure.text.Component.space
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions.floor
import kotlin.math.min

object AreaCommands {
    fun register(kommand: PluginKommand, manager: RegionManagerImpl) = kommand.register("area") {
        val worldArgument = dynamic { _, input ->
            manager.getRegionWorld(input)
        }.apply {
            suggests {
                suggest(manager.worlds, { it.name })
            }
        }

        val regionArgument = dynamic { _, input ->
            manager.getRegion(input)
        }.apply {
            suggests {
                suggest(manager.regions, { it.name })
            }
        }

        val protectionArgument = dynamic { _, input ->
            Protection.values().find { it.key.equals(input, true) }
        }.apply {
            suggests {
                suggest(Protection.values().asList(), { it.key })
            }
        }

        val permissionArgument = dynamic { _, input ->
            Permission.values().find { it.key.equals(input, true) }
        }.apply {
            suggests {
                suggest(Permission.values().asList(), { it.key })
            }
        }

        val userArgument = dynamic { _, input ->
            val profile = Bukkit.createProfile(input)
            if (profile.complete()) manager.getUser(profile) else null
        }.apply {
            suggests {
                suggest(Bukkit.getOnlinePlayers(), { it.name })
            }
        }

        requires { isOp }

        fun KommandNode.registerCommonCommands(areaArgumentName: String, areaArgument: KommandArgument<out Area>) {
            fun role(includePublicRole: Boolean) = dynamic { context, input ->
                context.runCatching {
                    context.get<Area>(areaArgumentName).getRole(input)
                }.getOrNull()?.takeIf { role ->
                    includePublicRole || !role.isPublic
                }
            }.apply {
                suggests { context ->
                    context.runCatching {
                        context.get<Area>(areaArgumentName)
                    }.getOrNull()?.let { area ->
                        if (includePublicRole) suggest(area.publicRole.name)
                        suggest(area.roles.map { it.name })
                    }
                }
            }

            val customRoleArgument = role(false)

            val memberArgument = dynamic { context, input ->
                context.runCatching {
                    get<Area>(areaArgumentName)
                }.getOrNull()?.getMember(input)
            }.apply {
                suggests { context ->
                    context.runCatching {
                        get<Area>(areaArgumentName)
                    }.getOrNull()?.let { area ->
                        suggest(area.members.map { member -> member.user.name })
                    }
                }
            }
            then("info", areaArgumentName to areaArgument) {
                executes { areaInfo(it[areaArgumentName]) }
            }
            then("modify", areaArgumentName to areaArgument) {
                then("protection") {
                    then("add", "protection" to protectionArgument) {
                        executes { areaModifyProtectionAdd(it[areaArgumentName], it["protection"]) }
                    }
                    then("remove", "protection" to protectionArgument) {
                        executes { areaModifyProtectionRemove(it[areaArgumentName], it["protection"]) }
                    }
                }
                then("role") {
                    then("add", "name" to string()) {
                        executes { areaModifyRoleAdd(it[areaArgumentName], it["name"]) }
                    }
                    then("remove", "role" to customRoleArgument) {
                        executes { areaModifyRoleRemove(it[areaArgumentName], it["role"]) }
                    }
                    then("permission", "role" to role(true)) {
                        then("add", "permission" to permissionArgument) {
                            executes {
                                areaModifyRolePermissionAdd(
                                    it[areaArgumentName],
                                    it["role"],
                                    it["permission"]
                                )
                            }
                        }
                        then("remove", "permission" to permissionArgument) {
                            executes {
                                areaModifyRolePermissionRemove(it[areaArgumentName], it["role"], it["permission"])
                            }
                        }
                    }
                }
                then("member") {
                    then("add", "user" to userArgument) {
                        executes { areaModifyMemberAdd(it[areaArgumentName], it["user"]) }
                    }
                    then("remove", "member" to memberArgument) {
                        executes { areaModifyMemberRemove(it[areaArgumentName], it["member"]) }
                    }
                    then("role", "member" to memberArgument) {
                        then("add", "role" to customRoleArgument) {
                            executes { areaModifyMemberRoleAdd(it[areaArgumentName], it["member"], it["role"]) }
                        }
                        then("remove", "role" to customRoleArgument) {
                            executes { areaModifyMemberRoleRemove(it[areaArgumentName], it["member"], it["role"]) }
                        }
                    }
                }
            }
        }

        then("world") {
            registerCommonCommands("world", worldArgument)
        }

        then("region") {
            then("add", "name" to string()) {
                executes { regionAdd(manager, it["name"]) }

                then("world" to dimension(), "p1" to position(), "p2" to position()) {
                    executes { regionAdd(manager, it["name"], it["world"], it["p1"], it["p2"]) }
                }
            }
            then("remove", "region" to regionArgument) {
                executes { regionRemove(it["region"]) }
            }
            then("relocate", "region" to regionArgument) {
                executes { regionRelocate(it["region"]) }

                then("world" to dimension(), "p1" to position(), "p2" to position()) {
                    executes { regionRelocate(it["region"], it["world"], it["p1"], it["p2"]) }
                }
            }
            then("parent", "region" to regionArgument) {
                then("add", "parent" to regionArgument) {
                    executes { regionParentAdd(it["region"], it["parent"]) }
                }
                then("remove", "parent" to regionArgument) {
                    executes { regionParentRemove(it["region"], it["parent"]) }
                }
            }
            then("list") {
                executes { regionList(manager, 0) }

                then("page" to int(0)) {
                    executes { regionList(manager, it["page"]) }
                }
            }

            registerCommonCommands("region", regionArgument)
        }
    }

    private fun KommandSource.areaModifyProtectionAdd(area: Area, protection: Protection) {
        area.addProtections(protection)
        area.save()

        feedback(text("[${area.name}] ${area.type}에 [${protection.key}] 보호를 추가했습니다"))
    }

    private fun KommandSource.areaModifyProtectionRemove(area: Area, protection: Protection) {
        area.removeProtections(protection)
        area.save()

        feedback(text("[${area.name}] ${area.type}에 [${protection.key}] 보호를 제거했습니다"))
    }

    private fun KommandSource.areaModifyRoleAdd(area: Area, name: String) {
        area.runCatching {
            registerNewRole(name)
        }.onSuccess {
            area.save()

            feedback(text("[${area.name}] ${area.type}에 [${name}] 역할을 생성했습니다"))
        }.onFailure {
            feedback(text("[${area.name}] ${area.type}에 [${name}] 역할 생성을 실패했습니다"))
        }
    }

    private fun KommandSource.areaModifyRoleRemove(area: Area, role: Role) {
        role.delete()
        area.save()

        feedback(text("[${area.name}] ${area.type}의 [${role.name}] 역할을 제거했습니다"))
    }

    private fun KommandSource.areaModifyRolePermissionAdd(area: Area, role: Role, perm: Permission) {
        area.addPermissionsToRole(role, perm)
        area.save()

        feedback(text("[${area.name}] ${area.type}의 [${role.name}] 역할에 [${perm.key}] 권한을 추가했습니다"))
    }

    private fun KommandSource.areaModifyRolePermissionRemove(area: Area, role: Role, perm: Permission) {
        area.removePermissionsFromRole(role, perm)
        area.save()

        feedback(text("[${area.name}] ${area.type}의 [${role.name}] 역할에 [${perm.key}] 권한을 제거했습니다"))
    }

    private fun KommandSource.areaModifyMemberAdd(area: Area, user: User) {
        area.runCatching {
            addMember(user)
        }.onSuccess {
            area.save()

            feedback(text("[${area.name}] ${area.type}에 [${user.name}] 구성원을 추가했습니다"))
        }.onFailure {
            feedback(text("[${area.name}] ${area.type}에 [${user.name}] 구성원을 추가에 실패했습니다 ${it.message}"))
        }
    }

    private fun KommandSource.areaModifyMemberRemove(area: Area, member: Member) {
        member.delete()
        area.save()

        feedback(text("[${area.name}] ${area.type}에 [${member.user.name}] 구성원을 제거했습니다"))
    }

    private fun KommandSource.areaModifyMemberRoleAdd(area: Area, member: Member, role: Role) {
        area.addRoleToMember(member, role)
        area.save()

        feedback(text("[${area.name}] ${area.type}의 [${member.user.name}] 구성원에게 ${role.name} 역할을 추가했습니다"))
    }


    private fun KommandSource.areaModifyMemberRoleRemove(area: Area, member: Member, role: Role) {
        area.removeRoleFromMember(member, role)
        area.save()

        feedback(text("[${area.name}] ${area.type}의 [${member.user.name}] 구성원에게 ${role.name} 역할을 제거했습니다"))
    }

    private fun KommandSource.areaInfo(area: Area) {
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

    private fun KommandSource.regionAdd(manager: RegionManagerImpl, name: String) {
        if (!isPlayer) {
            sender.sendMessage(text("콘솔에서 사용 할 수 없는 명령입니다."))
            return
        }

        val selection = player.selection

        if (selection == null) {
            sender.sendMessage(text("먼저 생성할 구역을 선택해주세요"))
            return
        }

        val min = selection.minimumPoint
        val max = selection.maximumPoint
        val world = BukkitAdapter.adapt(selection.world)

        regionAdd(manager, name, manager.getRegionWorld(world)!!, RegionBox(min.x, min.y, min.z, max.x, max.y, max.z))
    }

    private fun KommandSource.regionAdd(
        manager: RegionManagerImpl,
        name: String,
        world: World,
        p1: Position3D,
        p2: Position3D
    ) {
        regionAdd(
            manager, name, manager.getRegionWorld(world)!!, RegionBox(
                floor(p1.x), floor(p1.y), floor(p1.z),
                floor(p2.x), floor(p2.y), floor(p2.z)
            )
        )
    }

    private fun KommandSource.regionAdd(manager: RegionManagerImpl, name: String, world: RegionWorld, box: RegionBox) {
        manager.runCatching {
            registerNewRegion(name, world, box)
        }.onSuccess { region ->
            region.save()

            feedback(text("[${region.name}] 구역을 생성했습니다"))
        }.onFailure {
            feedback(text("[$name] 구역 생성에 실패했습니다 ${it.message}"))
        }
    }

    private fun KommandSource.regionRemove(region: Region) {
        region.delete()

        feedback(text("[${region.name}] 구역을 제거했습니다"))
    }

    private fun KommandSource.regionRelocate(region: Region) {
        if (!isPlayer) {
            sender.sendMessage(text("콘솔에서 사용 할 수 없는 명령입니다."))
            return
        }

        val selection = player.selection

        if (selection == null) {
            sender.sendMessage(text("먼저 재배치할 구역을 선택해주세요"))
            return
        }

        val min = selection.minimumPoint
        val max = selection.maximumPoint
        val world = BukkitAdapter.adapt(selection.world)

        regionRelocate(
            region,
            region.manager.getRegionWorld(world)!!,
            RegionBox(min.x, min.y, min.z, max.x, max.y, max.z)
        )
    }


    private fun KommandSource.regionRelocate(region: Region, world: World, p1: Position3D, p2: Position3D) {
        regionRelocate(
            region, region.manager.getRegionWorld(world)!!, RegionBox(
                floor(p1.x),
                floor(p1.y),
                floor(p1.z),
                floor(p2.x),
                floor(p2.y),
                floor(p2.z)
            )
        )
    }

    private fun KommandSource.regionRelocate(region: Region, world: RegionWorld, box: RegionBox) {
        region.runCatching {
            relocate(world, box)
        }.onSuccess {
            region.save()

            feedback(text("[${region.name}] 구역을 재배치했습니다"))
        }.onFailure {
            feedback(text("[${region.name}] 구역 재배치에 실패했습니다 ${it.message}"))
        }
    }

    private fun KommandSource.regionParentAdd(region: Region, parent: Region) {
        region.runCatching {
            addParent(parent)
        }.onSuccess {
            region.save()

            feedback(text("[${region.name}] 구역에 [${parent.name}] 부모 구역을 추가했습니다"))
        }.onFailure {
            feedback(text("[${region.name}] 구역에 [${parent.name}] 부모 구역 추가를 실패했습니다 ${it.message}"))
        }
    }

    private fun KommandSource.regionParentRemove(region: Region, parent: Region) {
        region.runCatching {
            removeParent(parent)
        }.onSuccess {
            region.save()

            feedback(text("[${region.name}] 구역에 [${parent.name}] 부모 구역을 제거했습니다"))
        }.onFailure {
            feedback(text("[${region.name}] 구역에 [${parent.name}] 부모 구역 제거에 실패했습니다 ${it.message}"))
        }
    }

    private fun KommandSource.regionList(manager: RegionManager, page: Int) {
        val length = if (isPlayer) 10 else 20
        val regions = manager.regions
        val lastPage = regions.count() / length

        val start = page.coerceIn(0, lastPage) * length
        val end = min(start + length, regions.count())

        for (i in start until end) {
            val region = regions[i]

            sender.sendMessage(text().also { text ->
                text.append(text(i + 1))
                    .append(text(". "))
                    .append(text(region.name))
            }.build())
        }
    }
}

private val Player.selection: com.sk89q.worldedit.regions.Region?
    get() {
        return try {
            WorldEdit.getInstance().sessionManager[BukkitAdapter.adapt(this)]?.run {
                getSelection(selectionWorld)
            }
        } catch (e: Exception) {
            null
        }
    }