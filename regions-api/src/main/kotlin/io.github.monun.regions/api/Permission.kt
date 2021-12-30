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

package io.github.monun.regions.api

import io.github.monun.regions.util.Indexable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSortedMap
import java.util.*

interface Permissible {
    val permissions: Set<Permission>

    fun hasPermission(permission: Permission): Boolean

    fun hasPermissions(vararg permissions: Permission): Boolean {
        return hasPermissions(permissions.asList())
    }

    fun hasPermissions(permissions: Collection<Permission>): Boolean
}

enum class Permission(val key: String) : Indexable {
    OWNERSHIP("ownership"),
    ADMINISTRATION("administration"),
    ENTRANCE("entrance"),
    EXIT("exit"),
    INTERACTION("interaction"),
    ATTACK_ENTITY("attackEntity"),
    ATTACK_PLAYER("attackPlayer"),
    PROJECTILE_LAUNCH("projectileLaunch"),
    BLOCK_BREAK("blockBreak"),
    BLOCK_PLACE("blockPlace"),
    BLOCK_IGNITING("blockIgniting"),
    BUCKET_FILL("bucketFill"),
    BUCKET_EMPTY("bucketEmpty"),
    HANGING_BREAK("hangingBreak"),
    HANGING_PLACE("hangingPlace"),
    DROP_ITEM("dropItem"),
    PICKUP_ITEM("pickupItem"),
    ARMOR_STAND_MANIPULATION("armorStandManipulation");

    override val offset: Int
        get() = ordinal

    override val raw: Int = super.raw

    override fun toString(): String {
        return key
    }

    companion object {
        private val byOffset = ImmutableList.copyOf(values())
        private val byKey: Map<String, Permission>

        init {
            val byKey = TreeMap<String, Permission>(String.CASE_INSENSITIVE_ORDER)

            for (value in values()) {
                byKey[value.key] = value
            }

            Companion.byKey = ImmutableSortedMap.copyOf(byKey)
        }

        fun getByOffset(offset: Int): Permission? {
            return byOffset.getOrNull(offset)
        }

        fun getByKey(key: String): Permission? {
            return byKey[key]
        }
    }
}

val Array<Permission>.rawPermissions: Int
    get() {
        var raw = 0

        forEach {
            raw = (raw or it.raw)
        }

        return raw
    }

val Iterable<Permission>.rawPermissions: Int
    get() {
        var raw = 0

        forEach {
            raw = (raw or it.raw)
        }

        return raw
    }