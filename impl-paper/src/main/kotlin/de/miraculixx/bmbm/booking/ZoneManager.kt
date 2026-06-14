package de.miraculixx.bmbm.booking

import de.miraculixx.bmbm.PluginManager
import de.miraculixx.bmbm.booking.model.BannerPos
import de.miraculixx.bmbm.booking.model.Zone
import de.miraculixx.bmbm.booking.model.ZoneBanner
import de.miraculixx.bmbm.booking.model.ZoneStorage
import de.miraculixx.bmbm.booking.model.ZoneType
import de.miraculixx.bmbm.utils.config.ConfigManager
import de.miraculixx.bmbm.utils.config.Configs
import de.miraculixx.bmbm.utils.messages.cError
import de.miraculixx.bmbm.utils.messages.prefix
import de.miraculixx.bmbm.utils.serializer.json
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.console
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bukkit.entity.Player
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID

object ZoneManager {
    private val zones = mutableListOf<Zone>()
    private val bannerIndex = mutableMapOf<BannerPos, Zone>()

    fun all(): List<Zone> = zones.toList()

    fun zoneAt(pos: BannerPos): Zone? = bannerIndex[pos]

    fun zonesOf(owner: UUID): List<Zone> = zones.filter { it.owner == owner }

    fun pointsOf(owner: UUID): Int = zones.count { it.type == ZoneType.PLAYER && it.owner == owner && it.banners.size == 1 }

    fun polygonsOf(owner: UUID): Int = zones.count { it.type == ZoneType.PLAYER && it.owner == owner && it.banners.size >= 2 }

    fun playerZone(owner: UUID, name: String, world: String): Zone? =
        zones.firstOrNull { it.type == ZoneType.PLAYER && it.owner == owner && it.world == world && it.name.equals(name, true) }

    fun stateZone(name: String, world: String): Zone? =
        zones.firstOrNull { it.type == ZoneType.STATE && it.world == world && it.name.equals(name, true) }

    fun zoneById(id: UUID): Zone? = zones.firstOrNull { it.id == id }

    fun create(name: String, type: ZoneType, owner: UUID?, world: String): Zone {
        val zone = Zone(UUID.randomUUID(), name, type, owner, world, Instant.now())
        zones.add(zone)
        return zone
    }

    fun addBanner(zone: Zone, banner: ZoneBanner) {
        zone.banners.add(banner)
        bannerIndex[BannerPos(zone.world, banner.x, banner.y, banner.z)] = zone
        save()
    }

    /** @return true if the zone was deleted because its last banner was removed */
    fun removeBanner(zone: Zone, pos: BannerPos): Boolean {
        val banner = zone.bannerAt(pos) ?: return false
        zone.banners.remove(banner)
        bannerIndex.remove(pos)
        if (zone.banners.isEmpty()) {
            zones.remove(zone)
            save()
            return true
        }
        save()
        return false
    }

    fun delete(zone: Zone) {
        zone.banners.forEach { bannerIndex.remove(BannerPos(zone.world, it.x, it.y, it.z)) }
        zones.remove(zone)
        save()
    }

    fun maxPolygonZones(player: Player): Int = limit(player, "booking.zone-limit", default = 1)

    fun maxPointZones(player: Player): Int = limit(player, "booking.point-limit", default = -1)

    private fun limit(player: Player, configKey: String, default: Int): Int {
        val section = ConfigManager.getConfig(Configs.SETTINGS).getConfigurationSection(configKey) ?: return default
        var max = section.getInt("default", default)
        section.getKeys(false).forEach { rank ->
            if (rank == "default") return@forEach
            val amount = section.getInt(rank)
            if (player.hasPermission("$configKey.$rank")) {
                if (amount == -1) return -1
                if (amount > max) max = amount
            }
        }
        return max
    }

    fun isBreakableByEveryone(zone: Zone): Boolean {
        if (zone.type == ZoneType.STATE) return false
        val days = ConfigManager.getConfig(Configs.SETTINGS).getInt("booking.protect-days", 30)
        if (days < 0) return false
        return Instant.now().isAfter(zone.createdAt.plus(Duration.ofDays(days.toLong())))
    }

    fun protectionDaysLeft(zone: Zone): Long {
        val days = ConfigManager.getConfig(Configs.SETTINGS).getInt("booking.protect-days", 30)
        if (days < 0) return Long.MAX_VALUE
        val remaining = Duration.between(Instant.now(), zone.createdAt.plus(Duration.ofDays(days.toLong())))
        return if (remaining.isNegative) 0 else remaining.plusDays(1).minusNanos(1).toDays()
    }

    fun load() {
        zones.clear()
        bannerIndex.clear()
        val legacyFolder = File(PluginManager.dataFolder, "marker")
        if (legacyFolder.exists() && legacyFolder.listFiles()?.isNotEmpty() == true) {
            console.sendMessage(prefix + cmp("Legacy point marker data found in '${legacyFolder.path}' - it is no longer loaded since booking zones replaced point markers (see README)"))
        }
        val file = storageFile()
        if (!file.exists()) return
        val storage = try {
            json.decodeFromString<ZoneStorage>(file.readText().ifBlank { "{}" })
        } catch (e: Exception) {
            val backup = File(file.parentFile, "zones.json.broken-${System.currentTimeMillis()}")
            file.copyTo(backup, overwrite = true)
            console.sendMessage(prefix + cmp("zones.json is invalid and was backed up to ${backup.name}! Starting with no zones", cError))
            return
        }
        zones.addAll(storage.zones)
        zones.forEach { zone ->
            zone.banners.forEach { bannerIndex[BannerPos(zone.world, it.x, it.y, it.z)] = zone }
        }
    }

    fun save() {
        val file = storageFile()
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(ZoneStorage(zones = zones)))
    }

    private fun storageFile() = File(PluginManager.dataFolder, "zones.json")
}
