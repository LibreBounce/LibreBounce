/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.combat.TickBase;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.features.module.modules.exploit.MultiActions;
import net.ccbluex.liquidbounce.features.module.modules.world.FastPlace;
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration;
import net.ccbluex.liquidbounce.injection.forge.SplashProgressLock;
import net.ccbluex.liquidbounce.ui.client.TitleScreen;
import net.ccbluex.liquidbounce.utils.attack.CPSCounter;
import net.ccbluex.liquidbounce.utils.client.ClientUtils;
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar;
import net.ccbluex.liquidbounce.utils.io.MiscUtils;
import net.ccbluex.liquidbounce.utils.render.IconUtils;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Window;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HitResult;
import net.minecraft.util.Util;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

@Mixin(Minecraft.class)
@SideOnly(Side.CLIENT)
public abstract class MixinMinecraft {

    @Shadow
    public Screen currentScreen;

    @Shadow
    public boolean skipRenderWorld;

    @Shadow
    private int leftClickCounter;

    @Shadow
    public HitResult objectMouseOver;

    @Shadow
    public WorldClient theWorld;

    @Shadow
    public LocalClientPlayerEntity player;

    @Shadow
    public ClientPlayerInteractionManager playerController;

    @Shadow
    public int displayWidth;

    @Shadow
    public int displayHeight;

    @Shadow
    public int rightClickDelayTimer;

    @Shadow
    public GameOptions gameOptions;

    @Shadow
    public abstract void displayScreen(Screen guiScreenIn);

    @Unique
    private Future<?> liquidBounce$preloadFuture;

