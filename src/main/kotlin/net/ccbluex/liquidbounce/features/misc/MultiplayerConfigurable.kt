package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleAutoConfig
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleBungeeSpoofer
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleResourceSpoof
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleSpoofer

// todo: this is just a placeholder for testing, do it properly later
object MultiplayerConfigurable : Configurable("multiplayer") {
    init {
        // tree all modules (features)
        tree(ModuleSpoofer)
        tree(ModuleResourceSpoof)
        tree(ModuleBungeeSpoofer)
        tree(ModuleAutoConfig)
    }
}
