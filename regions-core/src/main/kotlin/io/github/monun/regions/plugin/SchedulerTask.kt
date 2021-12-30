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

package io.github.monun.regions.plugin

import io.github.monun.regions.api.Permission
import io.github.monun.regions.api.regionArea
import io.github.monun.regions.internal.RegionManagerImpl
import io.github.monun.regions.internal.hasMasterKey
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerTeleportEvent

class SchedulerTask(
    private val manager: RegionManagerImpl
) : Runnable {

    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            val user = manager.getUser(player) ?: continue

            val previousLocation = user.previousLocation
            val currentLocation = player.location

            if (!player.hasMasterKey() && previousLocation != null) {
                val previousArea = previousLocation.regionArea
                val currentArea = currentLocation.regionArea

                if (previousArea !== currentArea) {
                    if (!previousArea.testPermission(player, Permission.EXIT) ||
                        !currentArea.testPermission(player, Permission.ENTRANCE)
                    ) {
                        previousLocation.apply { yaw = currentLocation.yaw; pitch = currentLocation.pitch }
                        player.teleport(previousLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                        continue
                    }
                }
            }
            user.previousLocation = currentLocation
        }
    }
}