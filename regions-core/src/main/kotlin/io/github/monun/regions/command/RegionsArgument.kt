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

import io.github.monun.kommand.KommandArgument
import io.github.monun.regions.api.Area
import io.github.monun.regions.api.RegionManager
import io.github.monun.regions.api.Role
import org.bukkit.Bukkit

object RegionsArgument {
    lateinit var manager: RegionManager
        internal set

    fun area() = KommandArgument.dynamic { _, input ->
        val colon = input.indexOf(':')
        if (colon < 0) return@dynamic null

        val type = input.substring(0, colon)
        val name = input.substring(colon + 1)

        when (type) {
            "world" -> manager.getRegionWorld(name)
            "region" -> manager.getRegion(name)
            else -> null
        }
    }.apply {
        suggests {
            manager.worlds.map { "world:${it.name}" } + manager.regions.map { "region:${it.name}" }
        }
    }

    fun role(areaArgumentName: String, withPublicRole: Boolean) = KommandArgument.dynamic { context, input ->
        context.runCatching {
            context.get<Area>(areaArgumentName)
        }.getOrNull()?.let { area ->
            area.getRole(input)?.takeIf { role ->
                if (withPublicRole) true
                else !role.isPublic
            }
        }
    }.apply {
        suggests { context ->
            context.runCatching {
                context.get<Area>(areaArgumentName)
            }.getOrNull()?.let { area ->
                arrayListOf<Role>().apply {
                    addAll(area.roles)
                    if (withPublicRole) add(area.publicRole)
                }
            } ?: emptyList<Role>()
        }
    }

    fun user() = KommandArgument.dynamic { context, input ->
        val profile = Bukkit.createProfile(input)
        if (profile.complete()) manager.getUser(profile) else null
    }.apply {
        suggests {
            suggest(Bukkit.getOnlinePlayers().map { it.name })
        }
    }

    fun member(areaArgumentName: String) = KommandArgument.dynamic { context, input ->
        context.runCatching {
            get<Area>(areaArgumentName)
        }.getOrNull()?.getMember(input)
    }.apply {
        suggests { context ->
            context.runCatching {
                get<Area>(areaArgumentName)
            }.getOrNull()?.let { area ->
                area.members.map { member -> member.user.name }
            }
        }
    }
}