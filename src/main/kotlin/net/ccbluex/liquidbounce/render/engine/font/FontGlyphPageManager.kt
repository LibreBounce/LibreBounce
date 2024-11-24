package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import java.awt.Dimension
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

private val BASIC_CHARS = '\u0000'..'\u0200'

class FontGlyphPageManager(
    baseFonts: Set<FontManager.FontFace>,
    additionalFonts: Set<FontManager.FontFace> = emptySet()
): Listenable {

    private var staticPage: List<StaticGlyphPage> = StaticGlyphPage.createGlyphPages(baseFonts.flatMap { loadedFont ->
        loadedFont.styles.filterNotNull().flatMap { font -> BASIC_CHARS.map { ch -> FontGlyph(ch, font) } }
    })
    private val dynamicPage: DynamicGlyphPage = DynamicGlyphPage(
        Dimension(1024, 1024),
        ceil(baseFonts.elementAt(0).styles[0]!!.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManager = DynamicFontCacheManager(
        this.dynamicPage,
        baseFonts + additionalFonts
    )

    private val availableFonts: Map<FontManager.FontFace, FontGlyphRegistry>
    private val dynamicallyLoadedGlyphs = HashMap<Pair<Int, Char>, GlyphDescriptor>()

    init {
        this.dynamicFontManager.startThread()

        this.availableFonts = createGlyphRegistries(baseFonts, this.staticPage)
    }

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> {
        this.dynamicFontManager.update().forEach { update ->
            val key = update.style to update.descriptor.renderInfo.char

            if (!update.removed) {
                dynamicallyLoadedGlyphs[key] = update.descriptor
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun createGlyphRegistries(
        baseFonts: Set<FontManager.FontFace>,
        glyphPages: List<StaticGlyphPage>
    ): Map<FontManager.FontFace, FontGlyphRegistry> {
        val fontMap = baseFonts.associateWith {
            Array(4) {
                ConcurrentHashMap<Char, GlyphDescriptor>()
            }
        }

        baseFonts.forEach { loadedFont ->
            loadedFont.styles.filterNotNull().forEach { fontId ->
                glyphPages.forEach { glyphPage ->
                    glyphPage.glyphs
                        .filter { it.first == fontId }
                        .forEach { (font, glyphRenderInfo) ->
                            fontMap[loadedFont]!![font.style][glyphRenderInfo.char] =
                                GlyphDescriptor(glyphPage, glyphRenderInfo)
                        }
                }
            }
        }

        return fontMap.entries.associate {
            it.key to FontGlyphRegistry(it.value, it.value[0]['?']!!)
        }
    }

    private fun getFont(font: FontManager.FontFace): FontGlyphRegistry {
        return availableFonts[font] ?: error("Font $font is not registered")
    }

    fun requestGlyph(font: FontManager.FontFace, style: Int, ch: Char): GlyphDescriptor? {
        val glyph = getFont(font).glyphs[style][ch]

        if (glyph == null) {
            val altGlyph = this.dynamicallyLoadedGlyphs[style to ch]

            if (altGlyph == null) {
                this.dynamicFontManager.requestGlyph(ch, style)
            } else {
                return altGlyph
            }
        }

        return glyph
    }

    fun getFallbackGlyph(font: FontManager.FontFace): GlyphDescriptor {
        return getFont(font).fallbackGlyph
    }

    fun unload() {
        this.dynamicPage.texture.close()
        this.staticPage.forEach { it.texture.close() }
    }

    private class FontGlyphRegistry(
        val glyphs: Array<ConcurrentHashMap<Char, GlyphDescriptor>>,
        val fallbackGlyph: GlyphDescriptor
    )

}

class GlyphDescriptor(val page: GlyphPage, val renderInfo: GlyphRenderInfo)
