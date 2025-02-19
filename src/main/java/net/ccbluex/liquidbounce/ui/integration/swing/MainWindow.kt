package net.ccbluex.liquidbounce.ui.integration.swing

import com.formdev.flatlaf.themes.FlatMacLightLaf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.autoSettingsList
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.ui.integration.swing.components.initMenuBar
import net.ccbluex.liquidbounce.ui.integration.swing.components.tabs.modulesPanel
import net.ccbluex.liquidbounce.utils.render.IconUtils
import net.ccbluex.liquidbounce.utils.render.IconUtils.initClientIcon
import java.awt.BorderLayout
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

private const val closeOperation = JFrame.HIDE_ON_CLOSE

object MainWindow {

    private val frame = JFrame(LiquidBounce.CLIENT_NAME)

    init {
        frame.apply {
            defaultCloseOperation = closeOperation
            setSize(800, 600)
            setLocationRelativeTo(null) // Middle

            initClientIcon()

            initMenuBar()

            jTabbedPane(tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT) {
                modulesPanel()

                jPanel("Client") {

                }

                // TODO: Scripts Fonts Alts Themes Configs OnlineConfigs

//                jPanel("Online Configs") {
//                }
//
//                jPanel("Swing") {
//                    val lookAndFeels = UIManager.getInstalledLookAndFeels()
//                    var currentIndex = 0
//
//                    jButton("Switch Theme") {
//                        currentIndex = (currentIndex + 1) % lookAndFeels.size
//                        val info = lookAndFeels[currentIndex]
//                        try {
//                            UIManager.setLookAndFeel(info.className)
//                            SwingUtilities.updateComponentTreeUI(frame)
//                        } catch (ex: Exception) {
//                            ex.printStackTrace()
//                        }
//                    }
//                }
            }
        }
    }

    fun show() {
        SwingUtilities.invokeLater {
            if (!frame.isVisible) {
                frame.isVisible = true
            }
        }
    }

    fun hide() {
        SwingUtilities.invokeLater {
            if (frame.isVisible) {
                frame.isVisible = false
            }
        }
    }

}
