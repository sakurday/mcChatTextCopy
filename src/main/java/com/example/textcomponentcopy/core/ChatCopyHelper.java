package com.example.textcomponentcopy.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 写剪贴板的唯一出口。
 *
 * <p>走原版的 {@code KeyboardHandler#setClipboard},它内部是
 * {@code ClipboardManager#setClipboard} → {@code GLFW.glfwSetClipboardString}。
 * 这条通道只支持纯字符串,但纯文本和 JSON 都是字符串,所以两种模式共用同一条路,
 * 不需要触碰 AWT / CF_HTML,也就没有跨平台富文本剪贴板的坑。
 */
public final class ChatCopyHelper {
	private ChatCopyHelper() {
	}

	/**
	 * 按指定模式复制一个组件。
	 *
	 * @return 实际写入剪贴板的文本;组件为 null 时返回 null
	 */
	public static String copy(final Component component, final CopyMode mode) {
		if (component == null) {
			return null;
		}

		String text = mode.extract(component);
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
		return text;
	}
}
