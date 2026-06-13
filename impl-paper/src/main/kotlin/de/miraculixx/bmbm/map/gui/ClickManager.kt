package de.miraculixx.bmbm.map.gui

import de.miraculixx.bmbm.PluginManager
import de.miraculixx.bmbm.territory.ZoneManager
import de.miraculixx.bmbm.territory.ZoneRenderer
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.bmbm.utils.messages.plainSerializer
import de.miraculixx.kpaper.event.SingleListener
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.localization.msg
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*

class ClickManager : Listener {
    private val listener: SingleListener<InventoryClickEvent> = listen {
        val player = it.whoClicked as? Player ?: return@listen
        val title = plainSerializer.serialize(it.view.title())
        if (!title.startsWith("Banner Zones - ")) return@listen
        it.isCancelled = true
        val item = it.currentItem ?: return@listen
        if (item.itemMeta?.customModel != 1) return@listen
        val id = item.itemMeta.persistentDataContainer.get(NamespacedKey(PluginManager, "zone-id"), PersistentDataType.STRING) ?: return@listen
        val zone = ZoneManager.zoneById(UUID.fromString(id)) ?: return@listen

        if (it.click.isShiftClick) {
            ZoneManager.delete(zone)
            ZoneRenderer.remove(zone)
            player.sendMessage(msg("zone.deleted", listOf(zone.name, "0")))
            player.playSound(player, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 1.2f)
            player.closeInventory()
        } else {
            val banner = zone.banners.firstOrNull() ?: return@listen
            val world = Bukkit.getWorld(zone.world) ?: return@listen
            val location = Location(world, banner.x + 0.5, banner.y + 1.0, banner.z + 0.5)
            player.teleportAsync(location)
            player.sendMessage(msg("event.teleport", listOf("${banner.x} ${banner.y} ${banner.z}")))
        }
    }

    //Constantly keep this listener running to prevent item glitching
    override fun register() {
        listener.register()
    }
    override fun unregister() {
        listener.unregister()
    }
}
