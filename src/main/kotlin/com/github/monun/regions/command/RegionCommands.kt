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

import com.github.monun.kommand.KommandDispatcherBuilder
import com.github.monun.kommand.KommandSyntaxException
import com.github.monun.kommand.argument.integer
import com.github.monun.kommand.argument.map
import com.github.monun.kommand.argument.string
import com.github.monun.kommand.sendFeedback
import com.github.monun.regions.api.Region
import com.github.monun.regions.api.RegionBox
import com.github.monun.regions.api.Regions
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.min
import com.sk89q.worldedit.regions.Region as WorldEditRegion

object RegionCommands {
    fun register(builder: KommandDispatcherBuilder) {
        builder.register("region") {
            val manager = Regions.manager
            val regionArg = map(
                {
                    manager.getRegion(it)
                },
                {
                    manager.regions.map { it.name }
                }
            )

            then("add") {
                require {
                    this is Player
                }
                then("name" to string()) {
                    executes {
                        addRegion(it.sender as Player, it.getArgument("name"))
                    }
                }
            }
            then("remove") {
                then("region" to regionArg) {
                    executes {
                        removeRegion(it.sender, it.parseArgument("region"))
                    }
                }
            }
            then("relocate") {
                then("region" to regionArg) {
                    require {
                        this is Player
                    }
                    executes {
                        relocateRegion(it.sender as Player, it.parseArgument("region"))
                    }
                }
            }
            then("parent") {
                then("region" to regionArg) {
                    then("parent" to regionArg) {
                        executes {
                            addParent(it.sender, it.parseArgument("region"), it.parseArgument("parent"))
                        }
                    }
                }
            }
            then("list") {
                executes {
                    printList(it.sender, 0)
                }

                then("page" to integer().apply { minimum = 0 }) {
                    executes {
                        printList(it.sender, it.parseArgument("page"))
                    }
                }
            }
        }
    }

    private fun addRegion(sender: Player, name: String) {
        val selection = sender.selection ?: throw KommandSyntaxException("먼저 구역을 선택해주세요")
        val min = selection.minimumPoint
        val max = selection.maximumPoint

        val world = BukkitAdapter.adapt(selection.world)
        val box = RegionBox(min.x, min.y, min.z, max.x, max.y, max.z)

        kotlin.runCatching {
            Regions.manager.registerNewRegion(name, Regions.manager.getRegionWorld(world)!!, box)
        }.onSuccess {
            it.save()
            sender.sendFeedback("[${it.name}] 구역을 생성했습니다.")
        }.onFailure {
            sender.sendFeedback("${ChatColor.WHITE}구역을 생성하지 못했습니다. ${it.message}")
        }
    }

    private fun removeRegion(sender: CommandSender, region: Region) {
        region.delete()
        sender.sendFeedback("[${region.name}] 구역을 제거했습니다.")
    }

    private fun relocateRegion(sender: Player, region: Region) {
        val selection = sender.selection ?: throw KommandSyntaxException("먼저 재배치할 구역을 선택해주세요")
        val min = selection.minimumPoint
        val max = selection.maximumPoint

        val world = BukkitAdapter.adapt(selection.world)
        val box = RegionBox(min.x, min.y, min.z, max.x, max.y, max.z)

        kotlin.runCatching {
            region.relocate(Regions.manager.getRegionWorld(world)!!, box)
        }.onSuccess {
            region.save()
            sender.sendFeedback("[${region.name}] 구역을 재배치했습니다.")
        }.onFailure {
            sender.sendFeedback("[${region.name}] 구역 재배치를 실패했습니다. ${it.message}")
        }
    }

    private fun addParent(sender: CommandSender, region: Region, parent: Region) {
        region.runCatching {
            region.addParent(parent)
        }.onSuccess {
            region.save()
            sender.sendFeedback("[${region.name}] 구역에 [${parent.name}] 구역을 부모로 추가했습니다.")
        }.onFailure {
            sender.sendFeedback("[${region.name}] 구역에 [${parent.name}] 구역을 부모로 추가하지 못했습니다. ${it.message}")
        }
    }

    private fun printList(sender: CommandSender, page: Int) {
        val length = if (sender is Player) 10 else 20
        val regions = Regions.manager.regions
        val lastPage = regions.count() / length

        val start = page.coerceIn(0, lastPage) * length
        val end = min(start + length, regions.count())

        for (i in start until end) {
            val region = regions[i]

            sender.sendMessage("${i + 1}. ${region.name}")
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