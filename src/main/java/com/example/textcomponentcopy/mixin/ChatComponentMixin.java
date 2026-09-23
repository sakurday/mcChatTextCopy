package com.example.textcomponentcopy.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.textcomponentcopy.core.ChatHistoryStore;

/**
 * 在消息进入原版聊天框的瞬间抓走原始 {@link Component}。
 *
 * <p>注入的是 26.1.2 里三个公开入口({@code addMessage} 本身是 private):
 * {@code addClientSystemMessage} / {@code addServerSystemMessage} / {@code addPlayerMessage}。
 * 这些方法拿到的 {@code Component} 尚未被裁行,是完整组件。
 *
 * <p>为什么不读 {@code GuiMessage.Line}:那里只有 {@code FormattedCharSequence},
 * 是排版后的字形序列,样式与事件结构已经难以还原。
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@Inject(method = "addClientSystemMessage", at = @At("HEAD"))
	private void textcomponentcopy$captureClientSystem(final Component message, final CallbackInfo ci) {
		ChatHistoryStore.get().add(message, GuiMessageSource.SYSTEM_CLIENT, null);
	}

	@Inject(method = "addServerSystemMessage", at = @At("HEAD"))
	private void textcomponentcopy$captureServerSystem(final Component message, final CallbackInfo ci) {
		ChatHistoryStore.get().add(message, GuiMessageSource.SYSTEM_SERVER, null);
	}

	@Inject(method = "addPlayerMessage", at = @At("HEAD"))
	private void textcomponentcopy$capturePlayer(
			final Component message,
			final @Nullable MessageSignature signature,
			final @Nullable GuiMessageTag tag,
			final CallbackInfo ci
	) {
		ChatHistoryStore.get().add(message, GuiMessageSource.PLAYER, signature);
	}
}
