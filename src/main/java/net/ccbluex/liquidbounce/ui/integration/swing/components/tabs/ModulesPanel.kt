package net.ccbluex.liquidbounce.ui.integration.swing.components.tabs

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.ui.integration.swing.jPanel
import net.ccbluex.liquidbounce.ui.integration.swing.jTabbedPane
import java.awt.FlowLayout
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.ItemEvent
import javax.swing.*

fun JTabbedPane.modulesPanel(): JPanel = jPanel("Modules") {
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)

    jTabbedPane(tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT) {
        for (category in Category.entries) {
            jPanel(category.displayName, category.icon) {
                layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

                val modules = ModuleManager[category]

                jTabbedPane(
                    tabPlacement = JTabbedPane.LEFT,
                    tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
                ) {
                    for (module in modules) {
                        jPanel(module.name) {
                            layout = GridLayout(10, 10, 8, 8)

                            for (value in module.values) {
                                if (value.excluded || !value.isSupported()) {
                                    continue
                                }

                                // TODO completion
                                when (value) {
                                    is BlockValue -> {}
                                    is BoolValue -> {
                                        val checkBox = JCheckBox(value.name, value.get())
                                        checkBox.addItemListener { e ->
                                            when (e.stateChange) {
                                                ItemEvent.SELECTED -> value.set(true, saveImmediately = true)
                                                ItemEvent.DESELECTED -> value.set(false, saveImmediately = true)
                                            }
                                            checkBox.isSelected = value.get()
                                        }
                                        add(checkBox)
                                    }

                                    is ColorValue -> {}
                                    is Configurable -> {}
                                    is FloatRangeValue -> {}
                                    is FloatValue -> {}
                                    is FontValue -> {}
                                    is IntRangeValue -> {}
                                    is IntValue -> {}

                                    is ListValue -> {
                                        jPanel {
                                            layout = GridLayout()

                                            add(JLabel(value.name))
                                            val comboBox = JComboBox(value.values)
                                            comboBox.isEditable = false
                                            comboBox.addActionListener {
                                                val newValue = comboBox.selectedItem as? String ?: return@addActionListener
                                                value.set(newValue, true)
                                                comboBox.selectedItem = value.get()
                                            }
                                            add(comboBox)
                                        }
                                    }

                                    is TextValue -> {
                                        jPanel {
                                            layout = FlowLayout()

                                            add(JLabel(value.name))
                                            val textField = JTextField(value.get())
                                            textField.isEditable = true
                                            textField.addActionListener {
                                                val newValue = textField.text
                                                value.set(newValue, true)
                                                textField.text = value.get()
                                            }
                                            add(textField)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
