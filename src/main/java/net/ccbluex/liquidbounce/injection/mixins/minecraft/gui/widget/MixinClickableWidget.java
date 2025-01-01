package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.widget;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
 *
 */
@Mixin(ClickableWidget.class)
public abstract class MixinClickableWidget {
    @Shadow
    public abstract int getRight();

    @Shadow
    public abstract int getHeight();

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract boolean isHovered();

    @Shadow
    public boolean active;
    @Shadow
    protected float alpha;

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract int getX();

    @Shadow
    public abstract Text getMessage();
}