    @Inject(method = "run", at = @At("HEAD"))
    private void init(CallbackInfo callbackInfo) {
        if (displayWidth < 1067) displayWidth = 1067;

        if (displayHeight < 622) displayHeight = 622;

        liquidBounce$preloadFuture = LiquidBounce.INSTANCE.preload();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 1))
    private void hook(CallbackInfo ci) {
        EventManager.INSTANCE.call(GameLoopEvent.INSTANCE);
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void startGame(CallbackInfo callbackInfo) throws ExecutionException, InterruptedException {
        liquidBounce$preloadFuture.get();

        LiquidBounce.INSTANCE.startClient();
    }

    @Inject(method = "startGame", at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/TextureManager"))
    private void waitForLock(CallbackInfo ci) {
        long end = System.currentTimeMillis() + 20000;

        while (end < System.currentTimeMillis() && SplashProgressLock.INSTANCE.isAnimationRunning()) {
            synchronized (SplashProgressLock.INSTANCE) {
                try {
                    SplashProgressLock.INSTANCE.wait(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void createDisplay(CallbackInfo callbackInfo) {
        if (ClientConfiguration.INSTANCE.getClientTitle()) {
            Display.setTitle(LiquidBounce.INSTANCE.getClientTitle());
        }
    }

    @Inject(method = "displayScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/Screen;", shift = At.Shift.AFTER))
    private void handleDisplayScreen(CallbackInfo callbackInfo) {
        if (currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModTitleScreen"))) {
            currentScreen = new TitleScreen();

            Window scaledResolution = new Window(mc);
            currentScreen.setWorldAndResolution(mc, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
            skipRenderWorld = false;
        }

        EventManager.INSTANCE.call(new ScreenEvent(currentScreen));
    }

    @Unique
    private long lastFrame = getTime();

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(final CallbackInfo callbackInfo) {
        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;

        RenderUtils.INSTANCE.setDeltaTime(deltaTime);
    }

    @Unique
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void injectGameRuntimeTicks(CallbackInfo ci) {
        ClientUtils.INSTANCE.setRunTimeTicks(ClientUtils.INSTANCE.getRunTimeTicks() + 1);
        SilentHotbar.INSTANCE.updateSilentSlot();
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void injectEndTickEvent(CallbackInfo ci) {
        EventManager.INSTANCE.call(TickEndEvent.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", ordinal = 0))
    private void onTick(final CallbackInfo callbackInfo) {
        EventManager.INSTANCE.call(GameTickEvent.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = At.Shift.AFTER))
    private void onKey(CallbackInfo callbackInfo) {
        if (Keyboard.getEventKeyState() && currentScreen == null)
            EventManager.INSTANCE.call(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
    }

    @Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/HitResult;getBlockPos()Lnet/minecraft/util/BlockPos;"))
    private void onClickBlock(CallbackInfo callbackInfo) {
        final BlockPos blockPos = objectMouseOver.getBlockPos();
        if (leftClickCounter == 0 && theWorld.getBlockState(blockPos).getBlock().getMaterial() != Material.air) {
            EventManager.INSTANCE.call(new ClickBlockEvent(blockPos, objectMouseOver.sideHit));
        }
    }

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void setWindowIcon(CallbackInfo callbackInfo) {
        if (Util.getOSType() != Util.EnumOS.OSX) {
            if (ClientConfiguration.INSTANCE.getClientTitle()) {
                if (IconUtils.initLwjglIcon()) {
                    callbackInfo.cancel();
                }
            }
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo callbackInfo) {
        LiquidBounce.INSTANCE.stopClient();
    }

    @Inject(method = "displayCrashReport", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;instance()Lnet/minecraftforge/fml/common/FMLCommonHandler;"))
    private void injectDisplayCrashReport(CrashReport crashReport, CallbackInfo callbackInfo) {
        MiscUtils.showErrorPopup(crashReport.getCrashCause(), "Game crashed! ", MiscUtils.generateCrashInfo());
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void clickMouse(CallbackInfo callbackInfo) {
        if (AutoClicker.INSTANCE.handleEvents()) {
            leftClickCounter = 0;
        }

        if (leftClickCounter <= 0) {
            CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.LEFT);
        }
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"))
    private void middleClickMouse(CallbackInfo ci) {
        CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.MIDDLE);
    }

    @Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = At.Shift.AFTER))
    private void rightClickMouse(final CallbackInfo callbackInfo) {
        CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.RIGHT);

        final FastPlace fastPlace = FastPlace.INSTANCE;
        if (!fastPlace.handleEvents()) return;

        // Don't spam-click when the player isn't holding blocks
        if (fastPlace.getOnlyBlocks() && (player.getDisplayItemInHand() == null || !(player.getDisplayItemInHand().getItem() instanceof BlockItem)))
            return;

        if (objectMouseOver != null && objectMouseOver.typeOfHit == HitResult.Type.BLOCK) {
            BlockPos blockPos = objectMouseOver.getBlockPos();
            BlockState blockState = theWorld.getBlockState(blockPos);
            // Don't spam-click when interacting with a TileEntity (chests, ...)
            // Doesn't prevent spam-clicking anvils, crafting tables, ... (couldn't figure out a non-hacky way)
            if (blockState.getBlock().hasTileEntity(blockState)) return;
            // Return if not facing a block
        } else if (fastPlace.getFacingBlocks()) return;

        rightClickDelayTimer = fastPlace.getSpeed();
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient p_loadWorld_1_, String p_loadWorld_2_, final CallbackInfo callbackInfo) {
        if (theWorld != null) {
            MiniMapRegister.INSTANCE.unloadAllChunks();
        }

        EventManager.INSTANCE.call(new WorldEvent(p_loadWorld_1_));
    }


    @Redirect(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/LocalClientPlayerEntity;isUsingItem()Z"))
    private boolean injectMultiActions(LocalClientPlayerEntity instance) {
        ItemStack itemStack = instance.itemInUse;

        if (MultiActions.INSTANCE.handleEvents()) itemStack = null;

        return itemStack != null;
    }

    @Redirect(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;resetBlockRemoving()V"))
    private void injectAbortBreaking(ClientPlayerInteractionManager instance) {
        if (!AbortBreaking.INSTANCE.handleEvents()) {
            instance.resetBlockRemoving();
        }
    }

    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean injectTickBase(Queue instance) {
        return TickBase.INSTANCE.getDuringTickModification() || instance.isEmpty();
    }

    @Redirect(method = {"middleClickMouse", "rightClickMouse"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/player/PlayerInventory;currentItem:I"))
    private int injectSilentHotbar(PlayerInventory instance) {
        return SilentHotbar.INSTANCE.getCurrentSlot();
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/LocalClientPlayerEntity;inventory:Lnet/minecraft/entity/living/player/PlayerInventory;"))
    private void injectSilentHotbarManualPressDetection(CallbackInfo ci) {
        SilentHotbar.INSTANCE.setPressedAtSlot(true);
    }

    /**
     * @author CCBlueX
     */
    @ModifyConstant(method = "getLimitFramerate", constant = @Constant(intValue = 30))
    public int getLimitFramerate(int constant) {
        return 60;
    }
}
