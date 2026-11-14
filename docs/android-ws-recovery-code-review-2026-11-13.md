# Android WS v4 实施后的代码审核与验证（2026-11-13）

分支：codex/android-ws-recovery；已合并origin/release 291e8f2；Android 1.8.37/10837。无服务端/desktop协议或心跳变更，不创建新PR、不合并原PR。

## 结论与处理

Claude Code使用本机已有登录，在执行沙箱外进行只读审核（Read/Glob/Grep）；未修改文件。审核时HEAD、暂存和工作区差异哈希前后相同。CLI工具限制使其不能自行git diff；原有/新增归属已由Codex补充核对。未发现高置信度P1；一个P2诊断遗漏已修复，见下表。审核后的最小修复由Codex复核并完成针对性测试，未声称Claude重新审核过最终补丁。

|发现或建议|决定与验证|
|---|---|
|P2：服务生命周期/保活日志缺少事件时间、双时钟与会话|已修。新增serviceEvent，同步持久化低频关键服务事件，记录eventTimeMs/elapsedMs/uptimeMs/pid/session；保留崩溃前日志的既有路径，不保证最后一条一定落盘。|
|DEVICE_INIT链路PushManager日志仍在锁内同步写盘|已改debug，避免该处SLS强制刷盘。|
|能力恢复与blocked解除没有进入关键测试分支|已补两项实际WsManager回调序列，均只增加一次attempt；未启用仍不会复活。|
|前台与通知结果覆盖不足|已补前台抢先恢复/页面切换合并，以及真实通知提交/重复mid去重。|
|JSON解析异常可能带正文或Token|Codex补充修复：GsonUtils提供默认兼容的logErrors开关，WS解析关闭原始异常日志，仅输出脱敏元数据。|
|任务移除入口难区分|已增加TASK_REMOVED_RESTART action及实际服务入口记录。|
|启动失败WARN没有额外堆栈限频|保留现有低频启动请求与failure结果，不新增重试；同一权限问题反复发生时仍可能每五分钟一条，不是高频连接失败日志。后续据实测再收缩。|
|补通知失败建议保持服务运行|未采纳；保留v4失败后清理资源和标记一致的行为，避免无法确认前台状态时继续持有资源。|
|完整taskId/epoch、观察线程、周期公网探针|不引入，现有到期保护及状态门禁足够当前修复。|

## 对审核结论的边界复核

- release原本即有“真实连接失败时，有权限setExact、否则Handler”策略。此次仍使用该策略，但RTC_WAKEUP改为ELAPSED_REALTIME_WAKEUP，无网检查改为30秒Handler，不能笼统说与release所有调度完全不变。五分钟周期保活没有升级精确闹钟，没有引入allow-while-idle。
- Android/AOSP中受阻断时activeNetwork可能不可用，但不能断言荣耀所有版本必然如此；OEM上的blocked回调与握手行为仍需实机确认。
- 异步诊断缩短了锁内操作，不等于可以证明日志完全不影响性能或零耗电；还存在入队成本、失败快照采样及丢失尾部日志的风险。
- 半死连接仍等待原有25秒ping发现，不因无业务消息主动判死；没有承诺穿透冻结、Doze或用户强行停止。
- Worker/服务启动异常处理经过静态复核；真实Android后台限制、通知恢复失败和OEM重建仍未自动化或实机验收。旧UPDATE_CLIENT的“入口接纳后再切换”竞态由同锁复核保证，尚未专门用解析屏障注入测试，不能把入口拒绝测试称为完整并发验证。

## 验证

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :androidApp:connectedOfflineDebugAndroidTest :androidApp:assembleProdDebug --console=plain`：最终11项通过、0失败/0错误/0跳过；模拟器Android15/API35，约11.97秒；任务BUILD SUCCESSFUL。offlineDebug也在此过程构建并安装成功。
- 包括：真实本地WS握手、DEVICE_INIT与业务消息、cancel后由原有退避自动恢复；真实关闭Wi-Fi/数据再恢复并在20秒期限内收到消息（整个用例约5.6秒）；能力变化退避门禁；初次caps恢复与blocked(false)合并；blocked true→false一次触发；前台立即检查与页面变化合并；已停用WS不复活；旧Token派发隔离；接纳的业务消息切通道后继续派发；真实通知/mid去重；旧任务不消费新due。
- lint单独任务通过；报告存在工具的Kotlin2.2元数据兼容提示，因此0错误不是完整静态分析保证。最终任务BUILD SUCCESSFUL（10秒），报告0错误、331条警告；相关修改的WS/诊断/服务/JSON源码未出现在警告定位中，整体仍存在依赖、布局和资源等警告。
- `git diff --check`通过。
- 外部测试URL `http://wxpusher.test.zjiecode.com/ws` 返回HTTP502：未能完成测试服务器集成，无生产测试请求和部署。MockWebServer仅证明客户端在受控网络中的恢复和投递。
- 初期失败保留说明：外部测试服502；模拟器旧快照无默认网络，冷启动后恢复；测试专用Application安装广播初始化、MockWebServer关闭握手/服务端cancel清理问题均已修正。最终测试没有通过忽略或跳过隐藏失败。
- 测试Application只初始化本地依赖并关闭日志上传，跳过生产SDK/网络初始化；测试服务为本地回环地址。未做荣耀实机、电池对照、release混淆包安装和生产部署。

