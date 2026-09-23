package com.example.textcomponentcopy.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

import com.example.textcomponentcopy.mixin.ChatComponentAccessor;

/**
 * 从模组自身代码读取原版 {@link ChatComponent} 内部消息列表的桥接。
 *
 * <p>为什么需要桥接:{@code ChatComponentAccessor} 是 Mixin 接口,Mixin 在运行时才把
 * 它挂到 {@code ChatComponent} 上;编译期两者没有继承关系,所以不能直接转型引用。
 * 放到独立的类里转型,并且对失败做兜底。
 */
public final class VanillaChatBridge {
	private VanillaChatBridge() {
	}

	/**
	 * 取出原版聊天框现存的消息内容,最新在前。
	 *
	 * <p>任何异常都退化为空列表——这只是"补漏",失败不应该影响界面打开。
	 */
	public static List<Component> currentMessages() {
		try {
			ChatComponent chat = Minecraft.getInstance().gui.getChat();
			List<net.minecraft.client.multiplayer.chat.GuiMessage> messages =
					((ChatComponentAccessor) (Object) chat).allMessages();
			List<Component> result = new ArrayList<>(messages.size());

			for (net.minecraft.client.multiplayer.chat.GuiMessage message : messages) {
				result.add(message.content());
			}

			return result;
		} catch (Throwable ignored) {
			return List.of();
		}
	}
}
