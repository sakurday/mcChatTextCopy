package com.example.textcomponentcopy.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * 导出器:把 {@link Component} 变成可放进剪贴板的字符串。
 *
 * <p>两种模式:
 * <ul>
 *   <li>{@link #extractPlainText} —— 只取可见文字,样式、点击事件、translatable 结构全部丢弃;</li>
 *   <li>{@link #extractJson} —— 用原版 codec 序列化成 JSON,保留颜色/加粗/点击事件/悬浮事件等全部信息。</li>
 * </ul>
 *
 * <p>注意:26.1.2 起组件体系是 NBT 优先的,{@code ComponentSerialization.CODEC} 在
 * {@link JsonOps} 下对含 NBT 的内容(如 {@code nbt} 类型组件)会有损,普通聊天消息不受影响。
 * 若以后要无损导出,应改用 {@code NbtOps} 输出 SNBT。
 */
public final class ComponentTextExtractor {
	private ComponentTextExtractor() {
	}

	/**
	 * 提取纯文本。{@link Component#getString()} 返回的是所有 literal 内容拼接,
	 * 不含任何样式与事件。
	 */
	public static String extractPlainText(final Component component) {
		return component == null ? "" : component.getString();
	}

	/**
	 * 序列化为 JSON 文本。
	 *
	 * <p>序列化失败时退回纯文本,保证复制按钮永远有东西可复制。
	 */
	public static String extractJson(final Component component) {
		if (component == null) {
			return "";
		}

		DataResult<JsonElement> result = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component);
		return result.result()
				.map(JsonElement::toString)
				.orElseGet(() -> component.getString());
	}
}
