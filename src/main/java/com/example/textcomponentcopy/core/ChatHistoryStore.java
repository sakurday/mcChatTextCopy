package com.example.textcomponentcopy.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;

/**
 * 模组自己的聊天环形缓冲。
 *
 * <p>用 {@link ArrayList} + 手动裁剪而不是 {@code ArrayDeque},因为我们只从尾部裁剪、
 * 只在头部插入,并且需要按下标随机访问。
 *
 * <p>线程约定:所有方法都必须在客户端主线程调用。聊天消息的接收本来就在主线程,
 * 界面也在主线程,因此不需要额外同步。
 */
public final class ChatHistoryStore {
	/** 缓冲上限,远大于原版的 100 条。 */
	public static final int CAPACITY = 1000;

	private static final ChatHistoryStore INSTANCE = new ChatHistoryStore();

	/** 最新的消息在 index 0。 */
	private final List<ChatHistoryEntry> entries = new ArrayList<>();

	/** 是否已经从原版聊天框导入过历史,避免每次打开界面都重复导入。 */
	private boolean seededFromVanilla;

	private ChatHistoryStore() {
	}

	public static ChatHistoryStore get() {
		return INSTANCE;
	}

	/**
	 * 记录一条新消息。
	 *
	 * @param component 原始组件,为 null 时忽略
	 */
	public void add(@Nullable Component component, final GuiMessageSource source, final @Nullable MessageSignature signature) {
		if (component == null) {
			return;
		}

		this.entries.add(0, new ChatHistoryEntry(component, source, System.currentTimeMillis(), signature));
		this.trim();
	}

	/**
	 * 把原版聊天框里现存的消息导入一次。
	 *
	 * <p>用途:玩家在装有本模组之前(或刚进世界时)已经收到过一些消息,开界面时先补进来。
	 * 原版最多只有 100 条,所以这只是补漏,不能代替 {@link #add}。
	 */
	public void seedFromVanilla(final List<? extends Component> vanillaMessages) {
		if (this.seededFromVanilla) {
			return;
		}

		this.seededFromVanilla = true;
		if (vanillaMessages == null || vanillaMessages.isEmpty()) {
			return;
		}

		// 原版列表是"最新在前",从后往前插到最前面即可保持时间顺序。
		for (int i = vanillaMessages.size() - 1; i >= 0; i--) {
			Component component = vanillaMessages.get(i);
			if (component != null) {
				this.entries.add(0, new ChatHistoryEntry(component, GuiMessageSource.SYSTEM_CLIENT, System.currentTimeMillis(), null));
			}
		}

		this.trim();
	}

	/** 最新在前的只读视图。 */
	public List<ChatHistoryEntry> entries() {
		return Collections.unmodifiableList(this.entries);
	}

	public int size() {
		return this.entries.size();
	}

	public boolean isEmpty() {
		return this.entries.isEmpty();
	}

	/** 切换世界或断线时清空,避免把上一个服务器的聊天带到下一个。 */
	public void clear() {
		this.entries.clear();
		this.seededFromVanilla = false;
	}

	private void trim() {
		while (this.entries.size() > CAPACITY) {
			this.entries.remove(this.entries.size() - 1);
		}
	}
}
