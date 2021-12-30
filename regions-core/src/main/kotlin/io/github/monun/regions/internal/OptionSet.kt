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

package io.github.monun.regions.internal

import io.github.monun.regions.api.Permission
import io.github.monun.regions.api.Protection
import io.github.monun.regions.util.IntBitSet

class ProtectionSet(rawElements: Int = 0) : IntBitSet<Protection>(rawElements, { Protection.getByOffset(it) })

class PermissionSet(rawElements: Int = 0) : IntBitSet<Permission>(rawElements, { Permission.getByOffset(it) })