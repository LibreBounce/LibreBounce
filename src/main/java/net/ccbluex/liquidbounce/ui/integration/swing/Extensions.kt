package net.ccbluex.liquidbounce.ui.integration.swing

import java.awt.Container
import javax.swing.*

// Menu Bar

inline fun JFrame.jMenuBar(
    builderAction: JMenuBar.() -> Unit
) = JMenuBar().apply(builderAction).also { jMenuBar = it }

inline fun JMenuBar.jMenu(
    name: String = "",
    builderAction: JMenu.() -> Unit
) = JMenu(name).apply(builderAction).also { add(it) }

inline fun JMenu.jMenuItem(
    text: String? = null,
    icon: Icon? = null,
    crossinline onClick: () -> Unit
) = JMenuItem(text, icon).apply { addActionListener { onClick() } }.also { add(it) }

// Tabs

inline fun Container.jTabbedPane(
    tabPlacement: Int = JTabbedPane.TOP,
    tabLayoutPolicy: Int = JTabbedPane.WRAP_TAB_LAYOUT,
    builderAction: JTabbedPane.() -> Unit
) = JTabbedPane(tabPlacement, tabLayoutPolicy).apply(builderAction).also { add(it) }

inline fun JTabbedPane.jPanel(
    title: String = "",
    icon: Icon? = null,
    tip: String? = null,
    builderAction: JPanel.() -> Unit
) = JPanel().apply(builderAction).also { addTab(title, icon, it, tip) }

// General

inline fun JComponent.jPanel(
    builderAction: JPanel.() -> Unit
) = JPanel().apply(builderAction).also { add(it) }

inline fun JComponent.jButton(
    text: String? = null,
    icon: Icon? = null,
    crossinline onClick: () -> Unit
) = JButton(text, icon).apply { addActionListener { onClick() } }.also { add(it) }