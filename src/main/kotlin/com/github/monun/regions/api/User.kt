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

package com.github.monun.regions.api

import org.bukkit.entity.Player
import java.util.*

interface User {
    val uniqueId: UUID

    val name: String

    val bukkitPlayer: Player?

    val regionMembers: List<Member>

    fun getMemberByRegion(region: Region): Member?

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null
        }

    fun sendMessage(message: String) {
        bukkitPlayer?.run { sendMessage(message) }
    }
}