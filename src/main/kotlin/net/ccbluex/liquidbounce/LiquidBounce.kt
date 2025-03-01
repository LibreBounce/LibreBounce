/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.liquidbounce

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.core.scope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.Reconnect
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.features.itemgroup.groups.heads
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.client.ipcConfiguration
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.browser.BrowserManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.integration.task.TaskProgressScreen
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.HAS_AMD_VEGA_APU
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.aiming.PostRotationExecutor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.input.InputTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.resource.ReloadableResourceManagerImpl
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.SynchronousResourceReloader
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.time.measureTime

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce : EventListener {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_AUTHOR = "CCBlueX"

    val clientVersion = gitInfo["git.build.version"]?.toString() ?: "unknown"
    val clientCommit = gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown"
    val clientBranch = gitInfo["git.branch"]?.toString() ?: "nextgen"

    /**
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of semantic versioning.
     *
     * TODO: Replace this approach with full semantic versioning.
     */
    const val IN_DEVELOPMENT = true

    val isIntegrationTesting = !System.getenv("TENACC_TEST_PROVIDER").isNullOrBlank()

    /**
     * Client logger to print out console messages
     */
    val logger = LogManager.getLogger(CLIENT_NAME)!!

    var taskManager: TaskManager? = null

    var isInitialized = false
        private set

    private fun initializeClient() {
        if (isInitialized) {
            return
        }

        isInitialized = true

        // Config
        ConfigSystem

        // Utility
        RenderedEntities
        ChunkScanner
        InputTracker

        // Feature managers
        ModuleManager
        CommandManager
        ProxyManager
        AccountManager

        // Script system
        EnvironmentRemapper
        ScriptManager

        // Utility managers
        RotationManager
        PacketQueueManager
        InteractionTracker
        CombatManager
        FriendManager
        InventoryManager
        WorldToScreen
        Reconnect
        ActiveServerList
        ConfigSystem.root(ClientItemGroups)
        ConfigSystem.root(LanguageManager)
        ConfigSystem.root(ClientAccountManager)
        ConfigSystem.root(SpooferManager)
        PostRotationExecutor
        TpsObserver

        // Register commands and modules
        CommandManager.registerInbuilt()
        ModuleManager.registerInbuilt()

        // Load user scripts
        ScriptManager.loadAll()

        val tasks = listOf(
            scope.async {
                // Load translations
                LanguageManager.loadDefault()
            },
            scope.async {
                val update = update ?: return@async
                logger.info("[Update] Update available: $clientVersion -> ${update.lbVersion}")
            },
            scope.async {
                // Load cosmetics
                CosmeticService.refreshCarriers(force = true) {
                    logger.info("Successfully loaded ${CosmeticService.carriers.size} cosmetics carriers.")
                }
            },
            scope.async {
                // Download player heads
                heads
            },
            scope.async {
                // Load configs
                configs
            },
            scope.async {
                // IPC configuration
                ipcConfiguration
            },
            scope.async {
                IpInfoApi.original
            },
            scope.async {
                if (ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT) {
                    runCatching {
                        ClientAccountManager.clientAccount.renew()
                    }.onFailure {
                        logger.error("Failed to renew client account token.", it)
                        ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
                    }.onSuccess {
                        logger.info("Successfully renewed client account token.")
                        ConfigSystem.storeConfigurable(ClientAccountManager)
                    }
                }
            }
        )

        runBlocking {
            tasks.awaitAll()
        }

        // Load theme and component overlay
        ThemeManager
        BrowserManager

        // Start Interop Server
        ClientInteropServer.start()

        IntegrationListener

        taskManager = TaskManager(scope).apply {
            launch("MCEF", BrowserManager::initBrowser)
        }

        FontManager
        ComponentOverlay.insertComponents()

        // Queue fonts of all themes
        // TODO: Will be removed with PR #3884 as it is not needed anymore
        ThemeManager.themesFolder.listFiles()
            ?.filter { file -> file.isDirectory }
            ?.forEach { file ->
                runCatching {
                    val assetsFolder = File(file, "assets")
                    if (!assetsFolder.exists()) {
                        return@forEach
                    }

                    FontManager.queueFolder(assetsFolder)
                }.onFailure {
                    logger.error("Failed to queue fonts from theme '${file.name}'.", it)
                }
            }

        // Load fonts
        val duration = measureTime {
            FontManager.createGlyphManager()
        }
        logger.info("Completed loading fonts in ${duration.inWholeMilliseconds} ms.")
        logger.info("Fonts: [ ${FontManager.fontFaces.joinToString { face -> face.name }} ]")

        ItemImageAtlas

        if (HAS_AMD_VEGA_APU) {
            logger.info("AMD Vega iGPU detected, enabling different line smooth handling. " +
                "If you believe this is a mistake, please create an issue at " +
                "https://github.com/CCBlueX/LiquidBounce/issues.")
        }

        // Load all configurations
        ConfigSystem.loadAll()
    }

    /**
     * Should be executed to start the client.
     */
    @Suppress("unused")
    private val startHandler = handler<ClientStartEvent> {
        runCatching {
            logger.info("Launching $CLIENT_NAME v$clientVersion by $CLIENT_AUTHOR")
            // Print client information
            logger.info("Client Version: $clientVersion ($clientCommit)")
            logger.info("Client Branch: $clientBranch")
            logger.info("Operating System: ${System.getProperty("os.name")} (${System.getProperty("os.version")})")
            logger.info("Java Version: ${System.getProperty("java.version")}")
            logger.info("Screen Resolution: ${mc.window.width}x${mc.window.height}")
            logger.info("Refresh Rate: ${mc.window.refreshRate} Hz")

            // Initialize event manager
            EventManager

            // Register resource reloader
            val resourceManager = mc.resourceManager
            val clientInitializer = ClientInitializer()
            if (resourceManager is ReloadableResourceManagerImpl) {
                resourceManager.registerReloader(clientInitializer)
            } else {
                logger.warn("Failed to register resource reloader!")

                // Run resource reloader directly as fallback
                clientInitializer.reload(resourceManager)
            }
        }.onFailure(ErrorHandler::fatal)
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent>(priority = FIRST_PRIORITY) { event ->
        val taskManager = taskManager ?: return@handler

        if (!taskManager.isCompleted && event.screen !is TaskProgressScreen) {
            event.cancelEvent()
            mc.setScreen(TaskProgressScreen("Loading Required Libraries", taskManager))
        }
    }

    /**
     * Resource reloader which is executed on client start and reload.
     * This is used to run async tasks without blocking the main thread.
     *
     * For now this is only used to check for updates and request additional information from the internet.
     *
     * @see SynchronousResourceReloader
     * @see ResourceReloader
     */
    class ClientInitializer : SynchronousResourceReloader {
        override fun reload(manager: ResourceManager) {
            runCatching(::initializeClient).onSuccess {
                logger.info("$CLIENT_NAME has been successfully initialized.")
            }.onFailure(ErrorHandler::fatal)
        }
    }

    /**
     * Should be executed to stop the client.
     */
    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        logger.info("Shutting down client...")

        ConfigSystem.storeAll()
        ChunkScanner.ChunkScannerThread.stopThread()

        // Shutdown browser as last step
        BrowserManager.shutdownBrowser()
    }

}
