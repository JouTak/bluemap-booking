package de.miraculixx.bmbm.booking.events

import de.miraculixx.bmbm.booking.ZoneManager
import de.miraculixx.bmbm.booking.ZoneRenderer
import de.miraculixx.bmbm.booking.model.BannerPos
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.bmbm.utils.messages.prefix
import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.extensions.bukkit.cmp
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.runnables.taskRunLater
import org.bukkit.Chunk
import org.bukkit.Tag
import org.bukkit.event.world.ChunkLoadEvent

class ZoneValidationListener : Listener {
    private val onChunkLoad = listen<ChunkLoadEvent> { validateChunk(it.chunk) }

    private fun validateChunk(chunk: Chunk) {
        val worldName = chunk.world.name
        ZoneManager.all().forEach { zone ->
            if (zone.world != worldName) return@forEach
            zone.banners.filter { it.x shr 4 == chunk.x && it.z shr 4 == chunk.z }.forEach { banner ->
                val block = chunk.world.getBlockAt(banner.x, banner.y, banner.z)
                if (Tag.BANNERS.isTagged(block.type)) return@forEach
                val deleted = ZoneManager.removeBanner(zone, BannerPos(worldName, banner.x, banner.y, banner.z))
                if (deleted) ZoneRenderer.remove(zone) else ZoneRenderer.render(zone)
                console.sendMessage(prefix + cmp("Zone '${zone.name}' lost a banner at ${banner.x} ${banner.y} ${banner.z} ($worldName) while the plugin was offline"))
            }
        }
    }

    override fun register() {
        onChunkLoad.register()
        taskRunLater(1, false) {
            worlds.forEach { world -> world.loadedChunks.forEach { validateChunk(it) } }
        }
    }

    override fun unregister() {
        onChunkLoad.unregister()
    }
}
