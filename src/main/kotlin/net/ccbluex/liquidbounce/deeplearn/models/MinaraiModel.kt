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
 *
 */
package net.ccbluex.liquidbounce.deeplearn.models

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import java.nio.file.Path

class MinaraiModel : ModelWrapper<FloatArray, FloatArray> {

    constructor(name: String, parent: ChoiceConfigurable<*>) : super(
        name,
        FloatArrayInAndOutTranslator(),
        2, // X, Y
        parent
    )

    constructor(path: Path, parent: ChoiceConfigurable<*>) : super(
        path,
        FloatArrayInAndOutTranslator(),
        2,
        parent
    )

}
