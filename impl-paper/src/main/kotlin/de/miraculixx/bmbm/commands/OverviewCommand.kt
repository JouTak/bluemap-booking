package de.miraculixx.bmbm.commands

import de.miraculixx.bmbm.PluginManager
import de.miraculixx.bmbm.map.gui.storageBuilder
import de.miraculixx.bmbm.territory.ZoneManager
import de.miraculixx.bmbm.territory.model.Zone
import de.miraculixx.bmbm.utils.messages.cHighlight
import de.miraculixx.bmbm.utils.messages.cMark
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import dev.jorel.commandapi.kotlindsl.asyncPlayerProfileArgument
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OverviewCommand {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    val command = commandTree("bmbanner") {
        withPermission("bmb.overview")
        withAliases("bmb")
        literalArgument("global") {
            playerExecutor { player, _ ->
                openGUI(player, null, ZoneManager.all())
            }
        }
        asyncPlayerProfileArgument("target") {
            playerExecutor { player, args ->
                val target = args[0] as OfflinePlayer
                val zones = ZoneManager.zonesOf(target.uniqueId)
                if (zones.isEmpty()) {
                    player.sendMessage(msg("command.no-zones", listOf(target.name ?: "Unknown")))
                    return@playerExecutor
                }
                openGUI(player, target, zones)
            }
        }
    }

    private fun openGUI(player: Player, target: OfflinePlayer?, zones: List<Zone>) {
        storageBuilder {
            title = cmp("Banner Zones - ", cHighlight, bold = true) + cmp(target?.name ?: "Global", cHighlight)
            header = itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    owningPlayer = target
                    name = cmp("${target?.name ?: "Global"} Zones", cHighlight, bold = true)
                    lore(listOf(cmp(target?.uniqueId?.toString() ?: "All zones", NamedTextColor.DARK_GRAY)))
                }
            }
            filterable = true
            items = zones.map { zone ->
                itemStack(Material.valueOf("${zone.color.name}_BANNER")) {
                    meta {
                        name = cmp(zone.name, cHighlight)
                        customModel = 1
                        persistentDataContainer.set(
                            NamespacedKey(PluginManager, "zone-id"),
                            PersistentDataType.STRING,
                            zone.id.toString()
                        )
                        val owner = zone.owner?.let { Bukkit.getOfflinePlayer(it).name ?: "?" } ?: "State"
                        lore(
                            listOf(
                                cmp("World: ", cMark) + cmp(zone.world),
                                cmp("Owner: ", cMark) + cmp(owner),
                                cmp("Banners: ", cMark) + cmp(zone.banners.size.toString()),
                                cmp("Created: ", cMark) + cmp(dateFormat.format(zone.createdAt)),
                                Component.empty(),
                                cmp("Click » ") + cmp("Teleport", cMark),
                                cmp("Shift Click » ") + cmp("Delete Zone", cMark)
                            )
                        )
                    }
                }
            }
        }.open()
        player.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
    }
}
