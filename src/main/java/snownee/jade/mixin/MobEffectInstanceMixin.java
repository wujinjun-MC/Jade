package snownee.jade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.util.JadeMobEffectInstance;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements JadeMobEffectInstance {
	@Shadow
	public abstract int getDuration();

	@Unique
	private long jade$updateTime;
	@Unique
	private int jade$maxDuration;

	@Override
	public long jade$updateTime() {
		return jade$updateTime;
	}

	@Override
	public void jade$setUpdateTime(long time) {
		this.jade$updateTime = time;
	}

	@Override
	public int jade$maxDuration() {
		return Math.max(jade$maxDuration, getDuration());
	}

	@Inject(method = "tickServer", at = @At("HEAD"))
	private void jade$tickServer(ServerLevel level, LivingEntity entity, Runnable runnable, CallbackInfoReturnable<Boolean> cir) {
		int duration = getDuration();
		if (duration > jade$maxDuration) {
			jade$maxDuration = duration;
		}
	}

	@Inject(method = "onEffectAdded", at = @At("HEAD"))
	private void jade$onEffectAdded(LivingEntity entity, CallbackInfo ci) {
		jade$updateTime = entity.level().getGameTime();
	}

	@WrapMethod(method = "update")
	private boolean jade$update(MobEffectInstance that, Operation<Boolean> original) {
		boolean bl = original.call(that);
		long thatTime = ((JadeMobEffectInstance) that).jade$updateTime();
		if (bl && thatTime > jade$updateTime) {
			jade$updateTime = thatTime;
		}
		return bl;
	}
}
