package de.miraculixx.bmbm.territory.model

import de.miraculixx.bmbm.utils.serializer.DyeColorSerializer
import de.miraculixx.bmbm.utils.serializer.InstantSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bukkit.DyeColor
import java.time.Instant
import java.util.UUID

enum class ZoneType { PLAYER, STATE }

@Serializable
data class BannerPos(val world: String, val x: Int, val y: Int, val z: Int)

@Serializable
data class ZoneBanner(
    val x: Int,
    val y: Int,
    val z: Int,
    @Serializable(with = DyeColorSerializer::class) val dye: DyeColor,
    @Serializable(with = InstantSerializer::class) val placedAt: Instant,
    @Contextual val placedBy: UUID
)

@Serializable
data class Zone(
    @Contextual val id: UUID,
    val name: String,
    val type: ZoneType,
    @Contextual val owner: UUID?,
    val world: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    val banners: MutableList<ZoneBanner> = mutableListOf()
) {
    val color: DyeColor get() = banners.maxByOrNull { it.placedAt }?.dye ?: DyeColor.WHITE

    fun bannerAt(pos: BannerPos): ZoneBanner? =
        if (pos.world != world) null else banners.firstOrNull { it.x == pos.x && it.y == pos.y && it.z == pos.z }
}

@Serializable
data class ZoneStorage(val version: Int = 1, val zones: MutableList<Zone> = mutableListOf())
