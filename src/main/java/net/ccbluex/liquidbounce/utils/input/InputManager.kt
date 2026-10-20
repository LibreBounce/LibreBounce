package net.ccbluex.liquidbounce.utils.input

import net.minecraft.client.Minecraft
import net.minecraft.client.options.GameOptions
import net.minecraft.client.options.KeyBinding
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

// Credits to Illusion Client for this!
object InputManager : MinecraftInstance {
	val player: InputState = InputState()
	val client: InputState = InputState()
	val options: GameOptions = mc.options

	fun held(KeyBinding key): Boolean {
		if (mc.screen != null) return false
		int code = key.getKeyCode()
		return code < 0 ? Mouse.isButtonDown(code + 100) : code > 0 && Keyboard.isKeyDown(code)
	}

	fun update() {
		player.forward = held(options.forwardKey)
		player.back = held(options.backKey)
		player.left = held(options.leftKey)
		player.right = held(options.rightKey)
		player.jump = held(options.jumpKey)
		player.sneak = held(options.sneakKey)
		client.copy(player)
		EventManager.call(MovementInputEvent((MovementInput) (Object) this))
	}

	fun resolve(KeyBinding key, boolean pressed): Boolean {
		if (key == options.forwardKey) return resolve(player.forward, client.forward, pressed)
		if (key == options.backKey) return resolve(player.back, client.back, pressed)
		if (key == options.leftKey) return resolve(player.left, client.left, pressed)
		if (key == options.rightKey) return resolve(player.right, client.right, pressed)
		if (key == options.jumpKey) return resolve(player.jump, client.jump, pressed)
		if (key == options.sneakKey) return resolve(player.sneak, client.sneak, pressed)
		return pressed
	}

	private fun resolve(boolean player, boolean client, boolean pressed): Boolean
		get () ? if (player == client) pressed else client
}