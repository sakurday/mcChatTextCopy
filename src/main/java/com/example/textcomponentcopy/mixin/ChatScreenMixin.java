package com.example.textcomponentcopy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.textcomponentcopy.client.ChatHistoryScreen;

/**
 * 在聊天框右上角加一个进入聊天记录界面的按钮。
 *
 * <p>目标方法名 {@code init} 与 {@code addRenderableWidget} 在源码里都是 protected,
 * 无法从 {@code com.example.textcomponentcopy.mixin} 包直接引用,因此:
 * <ul>
 *   <li>{@code init} 由 {@code textcomponentcopy.accesswidener} 提升为 public;</li>
 *   <li>本类在 mixin 配置里声明了 {@code "mixins": ["net.minecraft.client.gui.screens.Screen"]},
 *       使 {@code addRenderableWidget} 在 Mixin 的"本类层级"检查中可见。</li>
 * </ul>
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	private static final int BUTTON_WIDTH = 64;
	private static final int BUTTON_HEIGHT = 20;
	private static final int MARGIN = 4;

	@Inject(method = "init", at = @At("RETURN"))
	private void textcomponentcopy$addHistoryButton(final CallbackInfo ci) {
		Screen self = (Screen) (Object) this;

		self.addRenderableWidget(
				Button.builder(
						Component.translatable("textcomponentcopy.button.history"),
						button -> Minecraft.getInstance().setScreen(new ChatHistoryScreen())
				)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("textcomponentcopy.button.history.tooltip")))
				.bounds(self.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build()
		);
	}
}
