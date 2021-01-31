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

import com.github.monun.regions.util.Indexable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSortedMap
import java.util.*


enum class Protection(val key: String) : Indexable {
    DAMAGE("damage"),
    POTION("potion"),
    FIRE("fire"),
    FADE("fade"),
    GROWTH("growth"),
    FORM("form"),
    SPREAD("blockSpread"),
    REDSTONE("redstone"),
    PISTON("piston"),
    DISPENSER("dispenser"),
    OVERFLOW("overflow"),
    FLOOD("flood"),
    ENTITY_PICKUP_ITEM("entityPickupItem"),
    ENTITY_CHANGE_BLOCK("entityChangeBlock"),
    EXPLOSION("explosion"),
    ITEM_TRANSFER("itemTransfer"),
    VEHICLE_ENTRANCE("vehicleEntrance"),
    VEHICLE_EXIT("vehicleExit"),
    SPONGE("sponge");

    override val offset: Int
        get() = ordinal

    override val raw: Int = super.raw

    companion object {
        private val byOffset = ImmutableList.copyOf(values())
        private val byKey: Map<String, Protection>

        init {
            val byKey = TreeMap<String, Protection>(String.CASE_INSENSITIVE_ORDER)

            for (value in values()) {
                byKey[value.key] = value
            }
            this.byKey = ImmutableSortedMap.copyOf(byKey)
        }

        fun getByOffset(offset: Int): Protection? {
            return byOffset.getOrNull(offset)
        }

        @JvmStatic
        fun getByKey(key: String): Protection? {
            return byKey[key]
        }
    }

    override fun toString(): String {
        return key
    }
}