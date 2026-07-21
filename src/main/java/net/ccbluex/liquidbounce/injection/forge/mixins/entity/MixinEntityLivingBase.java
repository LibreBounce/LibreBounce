/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.LiquidWalk;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.Sprint;
import net.ccbluex.liquidbounce.features.module.modules.render.Animations;
import net.ccbluex.liquidbounce.features.module.modules.render.Rotations;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Tower;
import net.ccbluex.liquidbounce.utils.movement.MovementUtils;
import net.ccbluex.liquidbounce.utils.rotation.Rotation;
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings;
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils;
import net.ccbluex.liquidbounce.utils.extensions.MathExtensionsKt;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.entity.living.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow
    public float headYaw;
    @Shadow
    public boolean jumping;
    @Shadow
    public int jumpingCooldown;

    @Shadow
    protected abstract float getJumpUpwardsMotion();

    @Shadow
    public abstract StatusEffectInstance getActivePotionEffect(Potion potionIn);

    @Shadow
    public abstract boolean hasStatusEffect(Potion potionIn);

    @Shadow
    public void onLivingUpdate() {
    }

    @Shadow
    protected abstract void checkFallDamage(double y, boolean onGroundIn, Block blockIn, BlockPos pos);

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract ItemStack getDisplayItemInHand();

    @Shadow
    protected abstract void jumpInWater();

    /**
     * @author CCBlueX
     */
    @Overwrite
    protected void jump() {
        final JumpEvent prejumpEvent = new JumpEvent(getJumpUpwardsMotion(), EventState.PRE);
        if ((Object) this == Minecraft.getMinecraft().player) {
            EventManager.INSTANCE.call(prejumpEvent);
            if (prejumpEvent.isCancelled()) return;
        }

        motionY = prejumpEvent.getMotion();

        if (hasStatusEffect(Potion.jump))
            motionY += (float) (getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;

        if (isSprinting()) {
            float fixedYaw = this.rotationYaw;

            final RotationUtils rotationUtils = RotationUtils.INSTANCE;
            final Rotation currentRotation = rotationUtils.getCurrentRotation();
            final RotationSettings rotationData = rotationUtils.getActiveSettings();
            if (currentRotation != null && rotationData != null && rotationData.getStrafe()) {
                fixedYaw = currentRotation.getYaw();
            }

            final Sprint sprint = Sprint.INSTANCE;
            if (sprint.handleEvents() && sprint.getMode().equals("Vanilla") && sprint.getAllDirections() && sprint.getJumpDirections()) {
                fixedYaw += MathExtensionsKt.toDegreesF(MovementUtils.INSTANCE.getDirection()) - this.rotationYaw;
            }

            final float f = fixedYaw * 0.017453292F;
            motionX -= MathHelper.sin(f) * 0.2F;
            motionZ += MathHelper.cos(f) * 0.2F;
        }

        isAirBorne = true;

        if ((Object) this == Minecraft.getMinecraft().player) {
            final JumpEvent postjumpEvent = new JumpEvent((float) motionY, EventState.POST);
            EventManager.INSTANCE.call(postjumpEvent);
        }
    }

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void headLiving(CallbackInfo callbackInfo) {
        if (NoJumpDelay.INSTANCE.handleEvents() || Scaffold.INSTANCE.handleEvents() && Tower.INSTANCE.getTowerModeValues().equals("Pulldown")) jumpingCooldown = 0;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;jumping:Z", ordinal = 1))
    private void onJumpSection(CallbackInfo callbackInfo) {
        final LiquidWalk liquidWalk = LiquidWalk.INSTANCE;

        if (liquidWalk.handleEvents() && !jumping && !isSneaking() && inWater() && liquidWalk.getMode().equals("Swim")) {
            jumpInWater();
        }
    }

    @Inject(method = "getRotationVec", at = @At("HEAD"), cancellable = true)
    private void getRotationVec(CallbackInfoReturnable<Vec3d> callbackInfoReturnable) {
        //noinspection ConstantConditions
        if (((LivingEntity) (Object) this) instanceof LocalClientPlayerEntity)
            callbackInfoReturnable.setReturnValue(getRotationVector(rotationPitch, rotationYaw));
    }

    /**
     * Inject head yaw rotation modification
     */
    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateEntityActionState()V", shift = At.Shift.AFTER))
    private void hookHeadRotations(CallbackInfo ci) {
        Rotation rotation = Rotations.INSTANCE.getRotation();

        //noinspection ConstantValue
        this.headYaw = ((LivingEntity) (Object) this) instanceof LocalClientPlayerEntity && Rotations.INSTANCE.shouldUseRealisticMode() && rotation != null ? rotation.getYaw() : this.headYaw;
    }

    /**
     * Inject body rotation modification
     */
    @Redirect(method = "onUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;rotationYaw:F", ordinal = 0))
    private float hookBodyRotationsA(LivingEntity instance) {
        Rotation rotation = Rotations.INSTANCE.getRotation();

        return instance instanceof LocalClientPlayerEntity && Rotations.INSTANCE.shouldUseRealisticMode() && rotation != null ? rotation.getYaw() : instance.rotationYaw;
    }

    /**
     * Inject body rotation modification
     */
    @Redirect(method = "updateDistance", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;rotationYaw:F"))
    private float hookBodyRotationsB(LivingEntity instance) {
        Rotation rotation = Rotations.INSTANCE.getRotation();

        return instance instanceof LocalClientPlayerEntity && Rotations.INSTANCE.shouldUseRealisticMode() && rotation != null ? rotation.getYaw() : instance.rotationYaw;
    }

    /**
     * @author SuperSkidder
     * @reason Animations swing speed
     */
    @ModifyConstant(method = "getArmSwingAnimationEnd", constant = @Constant(intValue = 6))
    private int injectAnimationsModule(int constant) {
        Animations module = Animations.INSTANCE;
        int swingSpeed = module.getNormalizeSwingSpeed() ? (int) (module.getSwingSpeed() / Minecraft.getMinecraft().timer.timerSpeed) : module.getSwingSpeed();

        return module.handleEvents() ? (22 - swingSpeed) : constant;
    }
}
