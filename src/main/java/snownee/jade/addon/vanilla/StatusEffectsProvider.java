package snownee.jade.addon.vanilla;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.impl.ui.MobEffectElement;
import snownee.jade.util.JadeMobEffectInstance;

public class StatusEffectsProvider implements StreamServerDataProvider<EntityAccessor, List<StatusEffectsProvider.Effect>> {
	public static final StatusEffectsProvider INSTANCE = new StatusEffectsProvider();

	private static final StreamCodec<RegistryFriendlyByteBuf, List<Effect>> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, Effect>list()
			.apply(Effect.STREAM_CODEC);

	public static MutableComponent getEffectName(MobEffectInstance mobEffectInstance) {
		MutableComponent mutableComponent = mobEffectInstance.getEffect().value().getDisplayName().copy();
		if (mobEffectInstance.getAmplifier() >= 1 && mobEffectInstance.getAmplifier() <= 9) {
			mutableComponent.append(CommonComponents.SPACE).append(Component.translatable(
					"enchantment.level." + (mobEffectInstance.getAmplifier() + 1)));
		}
		return mutableComponent;
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		return accessor.getEntity() instanceof LivingEntity;
	}

	@Override
	@Nullable
	public List<Effect> streamData(EntityAccessor accessor) {
		List<Effect> effects = ((LivingEntity) accessor.getEntity()).getActiveEffects()
				.stream()
				.filter(MobEffectInstance::isVisible)
				.map(Effect::new)
				.toList();
		return effects.isEmpty() ? null : effects;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, List<Effect>> streamCodec() {
		return STREAM_CODEC;
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_POTION_EFFECTS;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			List<Effect> effects = StatusEffectsProvider.INSTANCE.decodeFromData(accessor).orElse(List.of());
			if (effects.isEmpty()) {
				return;
			}
			ITooltip box = JadeUI.tooltip();
			for (var effect : effects.stream().sorted().map(Effect::effect).toList()) {
				MobEffectElement element = MobEffectElement.withDuration(effect, accessor.tickRate());
				box.append(element);
			}

/*			for (var effect : effects) {
				Component name = getEffectName(effect);
				String duration;
				if (effect.isInfiniteDuration()) {
					duration = MobEffectElement.INFINITE.getString();
				} else {
					duration = StringUtil.formatTickDuration(effect.getDuration(), accessor.tickRate());
				}
				MutableComponent s = Component.translatable("jade.potion", name, duration);
				IThemeHelper t = IThemeHelper.get();
				box.add(effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? t.danger(s) : t.success(s));
			}*/
			tooltip.add(JadeUI.box(box, BoxStyle.nestedBox()).flexGrow(1));
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_POTION_EFFECTS;
		}
	}

	public record Effect(MobEffectInstance effect, long updateTime) implements Comparable<Effect> {
		public static final StreamCodec<RegistryFriendlyByteBuf, Effect> STREAM_CODEC = StreamCodec.composite(
				MobEffectInstance.STREAM_CODEC,
				Effect::effect,
				ByteBufCodecs.LONG,
				Effect::updateTime,
				Effect::new);

		public Effect(MobEffectInstance effect) {
			this(effect, ((JadeMobEffectInstance) effect).jade$updateTime());
		}

		@Override
		public int compareTo(StatusEffectsProvider.Effect o) {
			int compared = Long.compare(updateTime, o.updateTime);
			if (compared != 0) {
				return -compared;
			}
			return effect.compareTo(o.effect);
		}
	}
}
