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

import io.github.monun.regions.internal.hasMasterKey
import io.github.monun.regions.internal.toImpl
import io.github.monun.regions.api.*
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.Dispenser
import org.bukkit.block.data.Directional
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.projectiles.BlockProjectileSource
import java.util.*

class EventListener : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val toLoc = event.to

        if (player.hasMasterKey()) return

        val from = event.from.regionArea
        val to = toLoc.regionArea

        if (from !== to) {
            if (!from.testPermission(player, Permission.EXIT) ||
                !to.testPermission(player, Permission.ENTRANCE)
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVehicleMove(event: VehicleMoveEvent) {
        val from = event.from
        val to = event.to
        val fromArea = from.regionArea
        val toArea = to.regionArea

        if (fromArea !== toArea) {
            val vehicle = event.vehicle

            for (passenger in vehicle.passengers) {
                if (passenger is Player) {
                    if (passenger.hasMasterKey()) continue

                    if (!fromArea.testPermission(passenger, Permission.EXIT) ||
                        !toArea.testPermission(passenger, Permission.ENTRANCE)
                    ) {
                        passenger.eject()
                        passenger.teleport(from)
                    }
                }
            }

            if (vehicle.passengers.isEmpty()) {
                if (fromArea.hasProtection(Protection.VEHICLE_EXIT) ||
                    toArea.hasProtection(Protection.VEHICLE_ENTRANCE)
                ) {
                    vehicle.teleport(from)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val vehicle = event.vehicle
        val area = vehicle.regionArea
        val attacker = event.attacker

        if (attacker is Player) {
            if (!attacker.hasMasterKey() && !area.testPermission(attacker, Permission.ATTACK_ENTITY)) {
                event.isCancelled = true
            }
        } else {
            if (area.hasProtection(Protection.DAMAGE)) {
                event.isCancelled = true
            }
        }
    }

    private val checkTeleportCauses = EnumSet.of(
        PlayerTeleportEvent.TeleportCause.UNKNOWN,
        PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT,
        PlayerTeleportEvent.TeleportCause.ENDER_PEARL
    )

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (event.cause in checkTeleportCauses) {
            val player = event.player
            val toLoc = event.to

            if (player.hasMasterKey()) return

            val from = event.from.regionArea
            val to = toLoc.regionArea

            if (from !== to) {
                if (!from.testPermission(player, Permission.EXIT) ||
                    !to.testPermission(player, Permission.ENTRANCE)
                ) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleportMonitor(event: PlayerTeleportEvent) {
        if (event.cause !in checkTeleportCauses) {
            event.player.regionUser.toImpl().previousLocation = event.to
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (player.hasMasterKey()) return

        event.clickedBlock?.let { clickedBlock ->
            val area = clickedBlock.regionArea

            if (!area.testPermission(player, Permission.INTERACTION))
                event.isCancelled = true

            return
        }

        if (!player.regionArea.testPermission(player, Permission.INTERACTION)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        if (player.hasMasterKey()) return

        val target = event.rightClicked

        if (!target.regionArea.testPermission(player, Permission.INTERACTION)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = event.player.also { if (it.hasMasterKey()) return }
        val target = event.rightClicked

        if (!target.regionArea.testPermission(player, Permission.ARMOR_STAND_MANIPULATION))
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        val player = event.player.also { if (it.hasMasterKey()) return }
        val bed = event.bed
        val playerArea = player.regionArea
        val bedArea = bed.regionArea

        if (playerArea !== bedArea) {
            if (bedArea.testPermission(player, Permission.EXIT) ||
                !playerArea.testPermission(player, Permission.ENTRANCE)
            ) {
                player.teleport(bed.location.add(0.5, 0.56250, 0.5))
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        var damager = event.damager

        if (damager is Projectile) {
            val shooter = damager.shooter

            if (shooter is Entity)
                damager = shooter
        }

        val area = entity.regionArea

        if (damager is Player) {
            if (damager.hasMasterKey()) return

            val permission = if (entity is Player) Permission.ATTACK_PLAYER else Permission.ATTACK_ENTITY

            if (!area.testPermission(damager, permission)) {
                event.isCancelled = true
                return
            }
        }

        if (area.hasProtection(Protection.DAMAGE)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        val shooter = projectile.shooter

        if (shooter is Player) {
            if (shooter.hasMasterKey()) return

            if (!projectile.regionArea.testPermission(shooter, Permission.PROJECTILE_LAUNCH)) {
                event.isCancelled = true
            }
        } else if (shooter is BlockProjectileSource) {
            val block = shooter.block
            val area = block.regionArea

            if (area.hasProtection(Protection.DISPENSER)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockCanBuild(event: BlockCanBuildEvent) {
        val player = event.player
        if (player == null || player.hasMasterKey()) return

        if (!event.block.regionArea.testPermission(player, Permission.BLOCK_PLACE)) {
            event.isBuildable = false
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.hasMasterKey()) return

        if (!event.block.regionArea.testPermission(player, Permission.BLOCK_BREAK)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.hasMasterKey()) return

        if (!event.block.regionArea.testPermission(player, Permission.BLOCK_PLACE)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val area = event.block.regionArea
        val player = event.player

        if (player == null) {
            if (area.hasProtection(Protection.FIRE)) {
                event.isCancelled = true
            }
        } else {
            if (player.hasMasterKey()) return

            if (!area.testPermission(player, Permission.BLOCK_IGNITING)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        if (player.hasMasterKey()) return

        if (!event.blockClicked.regionArea.testPermission(player, Permission.BUCKET_FILL)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        if (player.hasMasterKey()) return

        if (!event.blockClicked.getRelative(event.blockFace).regionArea.testPermission(
                player,
                Permission.BUCKET_EMPTY
            )
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        var remover = event.remover ?: return

        if (remover is Projectile) {
            val shooter = remover.shooter

            if (shooter is Entity)
                remover = shooter
        }

        if (remover is Player) {
            if (remover.hasMasterKey()) return

            if (!event.entity.regionArea.testPermission(remover, Permission.HANGING_BREAK)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        val player = event.player

        if (player == null || player.hasMasterKey()) return

        if (!event.block.regionArea.testPermission(player, Permission.HANGING_PLACE)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        if (player.hasMasterKey()) return

        if (!event.itemDrop.regionArea.testPermission(player, Permission.DROP_ITEM)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity

        if (entity is Player) {
            if (entity.hasMasterKey()) return

            val item = event.item

            if (!item.regionArea.testPermission(entity, Permission.PICKUP_ITEM)) {
                event.isCancelled = true
                item.pickupDelay = 10
            }
        } else {
            val item = event.item

            if (item.regionArea.hasProtection(Protection.ENTITY_PICKUP_ITEM)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPotionSplash(event: PotionSplashEvent) {
        for (affectedEntity in event.affectedEntities) {
            val area = affectedEntity.regionArea

            if (area.hasProtection(Protection.POTION)) {
                event.setIntensity(affectedEntity, 0.0)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onLingeringPotionSplash(event: LingeringPotionSplashEvent) {
        if (event.areaEffectCloud.regionArea.hasProtection(Protection.POTION)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) {
        event.affectedEntities.removeIf { entity ->
            entity.regionArea.hasProtection(Protection.POTION)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        if (event.block.regionArea.hasProtection(Protection.FADE)) {
            event.isCancelled = true
        }
    }

    private val stemsByFruit = EnumMap<Material, Material>(Material::class.java).apply {
        this[Material.PUMPKIN] = Material.PUMPKIN_STEM
        this[Material.MELON] = Material.MELON_STEM
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockGrow(event: BlockGrowEvent) {
        val block = event.block
        val area = block.regionArea

        if (area.hasProtection(Protection.GROWTH)) {
            event.isCancelled = true
        } else {
            val type = event.newState.type
            stemsByFruit[type]?.let { stemType ->
                val directions = arrayOf(BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH)

                for (direction in directions) {
                    val relativeBlock = block.getRelative(direction)

                    if (relativeBlock.type == stemType) {
                        val relativeArea = relativeBlock.regionArea

                        if (area !== relativeArea && relativeArea.hasProtection(Protection.OVERFLOW)) {
                            event.isCancelled = true
                            break
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSpongeAbsorb(event: SpongeAbsorbEvent) {
        val block = event.block
        val area = block.regionArea

        event.blocks.removeIf { state ->
            val stateArea = state.block.regionArea
            area !== stateArea && stateArea.hasProtection(Protection.SPONGE)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockForm(event: BlockFormEvent) {
        if (event.block.regionArea.hasProtection(Protection.FORM)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        val source = event.source.regionArea
        val block = event.block.regionArea

        if (source !== block) {
            if (source.hasProtection(Protection.SPREAD)
                || block.hasProtection(Protection.SPREAD)
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        if (event.block.regionArea.hasProtection(Protection.REDSTONE)) {
            event.newCurrent = 0
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val area = event.location.regionArea

        if (area.hasProtection(Protection.OVERFLOW)) {
            val blocks = event.blocks

            if (area is Region) {
                val box = area.box
                blocks.removeIf { !box.contains(it.x, it.y, it.z) }
            } else if (area is World) {
                blocks.removeIf { it.block.regionArea === area }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        val direction = event.direction
        val piston = event.block
        val head = piston.getRelative(direction)

        val pistonArea = piston.regionArea
        val headArea = head.regionArea
        val pistonProtection = pistonArea.hasProtection(Protection.PISTON)

        if (pistonArea !== headArea
            && (pistonProtection || headArea.hasProtection(Protection.PISTON))
        ) {
            event.isCancelled = true
            return
        }

        if (pistonProtection) {
            for (block in event.blocks) {
                if (block.regionArea !== pistonArea || block.getRelative(direction).regionArea !== pistonArea) {
                    event.isCancelled = true
                    break
                }
            }
        } else {
            for (block in event.blocks) {
                val blockArea = block.regionArea

                if (blockArea !== pistonArea && blockArea.hasProtection(Protection.PISTON)) {
                    event.isCancelled = true
                    break
                }

                val toBlockArea = block.getRelative(direction).regionArea

                if (toBlockArea !== pistonArea && toBlockArea.hasProtection(Protection.PISTON)) {
                    event.isCancelled = true
                    break
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val blocks = event.blocks

        if (blocks.isEmpty())
            return

        val direction = event.direction
        val piston = event.block
        val pistonArea = piston.regionArea
        val pistonProtection = pistonArea.hasProtection(Protection.PISTON)

        if (pistonProtection) {
            for (block in event.blocks) {
                if (block.regionArea !== pistonArea || block.getRelative(direction).regionArea !== pistonArea) {
                    event.isCancelled = true
                    break
                }
            }
        } else {
            for (block in event.blocks) {
                val blockArea = block.regionArea

                if (blockArea !== pistonArea && blockArea.hasProtection(Protection.PISTON)) {
                    event.isCancelled = true
                    break
                }

                val toBlockArea = block.getRelative(direction).regionArea

                if (toBlockArea !== pistonArea && toBlockArea.hasProtection(Protection.PISTON)) {
                    event.isCancelled = true
                    break
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        val from = event.block.regionArea
        val to = event.toBlock.regionArea

        if (from !== to) {
            if (from.hasProtection(Protection.FLOOD)
                || to.hasProtection(Protection.FLOOD)
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityBlockChange(event: EntityChangeBlockEvent) {
        if (event.block.regionArea.hasProtection(Protection.ENTITY_CHANGE_BLOCK)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entity.regionArea.hasProtection(Protection.EXPLOSION)) {
            event.isCancelled = true
        } else {
            event.blockList().removeIf { block ->
                block.regionArea.hasProtection(Protection.EXPLOSION)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { it.regionArea.hasProtection(Protection.EXPLOSION) }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockDispense(event: BlockDispenseEvent) {
        val block = event.block
        val state = block.state

        if (state is Dispenser) {
            val data = block.blockData
            if (data is Directional) {
                val from = block.regionArea
                val to = block.getRelative(data.facing).regionArea

                if (from !== to &&
                    (from.hasProtection(Protection.DISPENSER) || to.hasProtection(Protection.DISPENSER))
                ) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        val sourceHolder = event.source.holder
        val destinationHolder = event.destination.holder

        if (sourceHolder is Container && destinationHolder is Container) {
            val sourceArea = sourceHolder.block.regionArea
            val destinationArea = destinationHolder.block.regionArea

            if (sourceArea !== destinationArea &&
                (sourceArea.hasProtection(Protection.ITEM_TRANSFER) || destinationArea.hasProtection(Protection.ITEM_TRANSFER))
            ) {
                event.isCancelled = true
            }
        }
    }
}