---

## Claude Code原始报告（上述复核和处理结果优先）

【识别到monoepo/AGENTS.md】

# Android WS v4 只读审核结论

**结论：没有找到高置信度的 P1。高置信度的 P2 有 1 项，属于诊断问题，不影响收消息。** 在代码逻辑层面，v4 已经做到三件事：
- 连接失败后，恢复入口不会断链；
- 网络恢复、回前台都会立即尝试连接；
- 停用的通道不会被这些入口复活。

模拟器的 7 项测试只证明了上述逻辑。荣耀 OEM、息屏 1 小时和耗电都还没测，不能据此说 11 月 12 日那类延迟已经修好。

**审核方式说明**：这次我只有读文件和搜索的工具，跑不了 `git diff`。所以"新增还是遗留"是根据代码、第二轮审核记录和设计文档推断的。凡是不能确认的地方，下文都会注明。我没有修改任何文件。

---

## 一、P1

无。

## 二、P2

### P2-1（诊断问题，不影响收消息）服务和保活路径的日志没有事件时间和会话号

- **位置**：`KeepWsAliveService.kt` 第 84、106、116、127、198、315、333 行。这几行直接调用 `WxpLogUtils.i`，涉及：系统重建、onCreate、onDestroy、onTaskRemoved、stopService、保活检查、保活调度。
- **对比**：`WsManager` 的日志都经过 `WsDiagnostics.event`，带有 eventTimeMs、elapsed、uptime、pid、session（`WsDiagnostics.kt:47`、`54-67`）。
- **原因**：SLS 的 `Log` 对象构造时，代码没有写入事件时间（`WxpLogUtilsKmp.kt:110-118`、`141-147`）。
- **触发序列**：事故中出现过 04:29 的 onTaskRemoved。当时这类日志延迟上传，SLS 的 `__time__` 变成了 05:08。v4 里这几行仍然没有显式事件时间。
- **后果**：
  - 下次出现同类空窗时，服务销毁、重建、任务移除、保活检查的真实发生时刻仍然无法还原；
  - 也无法和 WS 事件按同一会话对齐。
  - 设计 §4.2 的要求在最关键的服务路径上没有落实。
- **性质**：这几行日志本身是遗留的；v4 没有给它们补上时钟，属于本轮遗漏。
- **最小修复**：把 `WsDiagnostics.clocks()` 改为 internal，在上述几行同步拼接时钟和会话字段。不要改成异步的 event，保留现有同步强制落盘，否则进程被杀前的最后几条更容易丢。

---

## 三、按你的 5 个问题逐项说明

### 1. 消息接收、状态锁、旧回调、主线程 token 边界

- **onMessage 身份检查有效**：
  - 文本和二进制消息都在锁内检查 enabled 和当前 socket（`WsManager.kt:526-528`、`572-574`）；
  - 不再无条件写 Connected，Connected 只由 onOpen 设置（`:577-597`）；
  - OkHttp 会先回调 onOpen 再回调 onMessage，所以不会出现假 Connected。
- **UPDATE_CLIENT**：复核身份和 stop 在同一次加锁内完成（`:545-549`），有效。
- **DEVICE_INIT 的 token 边界真实有效**：
  - 派发经过 `Dispatchers.Main`，不是 immediate（`WxpScopeUtils.kt:22`），然后在锁内复核身份（`WsManager.kt:557-563`）；
  - 处理链是 `PushManager.onGetPushToken` → `PushChannelCoordinator.onWsToken` → `ThreadUtils.runOnMainThread`，在主线程上会直接同步执行（`ThreadUtils.kt:37-43`）；
  - 所以写 token、同步路由、进入 start/stop，都在同一次加锁内完成。
  - 我核对了调用 start/stop 的地方：协调器的回调都在主线程（ConfigManager 也切回了主线程，见 `ConfigManager.kt:138-141`）；唯一不在主线程的 stop 是 UPDATE_CLIENT，它同样受 WsManager 锁约束。
