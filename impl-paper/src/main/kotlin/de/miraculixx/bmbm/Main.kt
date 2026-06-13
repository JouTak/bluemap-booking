package de.miraculixx.bmbm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmbm.commands.OverviewCommand
import de.miraculixx.bmbm.map.gui.ClickManager
import de.miraculixx.bmbm.booking.ZoneManager
import de.miraculixx.bmbm.booking.ZoneRenderer
import de.miraculixx.bmbm.booking.events.ZonePlaceListener
import de.miraculixx.bmbm.booking.events.ZoneProtectionListener
import de.miraculixx.bmbm.booking.events.ZoneValidationListener
import de.miraculixx.bmbm.utils.Listener
import de.miraculixx.bmbm.utils.cache.MarkerImages
import de.miraculixx.bmbm.utils.config.ConfigManager
import de.miraculixx.bmbm.utils.config.Configs
import de.miraculixx.kpaper.localization.Localization
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.kpaper.main.KPaperConfiguration
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIPaperConfig
import java.io.File
import java.util.function.Consumer

class Main : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
        lateinit var localization: Localization
    }

    private lateinit var listener: List<Listener>
    private lateinit var assetsLoader: MarkerImages

    override fun load() {
        CommandAPI.onLoad(CommandAPIPaperConfig(this).silentLogs(true))

        dataFolder.mkdir()
    }

    override fun startup() {
        INSTANCE = this
        CommandAPI.onEnable()

        // Setup
        KPaperConfiguration.Events.autoRegistration = false

        // Load Content
        assetsLoader = MarkerImages()
        ZoneManager.load()
        listener = listOf(ZonePlaceListener(), ZoneProtectionListener(), ZoneValidationListener(), ClickManager())
        OverviewCommand()

        BlueMapAPI.onEnable(onBlueMapEnable)
        BlueMapAPI.onDisable(onBlueMapDisable)
    }

    override fun shutdown() {
        CommandAPI.onDisable()
        BlueMapAPI.unregisterListener(onBlueMapEnable)
        BlueMapAPI.unregisterListener(onBlueMapDisable)
        ZoneManager.save()
        logger.info("Successfully saved all data! Good Bye :)")
    }

    private val onBlueMapEnable = Consumer<BlueMapAPI> {
        logger.info("Connect to BlueMap API...")
        assetsLoader.loadImages(it)
        Configs.entries.forEach { c -> ConfigManager.reload(c) }
        val config = ConfigManager.getConfig(Configs.SETTINGS)
        val languages = listOf("en_US", "de_DE", "ru_RU", "fr_FR").map { it to javaClass.getResourceAsStream("/language/$it.yml") }
        localization = Localization(File("${dataFolder}/language"), config.getString("language") ?: "en_US", languages)
        ZoneRenderer.connect(it)
        listener.forEach { listener -> listener.register() }
        logger.info("Successfully enabled BlueMap Booking addition!")
    }

    private val onBlueMapDisable = Consumer<BlueMapAPI> {
        logger.info("Disconnecting from BlueMap API...")
        listener.forEach { listener -> listener.unregister() }
        assetsLoader.unloadImages()
        ZoneRenderer.disconnect()
        ZoneManager.save()
        logger.info("Successfully saved all data. Waiting for BlueMap to reload...")
    }
}

val PluginManager by lazy { Main.INSTANCE }