package com.example.textcomponentcopy.core;

import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;

/**
 * 一条被捕获的聊天记录。
 *
 * <p>这里保存的是原始的 {@link Component},不是渲染后的字形序列——这是本模组能复制
 * "文本组件"而不是"表面文字"的根本原因。原版 {@code ChatComponent} 只保留最近 100 条
 * (MAX_CHAT_HISTORY),所以必须在消息进入时就抓走。
 *
 * @param component 未被裁切的原始组件
 * @param source    消息来源(玩家 / 服务端系统 / 客户端系统)
 * @param timestamp 捕获时的毫秒时间戳
 * @param signature 原版消息签名,服务端撤回消息时可用于对账,可能为 null
 */
public record ChatHistoryEntry(
		Component component,
		GuiMessageSource source,
		long timestamp,
		@Nullable MessageSignature signature
) {
}