- **死锁**：没有发现。
  - OkHttp 不在自身锁内回调 listener；
  - 在 WsManager 锁内调用的 `newWebSocket`、`close`、`launch`、`postDelayed` 都是异步入队；
  - `WsDiagnostics` 不会反向获取 WsManager 锁。
- **业务消息**：已接收的 PUSH_NOTE 不会因为随后切换通道而丢（`:564-567`）。
- **通道复活**：不会发生。网络、前台、保活入口只调用 `tryConnect`，由它的 enabled 门禁拦住；`start()` 只有协调器的 `ensureWsRunning` 会调用。

### 2. 网络、caps/blocked、前台路径

- **主要序列都只建连一次**：
  - onAvailable 时能力未就绪 → caps → blocked(false)：caps 在 `networkUnavailable` 为真时检查一次；blocked(false) 因为前一个值是 null，且已不在等待网络，不再触发（`:177`、`:189`）。
  - blocked 由 true 变 false：触发一次。
  - 旧网络的事件：被 `current != network` 过滤掉。
- **不会重复握手或绕过退避**：
  - 状态为 Connected、Connecting、Closing 时，`tryConnect` 会合并请求；
  - 只有确实从"无网"变为"有网"时才清零退避（`:277-281`）。
- **前台**：只在 started Activity 数从 0 变 1 时触发；配合 700ms 防抖，页面跳转和旋转不会重复触发（`WsDiagnostics.kt:168-184`）。
- **被阻断时不会反复握手**：阻断时 `activeNetwork` 为 null，判为无网，只走 30 秒 Handler。

### 3. 服务与 Worker

- **CancellationException 处理正确**：Kotlin 的 `CancellationException` 就是 `java.util.concurrent.CancellationException`，它是 `IllegalStateException` 的子类。`KeepWsAliveServiceStarter.kt:56` 先单独 catch 并重新抛出，顺序正确。
- **其他异常**：返回 failure，不重试；下一次尝试由前台的 onResume 或广播触发，不会无限重试。
- **onCreate 里提升前台失败**：调用 `failStartup` → 清理 → `stopSelf`，onStartCommand 返回 NOT_STICKY（`KeepWsAliveService.kt:75`、`104-110`、`191-195`）。
- **部分初始化失败**：已经拿到的 wakelock 和闹钟都会释放（`:153-176`、`226-235`）。
- **isServiceStarted**：只在完全成功后才置 true；所有清理路径都会置 false。
- **过期回调**：由 `destroyed` 标志和 cancel 拦住（`:309`）。

### 4. 诊断与耗电

- **不影响恢复**：日志在调用处先记录时钟，再在单通道 IO 上异步写入；状态快照在锁外采集（`WsDiagnostics.kt:54-97`），不会阻塞恢复。
- **没有新增耗电项**：
  - 没有新增专用线程（`limitedParallelism(1)` 复用 IO 线程池）；
  - 没有周期探测；
  - 5 分钟保活已改为普通 `set`（`KeepWsAliveService.kt:325-329`）。
- **汇总和限频正确**：
  - 原因或模式变化立即记录，同一状态 5 分钟一条（`WsManager.kt:353-357`）；
  - onOpen 时输出 offlineMs/offlineUptimeMs，没有基准时输出 unknown；
  - Handler 任务按 uptime 判断迟到，闹钟任务按 elapsed 判断（`:365-367`）。
- **问题**：只有 P2-1，以及下面列出的低优先项。

### 5. 测试

- **哪些是真实 I/O**：
  - 第 5 个测试（本地握手、取消后由退避自动重连）和第 6 个测试（真实系统网络回调、20 秒内恢复）走真实 I/O；
  - 第 6 个测试的服务端在 localhost，只能证明"网络门禁加回调触发"，不能证明公网可达。
- **其余测试**：通过反射设置状态，测的是门禁，测得有效，但不是并发测试。
- **覆盖缺口**（只需补少量小用例，不需要框架）：
  - 第 7 个测试从未进入 `networkUnavailable=true` 分支，也没测 enabled 状态下 blocked 由 true 变 false。所以"只建连一次"和"解除阻断触发一次"其实没有验证。建议用同一个 callback 实例补一个序列测试，断言 `attempts` 只加 1。
  - 第 2 个测试里旧 socket 的 204 消息在入口处（`:527`）就被拦下，没有测到锁内复核（`:546-547`）。
  - `WsTestApplication` 没有初始化 `WsDiagnostics`，所以脱敏、时钟字段、unknown 都没覆盖。
  - 服务启动失败和 Worker 取消的路径没有自动化测试，需要人工或实机验证。
