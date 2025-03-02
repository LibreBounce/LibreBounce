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
 */

package net.ccbluex.liquidbounce.common;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;

public class CustomRenderPhase {

    /**
     * This allows to render textures with bilinear filtering,
     * which will make them look smoother, unlike with RenderLayer::getGuiTextured.
     */
    private static final Function<Identifier, RenderLayer> TEXTURE_BILINEAR = Util.memoize(
            texture ->
                    RenderLayer.of(
                            "translucent_antialiasing",
                            VertexFormats.POSITION_TEXTURE_COLOR,
                            VertexFormat.DrawMode.QUADS,
                            786432,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new RenderPhase.Texture(texture, TriState.DEFAULT, false))
                                    .program(POSITION_TEXTURE_COLOR_PROGRAM)
                                    .transparency(TRANSLUCENT_TRANSPARENCY)
                                    .depthTest(ALWAYS_DEPTH_TEST)
                                    .build(false)
                    ));

    public static RenderLayer getTextureBilinear(Identifier texture) {
        return TEXTURE_BILINEAR.apply(texture);
    }

}
