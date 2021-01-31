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


interface Region : Node, Area, Checkable {
    override val parent: RegionWorld

    val box: RegionBox

    val parents: List<Region>

    val children: List<Region>

    fun relocate(newWorld: RegionWorld, newBox: RegionBox)

    fun addParent(region: Region): Boolean

    fun removeParent(region: Region): Boolean

    fun getDirectAncestors(): Set<Region>

    fun getAllDescendants(): Set<Region>
}