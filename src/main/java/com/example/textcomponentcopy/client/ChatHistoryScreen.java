package com.example.textcomponentcopy.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import com.example.textcomponentcopy.core.ChatCopyHelper;
import com.example.textcomponentcopy.core.ChatHistoryEntry;
import com.example.textcomponentcopy.core.ChatHistoryStore;
import com.example.textcomponentcopy.core.CopyMode;
import com.example.textcomponentcopy.core.VanillaChatBridge;

/**
 * 聊天记录界面:上方一个格式开关,下面每条消息左侧一个复制按钮。
 *
 * <p>注意 26.1.2 的渲染模型:原版已移除 {@code GuiGraphics},屏幕改为"提取渲染状态"——
 * 覆写 {@link #extractRenderState} 而不是 {@code render},绘制通过
 * {@link GuiGraphicsExtractor} 完成。
 *
 * <p>另一个 26.1.2 的细节:输入事件是记录类({@link MouseButtonEvent}),不再是裸的
 * {@code (double mouseX, double mouseY, int button)}。
 */
public class ChatHistoryScreen extends Screen {
	private static final int BG_COLOR = 0xC0000000;
	private static final int HEADER_COLOR = 0x40000000;
	private static final int LIST_BG_COLOR = 0x20000000;
	private static final int ROW_ALT_COLOR = 0x14FFFFFF;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int STATUS_COLOR = 0xFF55FF55;
	private static final int MUTED_COLOR = 0xFFA0A0A0;

	private static final int ROW_HEIGHT = 18;
	private static final int COPY_BUTTON_WIDTH = 46;
	private static final int COPY_BUTTON_HEIGHT = 16;
	private static final int HEADER_HEIGHT = 44;
	private static final int OUTER_MARGIN = 12;
	private static final int CONTENT_MARGIN = 6;
	private static final int TIME_COLUMN_WIDTH = 40;

	/** 每一行对应的复制按钮,下标与聊天记录下标一一对应。 */
	private final List<Button> copyButtons = new ArrayList<>();

	private CopyMode mode = CopyMode.PLAIN;
	private Button modeButton;
	private int scrollOffset;

	/** 复制结果提示与过期时间。 */
	private String statusMessage = "";
	private long statusUntil;

	public ChatHistoryScreen() {
		super(Component.translatable("textcomponentcopy.screen.title"));
	}

