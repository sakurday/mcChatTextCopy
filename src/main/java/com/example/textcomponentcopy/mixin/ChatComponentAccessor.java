package com.example.textcomponentcopy.mixin;

import java.util.List;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取原版聊天组件内部的消息列表。
 *
 * <p>{@code ChatComponent.allMessages} 是私有的,且最多只保留 100 条
 * (源码里的 {@code MAX_CHAT_HISTORY})。我们用它在界面首次打开时补齐一次历史,
 * 真正的抓取靠 {@link ChatComponentMixin} 在消息进入时完成。
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
	@Accessor("allMessages")
	List<GuiMessage> allMessages();
}
