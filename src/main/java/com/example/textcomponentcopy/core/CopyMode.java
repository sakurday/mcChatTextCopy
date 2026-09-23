package com.example.textcomponentcopy.core;

import net.minecraft.network.chat.Component;

/**
 * 复制格式。界面上方的开关就是在两个值之间切换。
 */
public enum CopyMode {
	/** 只复制可见文字。 */
	PLAIN("textcomponentcopy.mode.plain"),
	/** 复制文本组件的 JSON,保留颜色/样式/事件。 */
	JSON("textcomponentcopy.mode.json");

	private final String translationKey;

	CopyMode(final String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public Component label() {
		return Component.translatable(this.translationKey);
	}

	/** 切换按钮上显示的文案,例如 "复制格式: 纯文本"。 */
	public Component buttonLabel() {
		return Component.translatable("textcomponentcopy.button.mode", this.label());
	}

	public CopyMode next() {
		CopyMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	/** 按当前模式导出可粘贴的文本。 */
	public String extract(final Component component) {
		return this == JSON
				? ComponentTextExtractor.extractJson(component)
				: ComponentTextExtractor.extractPlainText(component);
	}
}
