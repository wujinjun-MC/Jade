package snownee.jade.impl.ui;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import snownee.jade.addon.vanilla.StatusEffectsProvider;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.overlay.DisplayHelper;

public class MobEffectElement extends Element {
	public static final Component INFINITE = Component.translatable("effect.duration.infinite");
	private final Holder<MobEffect> effect;
	private final @Nullable FormattedText text;
	private final int textWidth;
	private final @Nullable Supplier<Component> narration;

	public static MobEffectElement withDuration(MobEffectInstance effectInstance, float tickRate) {
		Component seconds;
		if (effectInstance.getEffect().value().isInstantenous()) {
			seconds = null;
		} else if (effectInstance.isInfiniteDuration()) {
			seconds = colorize(INFINITE.copy(), effectInstance);
		} else {
			int ticks = effectInstance.getDuration();
			seconds = colorize(IThemeHelper.get().seconds(ticks, tickRate, true), effectInstance);
		}
		return new MobEffectElement(
				effectInstance.getEffect(), seconds, () -> {
			MutableComponent name = StatusEffectsProvider.getEffectName(effectInstance);
			if (seconds == null) {
				return name;
			}
			return name.append(CommonComponents.SPACE).append(seconds);
		});
	}

	public static Component colorize(MutableComponent text, MobEffectInstance effect) {
		IThemeHelper helper = IThemeHelper.get();
		return switch (effect.getEffect().value().getCategory()) {
			case BENEFICIAL -> helper.success(text);
			case HARMFUL -> helper.danger(text);
			case NEUTRAL -> text;
		};
	}

	public MobEffectElement(Holder<MobEffect> effect, @Nullable FormattedText text, @Nullable Supplier<Component> narration) {
		this.effect = effect;
		this.text = text;
		this.narration = narration;
		textWidth = text == null ? 0 : Math.max(DisplayHelper.font().width(text), 0);
		width = height = 18;
	}

	@Override
	public @Nullable Component getNarration() {
		return narration == null ? null : narration.get();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		IDisplayHelper.get().blitSprite(graphics, RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect), getX(), getY(), 18, 18);
		if (text != null) {
			int left = getX() + 18 - textWidth;
			int top = getY() + 20 - DisplayHelper.font().lineHeight;
			IDisplayHelper.get().drawText(graphics, text, left, top, IThemeHelper.get().getNormalColor());
		}
	}
}
