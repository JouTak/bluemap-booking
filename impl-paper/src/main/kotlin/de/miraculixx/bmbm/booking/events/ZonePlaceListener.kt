package de.miraculixx.bmbm.booking.events

import de.miraculixx.bmbm.booking.ZoneManager
import de.miraculixx.bmbm.booking.ZoneRenderer
import de.miraculixx.bmbm.booking.model.Zone
import de.miraculixx.bmbm.booking.model.ZoneBanner
import de.miraculixx.bmbm.booking.model.ZoneType
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.bmbm.utils.config.ConfigManager
import de.miraculixx.bmbm.utils.config.Configs
import de.miraculixx.bmbm.utils.messages.plainSerializer
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.block.BlockPlaceEvent
import java.time.Instant

class ZonePlaceListener : Listener {
    private val onPlace = listen<BlockPlaceEvent> {
        if (it.isCancelled) return@listen
        val block = it.block
        if (!Tag.BANNERS.isTagged(block.type)) return@listen
        val player = it.player
        if (!player.isSneaking) return@listen
        val nameComponent = it.itemInHand.itemMeta?.name ?: return@listen
        val name = plainSerializer.serialize(nameComponent).trim()
        if (name.isEmpty()) return@listen

        val config = ConfigManager.getConfig(Configs.SETTINGS)
        val worldName = block.world.name
        if (config.getStringList("disabled-worlds").contains(worldName)) {
            player.sendMessage(msg("zone.blocked-world", listOf(worldName)))
            return@listen
        }
        if (config.getStringList("blocked-words").any { word -> word.equals(name, true) }) return@listen
        val maxLength = config.getInt("booking.max-name-length", 32)
        if (name.length > maxLength) {
            player.sendMessage(msg("zone.name-too-long", listOf(maxLength.toString())))
            return@listen
        }

        val isState = config.getStringList("booking.state-prefixes").any { prefix -> name.startsWith(prefix, true) }
        val zone: Zone = if (isState) {
            if (!player.hasPermission("booking.state.place")) {
                player.sendMessage(msg("zone.state-no-permission", listOf(name)))
                return@listen
            }
            ZoneManager.stateZone(name, worldName) ?: ZoneManager.create(name, ZoneType.STATE, null, worldName)
        } else {
            ZoneManager.playerZone(player.uniqueId, name, worldName) ?: run {
                val max = ZoneManager.maxZones(player)
                if (max != -1 && ZoneManager.zonesOf(player.uniqueId).size >= max) {
                    player.sendMessage(msg("zone.limit", listOf(max.toString())))
                    return@listen
                }
                ZoneManager.create(name, ZoneType.PLAYER, player.uniqueId, worldName)
            }
        }

        ZoneManager.addBanner(zone, ZoneBanner(block.x, block.y, block.z, block.type.bannerDye(), Instant.now(), player.uniqueId))
        ZoneRenderer.render(zone)
        if (config.getBoolean("notify-player")) {
            val key = when (zone.banners.size) {
                1 -> "zone.created"
                2 -> "zone.rectangle"
                3 -> "zone.polygon"
                else -> "zone.banner-added"
            }
            player.sendMessage(msg(key, listOf(zone.name, zone.banners.size.toString())))
        }
    }

    private fun Material.bannerDye(): DyeColor =
        DyeColor.valueOf(name.removeSuffix("_WALL_BANNER").removeSuffix("_BANNER"))

    override fun register() {
        onPlace.register()
    }

    override fun unregister() {
        onPlace.unregister()
    }
}
