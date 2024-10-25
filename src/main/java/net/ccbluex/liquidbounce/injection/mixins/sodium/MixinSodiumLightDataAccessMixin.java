/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.sodium;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public class MixinSodiumLightDataAccessMixin {

    @Shadow
    protected BlockRenderView world;

    @Shadow
    @Final
    private BlockPos.Mutable pos;

    /**
     * Maximum light level for all color channels.
     * <p>
     * Minecraft's lighting system represents light in a range of 0-15,
     * where 15 corresponds to maximum brightness.
     */
    @Unique
    private static final int MAX_LIGHT_LEVEL = 15 | 15 << 4 | 15 << 8;

    @ModifyVariable(method = "compute", at = @At(value = "TAIL"), name = "bl")
    private int modifyLightLevel(int original) {
        var xray = ModuleXRay.INSTANCE;
        if (xray.getEnabled() && xray.getFullBright()) {
            var blockState = world.getBlockState(pos);

            if (xray.shouldRender(blockState, pos)) {
                // Ensures that the brightness is on max for all color channels
                return MAX_LIGHT_LEVEL;
            }
        }

        return original;
    }

}
