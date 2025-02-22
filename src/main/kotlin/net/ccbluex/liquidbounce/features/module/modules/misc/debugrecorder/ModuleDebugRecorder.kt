package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.*
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.text.SimpleDateFormat
import java.util.*

object ModuleDebugRecorder : ClientModule("DebugRecorder", Category.MISC) {

    val modes = choices("Mode", GenericDebugRecorder, arrayOf(
        MinaraiCombatRecorder,
        MinaraiTrainer,

        GenericDebugRecorder,
        DebugCPSRecorder,
        AimDebugRecorder,
        BoxDebugRecorder
    ))

    abstract class DebugRecorderMode<T>(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        val folder = ConfigSystem.rootFolder.resolve("debug-recorder/$name").apply {
            mkdirs()
        }
        internal val packets = mutableListOf<T>()

        protected fun recordPacket(packet: T) {
            if (!this.isSelected) {
                return
            }

            packets.add(packet)
        }

        override fun enable() {
            this.packets.clear()
            chat(regular("Recording "), variable(name), regular("..."))
        }

        internal val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        override fun disable() {
            if (this.packets.isEmpty()) {
                chat(regular("No packets recorded."))
                return
            }

            runCatching {
                val baseName = dateFormat.format(Date())
                var file = folder.resolve("${baseName}.json")

                var idx = 0
                while (file.exists()) {
                    file = folder.resolve("${baseName}_${idx++}.json")
                }

                runCatching { file.parentFile.mkdirs() }

                fileGson.newJsonWriter(file.writer()).use {
                    fileGson.toJson(this.packets, this.packets.javaClass, it)
                }

                file.absolutePath
            }.onFailure {
                chat(markAsError("Failed to write log to file $it".asText()))

                runCatching {
                    val selection = StringSelection(fileGson.toJson(this.packets))
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                }.onFailure {
                    chat(markAsError("Failed to write log to clipboard".asText()))
                }.onSuccess {
                    chat(regular("Log was written to clipboard."))
                }
            }.onSuccess { path ->
                val text = path.asText().styled {
                    it.withUnderline(true)
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, regular("Browse...")))
                        .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_FILE, path.toString()))
                }

                chat(regular("Log was written to "), text, regular("."))
            }

            this.packets.clear()
        }
    }
}
