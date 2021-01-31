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

package com.github.monun.regions.internal

import com.github.monun.regions.api.Member
import com.github.monun.regions.api.Region
import com.github.monun.regions.api.User
import com.google.common.collect.ImmutableList
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

class UserImpl(
    override val uniqueId: UUID,
    override var name: String
) : User {

    override var bukkitPlayer: Player? = null
        internal set

    override val regionMembers: List<Member>
        get() = ImmutableList.copyOf(_regionMembers)

    private val _regionMembers = HashSet<Member>()

    internal var previousLocation: Location? = null

    override fun getMemberByRegion(region: Region): Member? {
        return _regionMembers.find { it.parent === region }
    }

    internal fun addMember(member: Member) {
        _regionMembers.add(member)
    }
}

internal fun User.toImpl(): UserImpl {
    return this as UserImpl
}

internal fun Player.hasMasterKey(): Boolean {
    return hasPermission("regions.action")
}