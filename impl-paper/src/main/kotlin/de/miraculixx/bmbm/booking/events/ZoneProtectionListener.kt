package de.miraculixx.bmbm.booking.events

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import de.miraculixx.bmbm.booking.ZoneManager
import de.miraculixx.bmbm.booking.ZoneRenderer
import de.miraculixx.bmbm.booking.model.BannerPos
import de.miraculixx.bmbm.booking.model.Zone
import de.miraculixx.bmbm.booking.model.ZoneType
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import io.papermc.paper.event.block.BlockBreakBlockEvent
import org.bukkit.Bukkit
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent

class ZoneProtectionListener : Listener {

    private val onBreak = listen<BlockBreakEvent> {
        val zone = zoneAt(it.block) ?: return@listen
        val player = it.player
        val allowed = when {
            player.hasPermission("booking.admin") -> true
            zone.type == ZoneType.STATE -> player.hasPermission("booking.state.place")
            zone.owner == player.uniqueId -> true
            else -> ZoneManager.isBreakableByEveryone(zone)
        }
        if (!allowed) {
            it.isCancelled = true
            val daysLeft = ZoneManager.protectionDaysLeft(zone)
            val days = if (daysLeft == Long.MAX_VALUE) "∞" else daysLeft.toString()
            player.sendMessage(msg("zone.protected", listOf(zone.name, days)))
            return@listen
        }
        removeBanner(zone, it.block, player)
    }

    private val onDestroy = listen<BlockDestroyEvent> {
        val zone = zoneAt(it.block) ?: return@listen
        if (isProtected(zone)) it.isCancelled = true
        else removeBanner(zone, it.block, null)
    }

    private val onBreakByBlock = listen<BlockBreakBlockEvent> {
        val zone = zoneAt(it.block) ?: return@listen
        removeBanner(zone, it.block, null)
    }

    private val onLiquidFlow = listen<BlockFromToEvent> {
        val zone = zoneAt(it.toBlock) ?: return@listen
        if (isProtected(zone)) it.isCancelled = true
    }

    private val onEntityExplode = listen<EntityExplodeEvent> { handleExplosion(it.blockList()) }

    private val onBlockExplode = listen<BlockExplodeEvent> { handleExplosion(it.blockList()) }

    private val onPistonExtend = listen<BlockPistonExtendEvent> {
        if (pistonTouchesProtected(it.blocks, it.direction, it.block.getRelative(it.direction))) it.isCancelled = true
    }

    private val onPistonRetract = listen<BlockPistonRetractEvent> {
        if (pistonTouchesProtected(it.blocks, it.direction, null)) it.isCancelled = true
    }

    private val onEntityChangeBlock = listen<EntityChangeBlockEvent> {
        val zone = zoneAt(it.block) ?: return@listen
        if (isProtected(zone)) it.isCancelled = true
        else removeBanner(zone, it.block, null)
    }

    private fun handleExplosion(blocks: MutableList<Block>) {
        blocks.removeIf { block -> zoneAt(block)?.let { isProtected(it) } == true }
        blocks.filter { zoneAt(it) != null }.forEach { block ->
            removeBanner(zoneAt(block) ?: return@forEach, block, null)
        }
    }

    private fun pistonTouchesProtected(blocks: List<Block>, direction: BlockFace, head: Block?): Boolean {
        val affected = buildSet {
            blocks.forEach {
                add(it)
                add(it.getRelative(direction))
            }
            head?.let { add(it) }
        }
        return affected.any { block -> zoneAt(block)?.let { isProtected(it) } == true }
    }

    private fun removeBanner(zone: Zone, block: Block, breaker: Player?) {
        val pos = BannerPos(block.world.name, block.x, block.y, block.z)
        if (zone.bannerAt(pos) == null) return
        val deleted = ZoneManager.removeBanner(zone, pos)
        if (deleted) ZoneRenderer.remove(zone) else ZoneRenderer.render(zone)

        val isOwner = breaker != null && breaker.uniqueId == zone.owner
        breaker?.sendMessage(msg(if (deleted) "zone.deleted" else "zone.banner-removed", listOf(zone.name, zone.banners.size.toString())))
        if (!isOwner) {
            zone.owner?.let { Bukkit.getPlayer(it) }?.sendMessage(msg(if (deleted) "zone.lost" else "zone.banner-lost", listOf(zone.name, zone.banners.size.toString())))
        }
    }

    private fun zoneAt(block: Block): Zone? {
        if (!Tag.BANNERS.isTagged(block.type)) return null
        return ZoneManager.zoneAt(BannerPos(block.world.name, block.x, block.y, block.z))
    }

    private fun isProtected(zone: Zone): Boolean = !ZoneManager.isBreakableByEveryone(zone)

    override fun register() {
        onBreak.register()
        onDestroy.register()
        onBreakByBlock.register()
        onLiquidFlow.register()
        onEntityExplode.register()
        onBlockExplode.register()
        onPistonExtend.register()
        onPistonRetract.register()
        onEntityChangeBlock.register()
    }

    override fun unregister() {
        onBreak.unregister()
        onDestroy.unregister()
        onBreakByBlock.unregister()
        onLiquidFlow.unregister()
        onEntityExplode.unregister()
        onBlockExplode.unregister()
        onPistonExtend.unregister()
        onPistonRetract.unregister()
        onEntityChangeBlock.unregister()
    }
}
