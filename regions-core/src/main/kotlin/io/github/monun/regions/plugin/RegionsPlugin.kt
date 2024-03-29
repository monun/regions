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

import io.github.monun.kommand.kommand
import io.github.monun.regions.api.RegionsInternal
import io.github.monun.regions.command.AreaCommands
import io.github.monun.regions.internal.RegionManagerImpl
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Nemo
 */
class RegionsPlugin : JavaPlugin() {
    lateinit var regionManager: RegionManagerImpl
        private set

    override fun onEnable() {
        regionManager = RegionManagerImpl(this).also {
            RegionsInternal.register(it)
        }

        setupAdapters()
        setupCommands()
    }

    private fun setupAdapters() {
        server.apply {
            pluginManager.registerEvents(EventListener(), this@RegionsPlugin)
            scheduler.runTaskTimer(this@RegionsPlugin, SchedulerTask(regionManager), 0L, 1L)
        }
    }

    private fun setupCommands() {
        kommand { AreaCommands.register(this, regionManager) }
    }

    override fun onDisable() {
        regionManager.saveAll()
        RegionsInternal.unregister()
    }
}