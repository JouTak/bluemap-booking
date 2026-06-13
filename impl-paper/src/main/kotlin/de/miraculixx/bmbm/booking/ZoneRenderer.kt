package de.miraculixx.bmbm.booking

import com.flowpowered.math.vector.Vector2d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import de.bluecolored.bluemap.api.markers.ShapeMarker
import de.bluecolored.bluemap.api.math.Color
import de.bluecolored.bluemap.api.math.Shape
import de.miraculixx.bmbm.booking.geometry.Point2
import de.miraculixx.bmbm.booking.geometry.ZonePolygon
import de.miraculixx.bmbm.booking.model.Zone
import de.miraculixx.bmbm.booking.model.ZoneType
import de.miraculixx.bmbm.utils.cache.bannerImages
import de.miraculixx.bmbm.utils.config.ConfigManager
import de.miraculixx.bmbm.utils.config.Configs
import de.miraculixx.bmbm.utils.messages.cError
import de.miraculixx.bmbm.utils.messages.plainSerializer
import de.miraculixx.bmbm.utils.messages.prefix
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.localization.msg
import org.bukkit.Bukkit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ZoneRenderer {
    private val markerSets = mutableMapOf<String, MarkerSet>()
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    fun connect(api: BlueMapAPI) {
        val config = ConfigManager.getConfig(Configs.SETTINGS)
        worlds.forEach { world ->
            val set = MarkerSet.builder()
                .label(config.getString("booking.marker-set.label") ?: "Territories")
                .toggleable(config.getBoolean("booking.marker-set.toggleable", true))
                .defaultHidden(!config.getBoolean("booking.marker-set.visible", true))
                .build()
            markerSets[world.name] = set
            api.getWorld(world.uid).ifPresent { bmWorld ->
                bmWorld.maps.forEach { map -> map.markerSets["BOOKING_${world.name}"] = set }
            }
        }
        ZoneManager.all().forEach { render(it) }
    }

    fun disconnect() {
        markerSets.clear()
    }

    fun render(zone: Zone) {
        val set = markerSets[zone.world] ?: return
        val marker = buildMarker(zone)
        if (marker == null) {
            set.markers.remove("zone-${zone.id}")
            if (zone.banners.isNotEmpty()) {
                console.sendMessage(prefix + cmp("Failed to render zone '${zone.name}' in world ${zone.world}", cError))
            }
            return
        }
        set.markers["zone-${zone.id}"] = marker
    }

    fun remove(zone: Zone) {
        markerSets[zone.world]?.markers?.remove("zone-${zone.id}")
    }

    private fun buildMarker(zone: Zone): Marker? = when (zone.banners.size) {
        0 -> null
        1 -> buildPoi(zone)
        else -> buildShape(zone)
    }

    private fun buildPoi(zone: Zone): Marker? {
        val banner = zone.banners.first()
        val icon = bannerImages[zone.color] ?: return null
        return POIMarker.builder()
            .label(zone.name)
            .icon(icon.content, icon.width / 2, icon.height)
            .position(banner.x + 0.5, banner.y.toDouble(), banner.z + 0.5)
            .detail(detailHtml(zone, 1))
            .build()
    }

    private fun buildShape(zone: Zone): Marker {
        val config = ConfigManager.getConfig(Configs.SETTINGS)
        val styleKey = if (zone.type == ZoneType.STATE) "state" else "player"
        val fillOpacity = config.getDouble("booking.style.$styleKey.fill-opacity", 0.3).toFloat()
        val lineOpacity = config.getDouble("booking.style.$styleKey.line-opacity", 0.85).toFloat()
        val lineWidth = config.getInt("booking.style.$styleKey.line-width", 2)

        val points: List<Point2>
        val area: Int
        if (zone.banners.size == 2) {
            val (a, b) = zone.banners
            val minX = min(a.x, b.x)
            val maxX = max(a.x, b.x)
            val minZ = min(a.z, b.z)
            val maxZ = max(a.z, b.z)
            points = ZonePolygon.rectangle(Point2(minX.toDouble(), minZ.toDouble()), Point2(maxX + 1.0, maxZ + 1.0))
            area = (maxX - minX + 1) * (maxZ - minZ + 1)
        } else {
            points = ZonePolygon.build(zone.banners.sortedBy { it.placedAt }.map { Point2(it.x + 0.5, it.z + 0.5) })
            area = ZonePolygon.area(points).roundToInt()
        }

        val color = zone.color.color
        return ShapeMarker.builder()
            .label(zone.name)
            .shape(Shape(points.map { Vector2d(it.x, it.z) }), zone.banners.maxOf { it.y } + 1f)
            .fillColor(Color(color.red, color.green, color.blue, fillOpacity))
            .lineColor(Color(color.red, color.green, color.blue, lineOpacity))
            .lineWidth(lineWidth)
            .detail(detailHtml(zone, area))
            .build()
    }

    private fun detailHtml(zone: Zone, area: Int): String {
        val type = msgString(if (zone.type == ZoneType.STATE) "zone.detail.type-state" else "zone.detail.type-player")
        val protection = when {
            zone.type == ZoneType.STATE || ZoneManager.protectionDaysLeft(zone) == Long.MAX_VALUE -> msgString("zone.detail.protection-permanent")
            ZoneManager.isBreakableByEveryone(zone) -> msgString("zone.detail.protection-expired")
            else -> msgString("zone.detail.protection-days", listOf(ZoneManager.protectionDaysLeft(zone).toString()))
        }
        val lines = mutableListOf("<b>${zone.name.escapeHtml()}</b>", type)
        zone.owner?.let { owner ->
            val name = Bukkit.getOfflinePlayer(owner).name ?: owner.toString().take(8)
            lines.add(msgString("zone.detail.owner", listOf(name.escapeHtml())))
        }
        lines.add(msgString("zone.detail.founded", listOf(dateFormat.format(zone.createdAt))))
        lines.add(msgString("zone.detail.size", listOf(zone.banners.size.toString(), area.toString())))
        lines.add(msgString("zone.detail.protection", listOf(protection)))
        return lines.joinToString("<br>")
    }

    private fun msgString(key: String, input: List<String> = emptyList()): String =
        plainSerializer.serialize(msg(key, input))

    private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
