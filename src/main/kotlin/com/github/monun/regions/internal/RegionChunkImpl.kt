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

import com.github.monun.regions.api.RegionChunk
import com.github.monun.regions.api.RegionWorld
import com.google.common.collect.ImmutableList

class RegionChunkImpl(
    override val world: RegionWorld,
    override val x: Int,
    override val z: Int
) : RegionChunk {
    override val regions: List<RegionImpl>
        get() = ImmutableList.copyOf(_regions)

    internal val _regions = ArrayList<RegionImpl>(1)

    override fun regionAt(x: Int, y: Int, z: Int): RegionImpl? {
        return _regions.find { it.box.contains(x, y, z) }
    }

    internal fun addRegion(region: RegionImpl) {
        _regions += region
    }

    internal fun removeRegion(region: RegionImpl) {
        _regions -= region
    }
}