	@Override
	protected void init() {
		// 先把原版聊天框里仅存的 100 条补进我们自己的缓冲(整个会话只做一次)。
		ChatHistoryStore.get().seedFromVanilla(VanillaChatBridge.currentMessages());

		this.scrollOffset = 0;
		this.copyButtons.clear();

		this.modeButton = this.addRenderableWidget(
				Button.builder(this.mode.buttonLabel(), button -> {
					this.mode = this.mode.next();
					button.setMessage(this.mode.buttonLabel());
				}).bounds(this.width - 100 - OUTER_MARGIN, 8, 100, 20).build()
		);

		this.addRenderableWidget(
				Button.builder(Component.translatable("gui.done"), button -> this.onClose())
						.bounds(this.width / 2 - 50, this.height - 28, 100, 20).build()
		);

		int listTop = HEADER_HEIGHT;
		List<ChatHistoryEntry> rows = this.rows();

		for (int i = 0; i < rows.size(); i++) {
			int index = i;
			Button copy = Button.builder(Component.translatable("textcomponentcopy.button.copy"), button -> this.copy(index))
					.bounds(OUTER_MARGIN + CONTENT_MARGIN, listTop + i * ROW_HEIGHT, COPY_BUTTON_WIDTH, COPY_BUTTON_HEIGHT)
					.build();
			this.copyButtons.add(this.addRenderableWidget(copy));
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		this.drawChrome(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		// 点击行的空白处也能复制该行。
		int index = this.rowIndexAt((int) event.x(), (int) event.y());
		if (index >= 0) {
			this.copy(index);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		int maxScroll = this.maxScroll();

		if (maxScroll <= 0) {
			return false;
		}

		int delta = scrollY > 0 ? -1 : 1;
		this.scrollOffset = Math.clamp(this.scrollOffset + delta, 0, maxScroll);
		return true;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(null);
	}

	// ------------------------------------------------------------------
	// 绘制
	// ------------------------------------------------------------------

	private void drawChrome(final GuiGraphicsExtractor graphics) {
		int right = this.width - OUTER_MARGIN;
		int rows = this.rows().size();

		graphics.fill(0, 0, this.width, this.height, BG_COLOR);
		graphics.fill(0, 0, this.width, HEADER_HEIGHT, HEADER_COLOR);

		graphics.text(this.font, Component.translatable("textcomponentcopy.screen.header", rows, ChatHistoryStore.CAPACITY), OUTER_MARGIN, 10, TEXT_COLOR);

		Component hint = Component.translatable("textcomponentcopy.screen.hint", this.mode.label());
		graphics.text(this.font, hint, OUTER_MARGIN, 22, MUTED_COLOR);

		if (!this.statusMessage.isEmpty() && System.currentTimeMillis() < this.statusUntil) {
			graphics.text(this.font, this.statusMessage, OUTER_MARGIN + this.font.width(hint) + 8, 22, STATUS_COLOR);
		}

		this.drawRows(graphics, right);
	}

	private void drawRows(final GuiGraphicsExtractor graphics, final int right) {
		List<ChatHistoryEntry> rows = this.rows();
		int listTop = HEADER_HEIGHT;
		int listBottom = this.listBottom();

		graphics.fill(OUTER_MARGIN, listTop, right, listBottom, LIST_BG_COLOR);

		if (rows.isEmpty()) {
			graphics.text(this.font, Component.translatable("textcomponentcopy.screen.empty"), OUTER_MARGIN + CONTENT_MARGIN, listTop + 6, MUTED_COLOR);
			return;
		}

		int visible = this.visibleRows();
		int syncIndex = 0;

		graphics.enableScissor(OUTER_MARGIN, listTop, right, listBottom);

		for (int row = 0; row < visible; row++) {
			int storeIndex = this.scrollOffset + row;

			if (storeIndex >= rows.size()) {
				break;
			}

			ChatHistoryEntry entry = rows.get(storeIndex);
			int y = listTop + row * ROW_HEIGHT;

			if (row % 2 == 1) {
				graphics.fill(OUTER_MARGIN, y, right, y + ROW_HEIGHT, ROW_ALT_COLOR);
			}

			// 左侧来源色条
			graphics.fill(OUTER_MARGIN, y + 1, OUTER_MARGIN + 2, y + ROW_HEIGHT - 1, sourceColor(entry.source()));

			int textLeft = OUTER_MARGIN + CONTENT_MARGIN + COPY_BUTTON_WIDTH;
			int textRight = right - 4;
			int available = Math.max(16, textRight - textLeft - TIME_COLUMN_WIDTH - 4);

			String prefix = "[" + sourceTag(entry.source()) + "] ";
			String plain = entry.component().getString().replace('\n', ' ');
			String body = this.font.plainSubstrByWidth(prefix + plain, available);

			graphics.text(this.font, Component.literal(body), textLeft, y + 5, TEXT_COLOR);
			graphics.text(this.font, this.formatAge(entry.timestamp()), textRight - TIME_COLUMN_WIDTH, y + 5, MUTED_COLOR);

			// 把该行的复制按钮挪到当前可见位置
			if (syncIndex < this.copyButtons.size()) {
				Button button = this.copyButtons.get(syncIndex++);
				button.setX(OUTER_MARGIN + CONTENT_MARGIN);
				button.setY(y + 1);
				button.visible = true;
			}
		}

		graphics.disableScissor();

		// 滚动到列表外的按钮要隐藏,否则会停在旧位置
		for (int i = syncIndex; i < this.copyButtons.size(); i++) {
			this.copyButtons.get(i).visible = false;
		}
	}

	// ------------------------------------------------------------------
	// 行为
	// ------------------------------------------------------------------

	private void copy(final int storeIndex) {
		List<ChatHistoryEntry> rows = this.rows();

		// 界面打开期间可能还有新消息到达,行数会多于已建按钮的数量;
		// 这种情况下先补按钮,避免"点了行却复制不到"。
		while (this.copyButtons.size() <= storeIndex && this.copyButtons.size() < rows.size()) {
			int index = this.copyButtons.size();
			Button copy = Button.builder(Component.translatable("textcomponentcopy.button.copy"), button -> this.copy(index))
					.bounds(OUTER_MARGIN + CONTENT_MARGIN, HEADER_HEIGHT + index * ROW_HEIGHT, COPY_BUTTON_WIDTH, COPY_BUTTON_HEIGHT)
					.build();
			this.copyButtons.add(this.addRenderableWidget(copy));
		}

		if (storeIndex < 0 || storeIndex >= rows.size()) {
			return;
		}

		ChatHistoryEntry entry = rows.get(storeIndex);
		String copied = ChatCopyHelper.copy(entry.component(), this.mode);

		if (copied != null) {
			this.statusMessage = Component.translatable("textcomponentcopy.status.copied", copied.length(), this.mode.label()).getString();
			this.statusUntil = System.currentTimeMillis() + 3000L;
		}
	}

	private List<ChatHistoryEntry> rows() {
		return ChatHistoryStore.get().entries();
	}

	private int listBottom() {
		return this.height - 36;
	}

	private int visibleRows() {
		return Math.max(1, (this.listBottom() - HEADER_HEIGHT) / ROW_HEIGHT);
	}

	private int maxScroll() {
		return Math.max(0, this.rows().size() - this.visibleRows());
	}

	/** 返回点击位置对应的记录下标,不在任何行上则返回 -1。 */
	private int rowIndexAt(final int mouseX, final int mouseY) {
		int listTop = HEADER_HEIGHT;

		if (mouseX < OUTER_MARGIN || mouseX > this.width - OUTER_MARGIN || mouseY < listTop || mouseY >= this.listBottom()) {
			return -1;
		}

		int row = (mouseY - listTop) / ROW_HEIGHT;
		int index = this.scrollOffset + row;

		if (row >= this.visibleRows() || index >= this.rows().size()) {
			return -1;
		}

		return index;
	}

	private static int sourceColor(final GuiMessageSource source) {
		return switch (source) {
			case PLAYER -> ARGB.color(255, 85, 255, 85);
			case SYSTEM_SERVER -> ARGB.color(255, 255, 255, 85);
			case SYSTEM_CLIENT -> ARGB.color(255, 170, 170, 170);
		};
	}

	private static String sourceTag(final GuiMessageSource source) {
		return switch (source) {
			case PLAYER -> "P";
			case SYSTEM_SERVER -> "S";
			case SYSTEM_CLIENT -> "C";
		};
	}

	/** 相对时间,例如 12s / 3m / 2h。 */
	private String formatAge(final long timestamp) {
		long seconds = Math.max(0L, (System.currentTimeMillis() - timestamp) / 1000L);

		if (seconds < 60L) {
			return seconds + "s";
		}

		long minutes = seconds / 60L;
		return minutes < 60L ? minutes + "m" : (minutes / 60L) + "h";
	}
}
