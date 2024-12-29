package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.performSwap
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.WrittenBookContentComponent
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket
import net.minecraft.text.RawFilteredPair
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.util.*

/**
 * ModuleBookBot
 *
 * This module simplifies the process of filling and creating books using various principles,
 * enabling efficient generation and potential automation for mass book creation or "spam."
 *
 * @author sqlerrorthing
 * @since 12/28/2024
 **/
object ModuleBookBot : ClientModule("BookBot", Category.MISC, disableOnQuit = true) {
    val generationMode =
        choices(
            "Mode",
            RandomGenerationMode,
            arrayOf(
                RandomGenerationMode,
            ),
        ).apply { tagBy(this) }

    private object Sign : ToggleableConfigurable(ModuleBookBot, "Sign", true) {
        val bookName by text("Name", "Generated book #%count%")
    }

    init {
        treeAll(Sign)
    }

    private val delay by int("Delay", 20, 0..200)

    private var bookCount = 0

    internal var random: Random = Random()
        private set

    override fun enable() {
        bookCount = 0
        random = Random()
    }

    @Suppress("unused")
    private val gameTickHandler = tickHandler {
        val book = findInventorySlot {
            val component = it.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) ?: return@findInventorySlot false
            return@findInventorySlot it.item == Items.WRITABLE_BOOK && component.pages.isEmpty()
        } ?: run {
            enabled = false
            return@tickHandler
        }

        book.performSwap(to = HotbarItemSlot(player.inventory.selectedSlot)).performAction()

        waitTicks(delay)

        writeBook()
    }

    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    private fun writeBook() {
        val chars = generationMode.activeChoice.generate()
        val pages = ArrayList<String>()
        val filteredPages = ArrayList<RawFilteredPair<Text>>()
        val widthRetriever = mc.textRenderer.textHandler.widthRetriever

        var pageIndex = 0
        var lineIndex = 0

        val page = StringBuilder()

        var lineWidth = 0.0f

        while (chars.hasNext()) {
            val char = chars.nextInt().toChar()

            if (char == '\r' || char == '\n') {
                page.append('\n')
                lineWidth = 0.0f
                lineIndex++
            } else {
                val charWidth = widthRetriever.getWidth(char.code, Style.EMPTY)

                if (lineWidth + charWidth > 114f) {
                    page.append('\n')
                    lineWidth = charWidth
                    lineIndex++

                    if (lineIndex != 14) page.appendCodePoint(char.code)
                } else if (lineWidth == 0f && char == ' ') {
                    continue
                } else {
                    lineWidth += charWidth
                    page.appendCodePoint(char.code)
                }
            }

            if (lineIndex == 14) {
                filteredPages.add(RawFilteredPair.of(Text.of(page.toString())))
                pages.add(page.toString())
                page.setLength(0)
                pageIndex++
                lineIndex = 0

                if (pageIndex == generationMode.activeChoice.pages) {
                    break
                }

                if (char != '\r' && char != '\n') {
                    page.appendCodePoint(char.code)
                }
            }
        }

        if (page.isNotEmpty() && pageIndex != generationMode.activeChoice.pages) {
            filteredPages.add(RawFilteredPair.of(Text.of(page.toString())))
            pages.add(page.toString())
        }

        writeBook(Sign.bookName.replace("%count%", bookCount.toString()),
            filteredPages, pages)

        bookCount++
    }

    private fun writeBook(
        title: String,
        filteredPages: ArrayList<RawFilteredPair<Text>>,
        pages: ArrayList<String>
    ) {
        player.mainHandStack.set(
            DataComponentTypes.WRITTEN_BOOK_CONTENT,
            WrittenBookContentComponent(
                RawFilteredPair.of(title),
                player.gameProfile.name,
                0,
                filteredPages,
                true
            )
        )

        player.networkHandler.sendPacket(
            BookUpdateC2SPacket(
                player.inventory.selectedSlot,
                pages,
                if (Sign.enabled) Optional.of(title) else Optional.empty()
            )
        )
    }
}

abstract class GenerationMode(
    name: String,
) : Choice(name) {
    override val parent: ChoiceConfigurable<*> = ModuleBookBot.generationMode

    abstract val pages: Int

    abstract fun generate(): PrimitiveIterator.OfInt
}

object RandomGenerationMode : GenerationMode("Random") {
    override val pages by int("Pages", 50, 0..100)

    private val asciiOnly by boolean("AsciiOnly", false)

    override fun generate(): PrimitiveIterator.OfInt {
        val origin = if (asciiOnly) 0x21 else 0x0800
        val bound = if (asciiOnly) 0x7E else 0x10FFFF

        return ModuleBookBot.random
            .ints(origin, bound)
            .filter { !Character.isWhitespace(it) && it.toChar() != '\r' && it.toChar() != '\n' }
            .iterator()
    }
}
