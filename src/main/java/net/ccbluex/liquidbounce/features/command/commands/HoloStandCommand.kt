/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.NBTTagCompound
import net.ccbluex.liquidbounce.utils.extensions.NBTTagList
import net.ccbluex.liquidbounce.utils.extensions.set
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction

object HoloStandCommand : Command("holostand") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 4) {
            if (mc.playerController.isNotCreative) {
                chat("§c§lError: §3You need to be in creative mode.")
                return
            }

            try {
                val x = args[1].toDouble()
                val y = args[2].toDouble()
                val z = args[3].toDouble()
                val message = StringUtils.toCompleteString(args, 4)

                val itemStack = ItemStack(Items.armor_stand)

                itemStack.tagCompound = NBTTagCompound {
                    this["EntityTag"] = NBTTagCompound {
                        this["Invisible"] = 1
                        this["CustomName"] = message
                        this["CustomNameVisible"] = 1
                        this["NoGravity"] = 1
                        this["Pos"] = NBTTagList {
                            appendTag(NBTTagDouble(x))
                            appendTag(NBTTagDouble(y))
                            appendTag(NBTTagDouble(z))
                        }
                    }
                }

                itemStack.setStackDisplayName("§c§lHolo§eStand")

                sendPacket(C10PacketCreativeInventoryAction(36, itemStack))

                chat("The HoloStand was successfully added to your inventory.")
            } catch (exception: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("holostand <x> <y> <z> <message...>")
    }
}