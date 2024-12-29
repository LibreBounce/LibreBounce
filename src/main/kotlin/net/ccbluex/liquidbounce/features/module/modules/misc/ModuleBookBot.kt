package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.WrittenBookContentComponent
import net.minecraft.item.ItemStack
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
    private val inventoryConstraints = tree(PlayerInventoryConstraints())

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

    private val delay by float("Delay", .5f, 0f..20f, suffix = "s")

    private val chronometer = Chronometer()

    private var bookCount = 0

    internal var random: Random = Random()
        private set

    override fun enable() {
        bookCount = 0
        random = Random()
        chronometer.reset()
    }

    private val isCandidate: (ItemStack) -> Boolean = {
        val component = it.get(DataComponentTypes.WRITABLE_BOOK_CONTENT)
        it.item == Items.WRITABLE_BOOK && component?.pages?.isEmpty() == true
    }

    private val randomBook get() = findInventorySlot(isCandidate)

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        val book = randomBook ?: run {
            enabled = false
            return@handler
        }

        if (!isCandidate(player.mainHandStack)) {
            event.schedule(inventoryConstraints, ClickInventoryAction.performSwap(
                from = book,
                to = HotbarItemSlot(player.inventory.selectedSlot),
            ))
        }

        if (chronometer.hasElapsed((delay * 1000L).toLong())) {
            chronometer.reset()
            writeBook()
        }
    }

    /**
     * Generates a book with content based on the active choice of the generation mode.
     * The book content is generated character by character, and the text is split into pages,
     * ensuring that each page contains lines that fit within the given width constraints.
     *
     * This method processes each character from the generator, managing line breaks and page formatting,
     * and stores the generated text in the `pages` and `filteredPages` lists. Once a page is full, it is
     * added to the collection, and the process continues until the specified number of pages is reached.
     *
     * The method performs the following steps:
     * - Generates characters using the active choice from the generation mode.
     * - Breaks lines based on a width limit (114f) and ensures that a line fits within this constraint.
     * - Adds new lines when a line exceeds the width limit or encounters a line break character (`\r` or `\n`).
     * - If a page is full, it is added to the `pages` and `filteredPages` lists, and the process continues.
     * - Stops once the desired number of pages is generated.
     *
     * The generated pages are used to create a book with the specified name, which is then saved.
     *
     *
     * @see PrimitiveIterator.OfInt
     * @see GenerationMode.generate
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    private fun writeBook() {
        if (!isCandidate(player.mainHandStack)) {
            return
        }

        val chars = generationMode.activeChoice.generate()
        val widthRetriever = mc.textRenderer.textHandler.widthRetriever

        val pages = ArrayList<String>()
        val filteredPages = ArrayList<RawFilteredPair<Text>>()

        var pageIndex = 0
        var lineIndex = 0
        var lineWidth = 0.0f
        val page = StringBuilder()

        while (chars.hasNext()) {
            val char = chars.nextInt().toChar()

            if (char == '\r' || char == '\n') {
                page.append('\n')
                lineWidth = 0.0f
                lineIndex++
            } else {
                val charWidth = widthRetriever.getWidth(char.code, Style.EMPTY)

                if (lineWidth + charWidth > 114f) {
                    lineIndex++
                    lineWidth = charWidth
                    appendLineBreak(page, lineIndex)
                } else if (lineWidth == 0f && char == ' ') {
                    continue
                } else {
                    lineWidth += charWidth
                    page.appendCodePoint(char.code)
                }
            }

            if (lineIndex == 14) {
                addPageToBook(page, pages, filteredPages)
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
            addPageToBook(page, pages, filteredPages)
        }

        writeBook(Sign.bookName.replace("%count%", bookCount.toString()),
            filteredPages, pages)

        bookCount++
    }

    private fun appendLineBreak(page: StringBuilder, lineIndex: Int) {
        page.append('\n')
        if (lineIndex != 14) {
            page.appendCodePoint(' '.code)
        }
    }

    private fun addPageToBook(
        page: StringBuilder,
        pages: MutableList<String>,
        filteredPages: MutableList<RawFilteredPair<Text>>
    ) {
        filteredPages.add(RawFilteredPair.of(Text.of(page.toString())))
        pages.add(page.toString())
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
