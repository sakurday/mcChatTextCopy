# Text Component Copy

Minecraft **26.1.2** / Fabric 客户端模组。打开聊天框后右上角会出现一个「聊天记录」按钮,点进去可以看到已捕获的聊天记录,
每一行左侧有独立的「复制」按钮,界面顶部有一个开关,决定复制的是**纯文本**还是**文本组件 JSON**。

本模组是纯客户端模组,不需要服务端安装,也不注册任何网络通道。

## 为什么能做「复制文本组件」

原版 `ChatComponent` 把每一条消息存成 `GuiMessage`,里面保留着**原始 `Component`**:

```java
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature,
                         GuiMessageSource source, @Nullable GuiMessageTag tag) { ... }
```

而实际绘制用的 `GuiMessage.Line` 只有 `FormattedCharSequence`(排版后的字形序列)。
所以本模组在消息**进入聊天框的那一刻**就抓走 `Component`,而不是等渲染完再从行里反推。

注意原版只保留最近 100 条(`MAX_CHAT_HISTORY = 100`),所以本模组维护自己的环形缓冲(默认 1000 条),
并且在界面第一次打开时把原版仅存的那些补进来。

## 两种复制模式

| 模式 | 实现 | 丢失的信息 |
| --- | --- | --- |
| 纯文本 | `Component#getString()` | 颜色、加粗、点击事件、悬浮事件、translatable 结构全部丢失 |
| 文本组件 JSON | `ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component)` | 普通聊天消息无丢失;含 NBT 的组件(如 `nbt` 类型)在 JsonOps 下会有损,需改用 `NbtOps` 输出 SNBT |

两种模式的产物都是字符串,因此共用同一条剪贴板通道:
`KeyboardHandler#setClipboard` → `ClipboardManager#setClipboard` → `GLFW.glfwSetClipboardString`。
**不涉及 AWT / CF_HTML**,所以没有跨平台富文本剪贴板的坑。

## 26.1.2 的几个关键差异

写这个版本时踩到的坑,记录一下:

1. **Minecraft 26.1 起官方不再混淆代码**。旧的 `fabric-loom`(带 remap)不适用,必须用
   no-remap 的 `net.fabricmc.fabric-loom` 插件;不再需要 `mappings` 配置,依赖用普通
   `implementation` / `compileOnly`,也没有 `remapJar` 任务。用错插件会报
   `Failed to find official mojang mappings for 26.1.2`。
2. **access widener 的命名空间是 `official`**,不是 `named`。
3. **渲染模型变了**:`GuiGraphics` 已被移除,屏幕改为「提取渲染状态」,
   覆写 `Screen#extractRenderState(GuiGraphicsExtractor, int, int, float)`,绘制通过 `GuiGraphicsExtractor`。
4. **输入事件是记录类**:`mouseClicked(MouseButtonEvent, boolean)`、
   `mouseScrolled(double, double, double, double)`、`keyPressed(KeyEvent)`。
5. `ChatComponent.addMessage` 是 private,公开入口是
   `addClientSystemMessage` / `addServerSystemMessage` / `addPlayerMessage`,mixin 注入这三个。
6. `ChatScreen.init()` 是 `protected void init()` 且**没有 `CallbackInfo` 参数**,
   所以不能用 `@Inject(method = "init", ...)` 配 `CallbackInfo`;本仓库的做法是用
   access widener 把它提升为 public,注入点用 `@At("RETURN")`。

## 目录结构

```
src/main/java/com/example/textcomponentcopy/
├── TextComponentCopyClient.java        客户端入口
├── client/ChatHistoryScreen.java       聊天记录界面(开关 + 每行复制按钮)
├── core/
│   ├── ChatHistoryEntry.java           一条记录(组件 + 来源 + 时间 + 签名)
│   ├── ChatHistoryStore.java           环形缓冲(1000 条)
│   ├── ComponentTextExtractor.java     纯文本 / JSON 两种导出器
│   ├── CopyMode.java                   复制格式开关
│   ├── ChatCopyHelper.java             写剪贴板
│   └── VanillaChatBridge.java          读取原版聊天框现存消息的桥接
└── mixin/
    ├── ChatComponentAccessor.java      @Accessor 取 ChatComponent.allMessages
    ├── ChatComponentMixin.java         消息进入时抓取组件
    └── ChatScreenMixin.java            右上角按钮
```

## 构建

需要 **JDK 25**(26.1.2 要求)。

```bash
gradle build --no-watch-fs
```

产物:`build/libs/textcomponentcopy-1.0.0.jar`。

### 本机注意事项

* `gradle.properties` 里有四行 `systemProp.*.proxy*` 代理配置(指向 `127.0.0.1:7890`),
  换到没有代理的环境请删掉。
* 如果 Gradle 报 `Failed to load native library 'native-platform.dll'`,说明当前环境不允许写
  `~/.gradle/native`;把 `GRADLE_USER_HOME` 指到可写目录即可,例如:

  ```
  GRADLE_USER_HOME=<项目目录>/.gradle
  ```

## 已验证 / 未验证

**已验证**:`gradle build` 全流程通过(`compileJava`、`processResources`、`jar`、
`validateAccessWidener`),access widener 两条提升规则都能被 Loom 接受,产物 21KB 且内容干净。

**未验证**:还没有在游戏里实际运行过。界面绘制、mixin 注入点、剪贴板写入都属于运行时行为,
编译通过不代表运行正确。Minecraft 26.1.2 的资源包约 456MB,建议你本地跑:

```bash
gradle runClient --no-watch-fs
```

进游戏后按 T 打开聊天框(右上角应出现「聊天记录」按钮),聊几句后点按钮进入界面,确认:
1. 按钮位置和渲染是否正常;
2. 每行左侧复制按钮是否对齐;
3. 切到 JSON 模式后粘贴出来的内容是否是合法 JSON 组件。

## 已知取舍

* 老消息只补一次:原版聊天框里已有的 100 条会在界面首次打开时导入,之后再打开不会重复导入。
  模组启动**之前**收到的消息无法恢复(组件已经被回收了)。
* `clickEvent` / `hoverEvent` 目前原样保留在 JSON 里(`run_command` 也在其中),
  如果介意,可以在 `ComponentTextExtractor` 里做一次递归剥离。
* 切换世界不会清空缓冲,`ChatHistoryStore#clear()` 已经写好但还没有接入断线事件。