- **没有隐藏失败**：listener 里的断言在主线程失败时会让测试进程崩溃，结果会显示为失败，不会被吞掉。

---

## 四、可接受的限制（不算缺陷）

- **半死的 Connected 连接**：如果对端的 FIN/RST 丢失，连接显示 Connected 但已失效。这时前台和网络事件都不会抢先重连，只能等 OkHttp 的 25 秒 ping 失败（清醒时间里最长约 50 秒）。这是设计里"不因没收到消息就判定连接死亡"的选择。
- **切换通道瞬间的消息**：stop 之后旧 socket 上才到达的帧会被丢弃（`WsManager.kt:527`）。窗口很窄，属于设计选择；服务端是否把这类消息算作已送达，未核实。
- **API 26–28**：没有 blocked 回调，只能依赖 30 秒 Handler 和 5 分钟保活。
- **服务重启**：正常 sticky 前台服务的系统重启不受 Android 12 后台启动限制，这一点不再重复讨论。
- **stopSelf 后是否崩溃**：startForeground 抛异常后再 stopSelf，是否还会触发"did not then call startForeground"，要看系统当时的 fgRequired 状态。预期的失败路径多数不涉及它，但需要实机 logcat 确认，不能下结论。
- **lint**：lint 有 Kotlin 2.2 元数据兼容输出，"0 错误"作为证据较弱。
- **重连精确闹钟**：重连调度是有权限时用 `setExact`（`:329-343`）。我无法 diff，请用 `git diff 291e8f2 -- …/WsManager.kt` 确认它和 release 基线一致，再说"重试策略没变"。

## 五、低优先建议

1. **锁内同步刷盘**：`PushManager.kt:77` 的 `WxpLogUtils.i` 在 DEVICE_INIT 的锁内同步强制刷盘（`WxpLogUtilsKmp.kt:65-69`）。每个连接一次，不会死锁，但违背了设计 §3.1。建议改用 `WsDiagnostics.event` 或降为 debug 日志。
2. **启动失败日志没有限频**：Worker 和服务启动失败的 WARN 带完整堆栈，且没限频（`KeepWsAliveServiceStarter.kt:59-63`、`KeepWsAliveService.kt:193`）。Android 14 上用户可以划掉前台服务通知，之后 5 分钟保活每轮都会提交一个 Worker（`:320`）。如果后台启动被拒，每 5 分钟会写一条堆栈。这一点我没实测，建议按首条加计数的方式限频。
3. **运行中补通知失败的处理**：运行中补发通知失败时会停掉整个前台服务（`:148-150` → `191-195`）。这符合设计，但对"及时收消息"不利。可以考虑只记录日志、保持运行。
4. **等待网络时的重复检查**：等待网络期间，每次 caps 变化都会做 2 次 Binder 查询并重排 30 秒 Handler（`:177` → `265-269`）；每次 ws-failure 也会先采集快照再去重（`WsDiagnostics.kt:91-93`）。影响小，可以先去重再采集。
5. **入口和结果日志不全**：
   - onTaskRemoved 拉起服务的实际入口无法和其他启动区分（restart intent 没有 action，走到 `:81`），建议加一个 action；
   - 通知提交结果没有日志。
6. **清理和遗留项**：
   - 死代码：`WsManager.kt:272-275`、`:283`；
   - 设计文档头部（第 3-6 行）仍写着"未实现"；
   - 华为、vivo 仍打印完整厂商 token（`HuaweiHmsMessageService.kt:22,28`、`HuaweiPushUtils.kt:24`、`VIVOPushUtils.kt:52`），属于 WS 范围外的遗留问题。

## 六、目标达成程度和必要修正

- **逻辑层面**：§3 的恢复路径已经实现，模拟器做了基础验证，没有发现阻塞性代码缺陷。
- **效果层面**：荣耀上息屏、Doze、冻结、划除任务这些场景下能否及时恢复，以及耗电情况，都没有验证。
- **发布前必须做**：
  1. 按设计 §7 在荣耀实机上验收，并和旧版对比消息延迟分布；
  2. 确认重连 `setExact` 和 release 基线一致。
- **强烈建议一起修**：P2-1，改动很小，下次定位问题要靠它。
- **可以随后处理**：第 5 节第 1、2 项，以及测试缺口里第 7 个测试的分支补充。
