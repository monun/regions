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

package com.github.monun.regions.plugin

import com.github.monun.kommand.kommand
import com.github.monun.regions.api.Regions
import com.github.monun.regions.command.AreaCommands
import com.github.monun.regions.command.RegionCommands
import com.github.monun.regions.internal.RegionManagerImpl
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Nemo
 */
class RegionPlugin : JavaPlugin() {
    override fun onEnable() {
        Regions.initialize(this)

        server.apply {
            pluginManager.registerEvents(EventListener(), this@RegionPlugin)
            scheduler.runTaskTimer(this@RegionPlugin, SchedulerTask(Regions.manager as RegionManagerImpl), 0L, 1L)
        }

        setupCommands()
    }

    private fun setupCommands() {
        kommand {
            AreaCommands.register(this)
            RegionCommands.register(this)
        }
    }

    override fun onDisable() {
        Regions.manager.saveAll()
    }
}