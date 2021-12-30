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

package io.github.monun.regions.command

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import io.github.monun.kommand.KommandArgument
import io.github.monun.kommand.KommandSource
import io.github.monun.kommand.PluginKommand
import io.github.monun.regions.api.Region
import io.github.monun.regions.api.RegionBox
import io.github.monun.regions.api.RegionManager
import io.github.monun.regions.api.Regions
import net.kyori.adventure.text.Component.text
import org.bukkit.entity.Player
import kotlin.math.min
import com.sk89q.worldedit.regions.Region as WorldEditRegion

object RegionCommands {
    private lateinit var manager: RegionManager

    fun register(kommand: PluginKommand, manager: RegionManager) {
        this.manager = manager

        val regionArgument = KommandArgument.dynamicByMap(Regions.regionMap)

        kommand.register("region") {
            then("add") {
                requires { isPlayer }
                then("name" to string()) {
                    executes { regionAdd(it["name"]) }
                }
            }
            then("remove", "region" to regionArgument) {
                executes { regionRemove(it["region"]) }
            }
            then("relocate", "region" to regionArgument) {
                requires { isPlayer }
                executes { regionRelocate(it["region"]) }
            }
            then("parent", "region" to regionArgument) {
                then("add" to regionArgument, "parent" to regionArgument) {
                    executes { regionParentAdd(it["region"], it["parent"]) }
                }
                then("remove" to regionArgument, "parent" to regionArgument) {
                    executes { regionParentRemove(it["region"], it["parent"]) }
                }
            }
            then("list") {
                executes { regionList(0) }

                then("page" to KommandArgument.int(0)) {
                    executes { regionList(it["page"]) }
                }
            }
        }
    }

    private fun KommandSource.regionAdd(name: String) {
        val selection = player.selection

        if (selection == null) {
            sender.sendMessage(text("먼저 생성할 구역을 선택해주세요"))
            return
        }

        val min = selection.minimumPoint
        val max = selection.maximumPoint

        val world = BukkitAdapter.adapt(selection.world)
        val box = RegionBox(min.x, min.y, min.z, max.x, max.y, max.z)

        manager.runCatching {
            registerNewRegion(name, getRegionWorld(world)!!, box)
        }.onSuccess { region ->
            region.save()
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역을 생성했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(name))
                    .append(text("] 구역 생성에 실패했습니다. "))
                    .append(text(it.message.toString()))
            }.build())
        }
    }

    private fun KommandSource.regionRemove(region: Region) {
        region.delete()

        feedback(text().also { text ->
            text.append(text("["))
                .append(text(region.name))
                .append(text("] 구역을 제거했습니다"))
        }.build())
    }

    private fun KommandSource.regionRelocate(region: Region) {
        val selection = player.selection

        if (selection == null) {
            sender.sendMessage(text("먼저 재배치할 구역을 선택해주세요"))
            return
        }

        val min = selection.minimumPoint
        val max = selection.maximumPoint

        val world = BukkitAdapter.adapt(selection.world)
        val box = RegionBox(min.x, min.y, min.z, max.x, max.y, max.z)

        manager.runCatching {
            region.relocate(getRegionWorld(world)!!, box)
        }.onSuccess {
            region.save()
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역을 재배치했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역 재배치를 실패했습니다 "))
                    .append(text(it.message.toString()))
            }.build())
        }
    }

    private fun KommandSource.regionParentAdd(region: Region, parent: Region) {
        region.runCatching {
            addParent(parent)
        }.onSuccess {
            region.save()
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역에 ["))
                    .append(text(parent.name))
                    .append(text("] 부모 구역을 추가했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역에 ["))
                    .append(text(parent.name))
                    .append(text("] 부모 구역 추가를 실패했습니다 "))
                    .append(text(it.message.toString()))
            }.build())
        }
    }

    private fun KommandSource.regionParentRemove(region: Region, parent: Region) {
        region.runCatching {
            removeParent(parent)
        }.onSuccess {
            region.save()
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역에 ["))
                    .append(text(parent.name))
                    .append(text("] 부모 구역을 제거했습니다"))
            }.build())
        }.onFailure {
            feedback(text().also { text ->
                text.append(text("["))
                    .append(text(region.name))
                    .append(text("] 구역에 ["))
                    .append(text(parent.name))
                    .append(text("] 부모 구역 제거를 실패했습니다 "))
                    .append(text(it.message.toString()))
            }.build())
        }
    }

    private fun KommandSource.regionList(page: Int) {
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

val Player.selection: WorldEditRegion?
    get() {
        return try {
            WorldEdit.getInstance().sessionManager[BukkitAdapter.adapt(this)]?.run {
                getSelection(selectionWorld)
            }
        } catch (e: Exception) {
            null
        }
    }