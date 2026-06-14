package de.miraculixx.bmbm.booking.events

import de.miraculixx.bmbm.booking.ZoneManager
import de.miraculixx.bmbm.booking.model.BannerPos
import de.miraculixx.bmbm.booking.model.Zone
import de.miraculixx.bmbm.booking.model.ZoneType
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import org.bukkit.Bukkit
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class ZoneInfoListener : Listener {
    private val cooldown = mutableMapOf<UUID, Long>()
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    private val onInteract = listen<PlayerInteractEvent> {
        if (it.action != Action.RIGHT_CLICK_BLOCK || it.hand != EquipmentSlot.HAND) return@listen
        val block = it.clickedBlock ?: return@listen
        if (!Tag.BANNERS.isTagged(block.type)) return@listen
        val zone = ZoneManager.zoneAt(BannerPos(block.world.name, block.x, block.y, block.z)) ?: return@listen
        val player = it.player
        val now = System.currentTimeMillis()
        if (now - (cooldown[player.uniqueId] ?: 0L) < 1000L) return@listen
        cooldown[player.uniqueId] = now
        sendInfo(player, zone, block)
    }

    private fun sendInfo(player: Player, zone: Zone, block: Block) {
        player.sendMessage(msg("zone.info.header", listOf(zone.name)))
        player.sendMessage(msg(if (zone.type == ZoneType.STATE) "zone.info.type-state" else "zone.info.type-player"))
        zone.owner?.let { owner ->
            val name = Bukkit.getOfflinePlayer(owner).name ?: owner.toString().take(8)
            player.sendMessage(msg("zone.info.owner", listOf(name)))
        }
        player.sendMessage(msg("zone.info.founded", listOf(dateFormat.format(zone.createdAt))))
        val protectionKey = when {
            zone.type == ZoneType.STATE || ZoneManager.protectionDaysLeft(zone) == Long.MAX_VALUE -> "zone.info.protection-permanent"
            ZoneManager.isBreakableByEveryone(zone) -> "zone.info.protection-expired"
            else -> null
        }
        if (protectionKey != null) player.sendMessage(msg(protectionKey))
        else player.sendMessage(msg("zone.info.protection-days", listOf(ZoneManager.protectionDaysLeft(zone).toString())))
        player.sendMessage(msg("zone.info.coords", listOf("${block.x} ${block.y} ${block.z}")))
    }

    override fun register() {
        onInteract.register()
    }

    override fun unregister() {
        onInteract.unregister()
    }
}
