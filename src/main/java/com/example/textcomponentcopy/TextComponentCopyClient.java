package com.example.textcomponentcopy;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端入口。
 *
 * <p>本模组完全运行在客户端:聊天内容的 {@code Component} 在客户端才会被真正解析,
 * 因此不需要服务端配套,也不注册任何网络通道。
 */
public class TextComponentCopyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 目前不需要注册键位或事件,抓取与界面都由 mixin 驱动。
	}
}
