package net.ccbluex.liquidbounce.ui.integration.swing.components

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.ui.integration.swing.jMenu
import net.ccbluex.liquidbounce.ui.integration.swing.jMenuBar
import net.ccbluex.liquidbounce.ui.integration.swing.jMenuItem
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.open
import javax.swing.JFrame

fun JFrame.initMenuBar() {
    jMenuBar {
        jMenu("File") {
            jMenuItem("Open Client Folder") { FileManager.dir.open() }
            addSeparator()
            jMenuItem("Open Font Folder") { FileManager.fontsDir.open() }
            jMenuItem("Open Setting Folder") { FileManager.settingsDir.open() }
            jMenuItem("Open Theme Folder") { FileManager.themesDir.open() }
            jMenuItem("Open Script Folder") { ScriptManager.scriptsFolder.open() }
        }

        jMenu("Help") {
            jMenuItem("WebSite") { MiscUtils.showURL("https://${LiquidBounce.CLIENT_WEBSITE}") }
            jMenuItem("GitHub") { MiscUtils.showURL(LiquidBounce.CLIENT_GITHUB) }
            jMenuItem("Forum") {  } // TODO: add links
            jMenuItem("Discord") {  }
            jMenuItem("YouTube") {  }
        }
    }
}
