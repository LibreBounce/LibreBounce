package net.ccbluex.liquidbounce.utils.input

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

// Credits to Illusion Client for this!
object InputManager : MinecraftInstance {
	val player: InputState = InputState()
	val client: InputState = InputState()
	val options: GameSettings = mc.options

	fun held(KeyBinding key): Boolean {
		if (mc.screen != null) return false
		int code = key.getKeyCode()
		return if (code < 0) Mouse.isButtonDown(code + 100) else code > 0 && Keyboard.isKeyDown(code)
	}

	fun update() {
		player.forward = held(options.keyBindForward)
		player.back = held(options.keyBindBack)
		player.left = held(options.keyBindLeft)
		player.right = held(options.keyBindRight)
		player.jump = held(options.keyBindJump)
		player.sneak = held(options.keyBindSneak)
		client.copy(player)
		EventManager.call(MovementInputEvent)
	}

	fun resolve(KeyBinding key, boolean pressed): Boolean {
		if (key == options.keyBindForward) return resolve(player.forward, client.forward, pressed)
		if (key == options.keyBindBack) return resolve(player.back, client.back, pressed)
		if (key == options.keyBindLeft) return resolve(player.left, client.left, pressed)
		if (key == options.keyBindRight) return resolve(player.right, client.right, pressed)
		if (key == options.keyBindJump) return resolve(player.jump, client.jump, pressed)
		if (key == options.keyBindSneak) return resolve(player.sneak, client.sneak, pressed)
		return pressed
	}

	private fun resolve(boolean player, boolean client, boolean pressed): Boolean
		get() = if (player == client) pressed else client
}