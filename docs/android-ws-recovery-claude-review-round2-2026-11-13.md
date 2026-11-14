# Android WS 恢复第二轮审核与复核（2026-11-13）

范围：未合并 PR #27（head e1fbf602 / base f51bcb15）、本地暂存诊断增强、v3设计稿。由本机 Claude Code 在执行沙箱外只读审核；Codex复核代码与关键平台规则。未实现、未编译、未跑实机、未访问生产。

## 复核后的结论

未找到PR和暂存改动引入的高置信度P1回归，不代表已通过运行验证。现有onMessage状态竞态仍需按v3修复。v3可作为实施基础，实施前应明确以下边界：

1. 网络能力回调仅在等待网络且实际具备INTERNET等基础条件时检查恢复；VALIDATED只诊断，不用其抖动反复抢占退避。blocked仅真实解除/初次等待网络时触发，过滤无关或过期网络。API26–28没有blocked回调，保留现有检查兜底。
2. 前台和网络恢复只调用tryConnect，不调用会重新启用WS的start/ensureWsRunning。此项v3已经基本写明，是实施约束，不应重复算作已发生缺陷。
3. WsDiagnostics初始化限定主进程；必要时生命周期注册也限定主进程。当前存在:mipush进程，诊断启动和历史主进程退出可能串号。这影响归因，不是已证实的荣耀收消息原因。
4. 检查Worker调用服务的边界和服务内部所有startForeground调用点，分类处理预期启动失败，保持资源/运行标记一致，不吞CancellationException、不无限重试。不能把Worker catch当作能捕获异步onCreate异常。
5. 暂存的五分钟保活从普通set变为有权限时setExact是实际策略变化；它不穿透Doze。建议首轮沿用PR普通set，除非同机对照证实精确调度对恢复有收益；本轮审核未改代码或替用户作最终取舍。
6. 控制命令的身份复核与会影响通道的动作必须在同一串行/同步边界；锁内捕获不可变事件，锁外做Binder/日志。脱敏包含Coordinator里带token的data class字符串。

## 对Claude原报告的修正和保留意见

- **不采纳“正常START_STICKY重启因Android12后台启动限制形成崩溃循环”的推断。** Android官方Service文档明确sticky前台服务的系统重启不受该项限制；应用主动后台启动与系统重启必须分开。其他启动路径及OEM差异仍需验证，异常保护可以考虑，但不得当作已确认根因或新引入缺陷。来源：https://developer.android.com/reference/android/app/Service#START_STICKY ；https://developer.android.com/develop/background-work/services/fgs/launch 。
- **保留显式eventTimeMs。** 本次既有查询已核实04:14/04:29事件的SLS __time__为05:08:20，v3已写明，不能因SDK一般行为推测而删掉。Claude再次建议核实并不推翻现有证据。
- **不采纳“诊断只阻塞毫秒级、不会造成收不到”的确定性措辞。** 没有测量可以给出IO/Binder耗时上界；锁内强制日志刷盘可能拖慢状态回调。v3移出锁及采样前去重的要求仍然必要。
- 复核控制命令不能仅依赖“通常在主线程执行”；需确认所有stop/start调用者使用同一同步边界。业务消息已经接纳后保持投递和mid去重。
- 没有证据要求完整epoch/taskId框架，但过期回调仍可能提前握手或污染新的due诊断。保留v3确定性测试；若证实问题，仅增加最小任务身份或到期校验，不能宣称不存在竞态。
- “Connecting且socket为空”的同步构建异常属于低概率遗留问题；若纳入修复，优先捕获合法范围的建连异常并回滚状态。不得把异步握手中途的瞬时状态误判为需要再次建连。
- PR合并冲突、release基线和版本落后是发布阻塞；暂存诊断改动不在远端PR中，审核已分层区分。

## 审核完整性

审核前后：暂存差异、未暂存差异、v3设计文件SHA256均一致。仅新增本报告；未修改设计或实现。最小回归范围见原报告最后一节，需加入服务通知重建的startForeground调用点和明确的网络恢复门禁。

---

## Claude Code原始输出（未经改写；以上复核优先）

【识别到monoepo/AGENTS.md】

# Android WS 恢复第二轮只读审核（PR #27 + 暂存改动 + v3 设计）

本轮只读了代码和文档：没有改文件，没有编译，没有跑设备，也没有访问生产资源。`wxpusher-server` 的 ws-connect 源码在这个 worktree 里没有检出，所以凡是依赖服务端 token 语义的判断，都标为"条件成立"。

## 总结论

- **A+B（实际文件）**：没有找到 A 或 B 引入的 P1 回归。A 本身做得对：锁内的身份检查、单调时钟、无网时不推进退避、保活兜底不再降级成 Handler、onCreate 幂等启动。B 的问题集中在诊断开销、多进程误归因和耗电，不会造成收不到消息。
- **C（v3）**：方向正确，可以按 v3 实施。但有 4 处决策要先改写清楚，否则实施时容易引入新问题：
  1. 3.2 网络触发条件写得不明确；
  2. 3.5 只覆盖了 Worker，没有覆盖 onCreate 里的 `startForeground`；
  3. WsDiagnostics 没有限定主进程；
  4. 恢复入口只能调 `tryConnect`，不能调 `start()`。
- **不需要完整的 taskId/epoch**：我核查了真实时序，结论见第 4 节。
- **发布阻塞（已知）**：PR 停在 1.8.32，release 已经到 1.8.36，mergeable=false。另外 B 不在远端 PR head 里，发布前必须 rebase、带上 B（或明确舍弃）、版本号高于 1.8.36。

---

## 一、高置信度发现（未被 v3 正确覆盖，或需改写）

### P2-1 ［C｜实施时可能引入｜条件成立］3.2 的网络触发条件不明确，可能绕过退避，或者用错网络判断

- **位置**：v3 §3.2 写的是"重要能力从不可用转可用"。实际代码 `WsManager.kt:208` 的 `tryConnect` 用 `activeNetwork` 现查网络，并不使用回调传进来的 network/caps。
- **问题 1：VALIDATED 抖动会反复绕过退避**
  - 触发序列：服务端不可达或网络较差，连接失败，`reTryCount` 已升到 120 秒档。此时 Wi-Fi 或蜂窝的 VALIDATED 在 false/true 之间抖动。
  - 如果实现把"VALIDATED 转为 true"也算作"重要能力转可用"，每次翻转都会调 `tryConnect`。`tryConnect` 在 `WsManager.kt:224` 会取消已排定的退避任务，然后立即真实握手。
  - 后果：每次抖动都绕过一次退避。次数受抖动频率限制，不会无限，但会削弱"真实失败继续退避"。
- **问题 2：API 26–28 没有阻断回调**
  - minSdk=26（`libs.versions.toml:4`），而 `onBlockedStatusChanged` 是 API 29 才有的。
  - 在 26–28 上，解除阻断只能靠现有的 30 秒 Handler 和 5 分钟保活。v3 没写这一点，验收时容易误判。
- **最小修正（建议直接写进 v3）**：
  - `onCapabilitiesChanged` 只在 `networkUnavailable == true` 时调用 `tryConnect`。这个状态 A 已经有了，不需要新增状态机。
  - `onBlockedStatusChanged` 按 network 记住上一次的 blocked 值，只在 true→false 时触发；首次收到 false 且 `networkUnavailable == true` 时允许触发一次。
  - 第一轮不把 VALIDATED 作为触发条件，只用于诊断。
  - 文档注明 API < 29 时的恢复路径。

### P2-2 ［C｜引入风险｜条件成立］恢复入口必须只调 `tryConnect`，不能调 `start()` 或 `ensureWsRunning`

- **位置**：`WsManager.kt:108-113` 的 `start()` 会无条件设置 `enabled=true`、`disableConnect=false`。
- **触发序列**：用户已经切到厂商通道，`stop()` 把 `enabled` 置为 false。之后发生回前台或网络事件。如果实现图省事调了 `WsManager.start()`（或协调器的 `ensureWsRunning()`），WS 会被重新启用并连上。
- **后果**：WS 和厂商通道同时运行；服务端路由已经切走，WS 却还持有连接、耗电。服务端收到 UPDATE_CLIENT 后的 stop 也会被绕过。
- **现状**：v3 §3.3 写了"WS未启用…无连接副作用"，但没有限定调用哪个入口。
- **最小修正**：在 v3 里明确：前台、网络这类恢复入口只调 `tryConnect(source)`，依靠它在 `WsManager.kt:191` 的 `enabled` 检查作门禁。

### P2-3 ［遗留｜v3 未覆盖｜条件成立，需实机或日志确认］服务重建时 onCreate 里的 `startForeground` 可能抛异常导致崩溃

- **位置**：`KeepWsAliveService.kt:100-107`。onCreate 无保护地调用 `startForeground`；A 之后还会同步进入 `startService`。
- **触发序列**：
  1. Android 12+ 上，进程被回收；
  2. START_STICKY 让系统在后台重建服务，或者 `onTaskRemoved`（`:119-132`）的 PendingIntent 在后台拉起服务；
  3. 应用不在前台启动豁免中，`startForeground` 可能抛 `ForegroundServiceStartNotAllowedException`；
  4. 异常发生在 onCreate，未捕获，进程崩溃。
- **后果**：进程崩溃，sticky 再次重建，形成崩溃循环（系统会逐步拉长重启间隔），恢复窗口反而变长。这是旧问题，A 没有让它变坏。
- **现状**：v3 §3.5 只要求在 Worker 边界捕获，没有覆盖这条路径。
- **最小修正**：
  - 在 onCreate 的 `startForeground` 外 catch `IllegalStateException`（`ForegroundServiceStartNotAllowedException` 是它的子类）。
  - 捕获后记一条 WARN，调用 `stopSelf()`，不进入 `startService`，`isServiceStarted` 保持 false。
  - 先在 SLS/崩溃平台搜 `mAllowStartForeground false` 确认是否真实发生；荣耀"划除任务/进程回收"场景要专门核对这一项。

### P2-4 ［B｜引入｜条件成立：存在非主进程时］WsDiagnostics 没有限定主进程，可能把非主进程启动误判为主进程重建

- **位置**：
  - `WxPusherApplication.kt:33` 在每个进程里都注册了生命周期回调；
  - `WxPusherApplication.kt:70` 的 `WsDiagnostics.init` 在 `initSdkAfterAgreement` 里，没有判断进程。对比 `PushManager.kt:36` 是有判断的；
  - Manifest 里声明了 `:mipush` 进程（`AndroidManifest.xml:207-216`）。
- **触发序列**：小米设备拉起 `:mipush` 进程，该进程执行到 `WsDiagnostics.init`：
  1. 以 force 写出一条 `process-start`，PID 是 mipush 进程的；
  2. 后台线程执行 `reportPreviousExit`（`WsDiagnostics.kt:94`），按包名过滤出主进程的退出记录并上报，再写 `EXIT_REPORTED_KEY`。
- **后果**：
  - 排查时会把 mipush 进程启动误当成主进程重建；
  - 如果存储跨进程可见，主进程自己反而可能漏报，或与 mipush 进程重复上报。
  - 对荣耀事故机型基本无影响。
- **最小修正**：`WsDiagnostics.init` 开头加 `if (!ApplicationUtils.isMainProcess()) return`（该方法已在 `shared/.../ApplicationUtils.kt:47`）。

### P2-5 ［B｜引入的耗电变化｜确定存在，影响需测量］5 分钟保活检查改成了精确闹钟

- **位置**：`KeepWsAliveService.kt:298-317`。A 已经改成普通 `set(ELAPSED_REALTIME_WAKEUP)`，B 又改回：有权限时用 `setExact`。
- **影响**：
  - 健康在线时每 5 分钟固定一次精确唤醒；这次检查只做一次空的 `tryConnect` 和一次启动器检查。
  - 非 allow-while-idle 的闹钟在 Doze 下同样被推迟，所以对事故场景收益有限。
  - 服务本来就一直持有 PARTIAL_WAKE_LOCK，CPU 醒着时普通 `set` 的窗口已经够用。
- **现状**：v3 §6 承认"需测量"，部分覆盖。
- **建议决策**：第一轮默认回退到 A 的 `set`；只有在荣耀耗电对照里证明 `setExact` 能缩短恢复时间，才保留。

### P3 ［C｜v3 未覆盖｜低概率］状态卡在 Connecting 但 socket 为空

- **位置**：`WsManager.kt:228-234`。代码先设置 Connecting，再调 `Request.Builder().url()` 和 `newWebSocket`。
- **触发**：如果这一步抛异常（例如 offline flavor 测试面板里填了非法的自定义 WS 地址），状态永久停在 Connecting，之后每次 `tryConnect` 都在 `:200` 直接返回。
- **最小修正**：既然要实现"Connected 但 socket 为空就自愈"，顺手把 Connecting 也纳入同一判断，或者给 `newWebSocket` 加 try/catch，失败时回到 NotConnect 并调用 `tryConnectDelay`。生产环境地址固定，概率很低。

---

## 二、已被 v3 正确覆盖（实施时验证，不计为未解决缺陷）

| 风险 | 层/性质 | 说明与实施要点 |
|---|---|---|
| `onMessage` 不在锁内，之后无条件写 Connected，可能卡在"已连接但没有 socket"（`WsManager.kt:449-453`、`487-492`） | 遗留 P1，低概率、后果严重 | v3 §3.1 已覆盖，实施时验证。死锁风险分析：OkHttp 在 RealWebSocket 锁之外回调 listener；锁内调 `close()` 和 `newWebSocket` 都是异步入队；WsDiagnostics 不反向获取 WsManager 锁。所以加锁不会形成环。删除 onMessage 里写 Connected 也安全：OkHttp 保证先回调 onOpen，`webSocket` 非空时状态只可能是 Connecting/Connected/Closing。 |
| 旧连接晚到的 UPDATE_CLIENT 会停掉新通道（`:470-472`） | 遗留 | 已覆盖，实施时验证。要点：解析放在锁外，但"复核身份再 `stop()`"必须在同一次加锁内完成。只在锁内检查、锁外再调 `disconnect()` 等于没修。 |
| 旧连接晚到的 DEVICE_INIT 覆盖 WS token | 遗留，后果取决于服务端是否每次连接重新发 token（未核实） | 已覆盖，实施时验证。在 `:480` 的主线程派发处复核：`enabled` 为真且当前 socket 仍是捕获到的那个。`stop`/`start` 和 `onWsToken` 都在主线程执行，所以复核和执行是原子的。PUSH_NOTE 不加这项检查。 |
| `logState` 在 WsManager 锁内执行 binder 调用并强制刷盘写 SLS（`:443`、`:504`） | B，引入 | 已覆盖（§3.1 末条），实施时验证。 |
| 每次 `onCapabilitiesChanged` 都执行约 12 次 binder 查询（`:139-141`） | B，引入 | 已覆盖（§4.3），实施时验证。日志会去重，但查询本身照样执行。 |
| 五分钟限频所有原因共用，原因切换被吞掉（`:287-290`） | A | 已覆盖（§4.1）。 |
| Handler 按 uptime 调度，但 `lateMs` 用 elapsed 计算，深睡时间被算成"延迟执行"（`:300-303`） | A，会误归因 | 已覆盖（§4.2）。 |
| Worker 吞掉前台服务启动失败（`KeepWsAliveServiceStarter.kt:55-58`） | 遗留 | 已覆盖（§3.5）。补充一个实施要点：只 catch `IllegalStateException` 和 `SecurityException`；不要用 catch `Exception`，它会吞掉协程的 `CancellationException`。返回 `failure`，不要返回 `retry`。 |
| `isServiceStarted` 先置 true，再拿锁、再 doWork（`KeepWsAliveService.kt:149-163`） | A | 已覆盖（§3.5"标记一致"）。 |
| 消息全文和 token 写进 SLS | 遗留 | 已覆盖（§4.1）。清理范围要补一处：`PushChannelCoordinator.kt:509-512` 打印的 `PushChannelTarget` data class 的 `toString` 也带完整 token。其余位置是 `WsManager.kt:163`、`454/457/462/467`、`PushManager.kt:77`。 |

---

## 三、逐项回答 7 个重点问题

### 1. onMessage 加锁、移除 Connected

- 死锁：没有证据，理由见第二节第一行。
- 事件顺序：没问题，状态只由 onOpen、onFailure、onClosed、stop 决定。
- 业务消息误丢：只要身份检查放在入口，PUSH_NOTE 派发时不再复核，就不会丢。
- 对通知去重没有影响：`WxpNotificationManager.kt:113-139` 的 mid 去重在派发之后才执行。

### 2. 网络与前台入口

- 重复或过早触发：
  - onAvailable 时查询的能力可能还没就绪，会走 30 秒检查；v3 的"等待网络时由 caps 触发"正好补上这一点。
  - API 29+ 上一个新默认网络会连续收到 onAvailable、caps、blocked(false) 三次回调，后两次会因为状态已是 Connecting 被合并掉。
- 绕过退避：
  - 前台入口是有意绕过，次数受用户操作限制，可以接受。
  - 网络入口必须按 P2-1 限定触发条件。
- 取消新任务：`tryConnect` 只有在自己真正发起连接时才取消待执行的重试，等于用一个进行中的连接替换了它，不会丢失恢复能力。
- 错误切回 WS：见 P2-2。
- INTERNET 判定：
  - 全仓只有 `WsManager.kt:208` 调用 `isNetworkConnected`，不影响其他调用者。
  - AOSP 上被阻断的应用拿到的 `activeNetwork` 是 null，所以依然判为无网（荣耀需实测）。
  - 强制门户这类网络原本就能通过 Wi-Fi 白名单，行为不变，没有回归，收益主要在 VPN 和其他传输类型上。

### 3. 服务启动

- Worker 吞掉失败：遗留问题，v3 已覆盖。
- onCreate 的 `startForeground` 异常：见 P2-3，未覆盖。
- A 的启动器判断条件 `isRunning && 通知存在`（`KeepWsAliveServiceStarter.kt:71-75`）：没有发现漏启动。进程死亡时静态标记和通知会一起消失。
- `cleanupServiceResources` 是幂等的，没有发现清理错状态。
- 有一个遗留问题本轮不处理：Android 14 允许用户划掉前台服务通知。划掉后，5 分钟检查每轮都会提交一次 Worker；在后台启动可能失败，会产生少量无效唤醒。

### 4. 不加 taskId 是否还有真实竞态

结论：没有会让恢复链断掉的竞态，不需要 taskId。

- **唯一真实的时序**：
  1. 精确闹钟已经触发，`onAlarm` 已投递到主线程队列；
  2. 这时网络或前台入口调了 `tryConnect`，其中的 `cancelReconnectTask()`（`:307-311`）无法撤回已在队列里的 AlarmManager 回调；
  3. 主线程随后执行这个过期的 `runScheduledReconnect`。
- **后果**：
  - 只会把 `reconnectDueElapsed` 清零，影响的是下一次 `lateMs` 的诊断精度；
  - 或者在新连接已经失败、新退避已排定的情况下，提前一次尝试连接。
  - `tryConnect` 在 Connected、Connecting、Closing、未启用、disableConnect 这几种情况下直接返回，而这些状态本来就不需要待执行任务，所以不会出现"清掉有效任务却没人补上"。
- 保活闹钟同理：过期回调落在已销毁的服务实例上，需要毫秒级的时间窗口，可以忽略。
- **最小修复**：不修。如果在意诊断精度，只把这种情况记为"未知"。

### 5. 暂存的 WsDiagnostics（B）

- 主线程阻塞：只有毫秒级，来源是锁内 IO（已覆盖）。
- 刷日志：没有证据。有去重，正常在线时没有周期日志。
- 耗电：binder 调用过于频繁（已覆盖），外加 P2-5。
- 前后台误判：没有证据。用了计数加 700ms 去抖。
- 进程退出误判：见 P2-4。
- 导致服务异常：没有证据。`state()` 和 `reportPreviousExit` 都包在 `runCatching` 里；API 33 才有的 exit reason 常量在编译期内联，低版本不会崩。

### 6. 日志、时钟与归因

- **A 引入一处轻度误导**：`onOpen` 在新进程里首次连接，或者 `stop`→`start` 之后，会记 `offlineMs=0`（`:501-503`），容易被读成"没有离线过"。最小修正：没有基准时间时输出 `offlineMs=unknown`。
- **eventTime 先核实、再决定加不加**：据我对阿里云 SLS producer SDK 的了解，`Log` 在构造时会写入秒级的 `__time__`，所以事件时间可能已经存在，被延后的只是接收时间。这点需要在 SLS 里对比 04:14 那两条日志的 `__time__` 和 `__receive_time__` 来确认。
- 跨进程的汇总丢失、只有空窗无法下结论：这些属于 v3 §5 表格里已经声明的"未知"，不算缺陷。

### 7. 脱敏、路由、去重与隐私

- 恢复入口不碰路由：前提是遵守 P2-2。
- mid 去重：不受影响。
- WsDiagnostics 在同意隐私协议前只做计数、不上传，同意后才 init，与隐私初始化没有冲突。
- `network=$network` 只是 netId，不算敏感信息。
- 脱敏清单要补上 Coordinator 那一处（见第二节最后一行）。

---

## 四、按 v3 实施前需修订的决策

1. **§3.2**：改为采用 P2-1 的触发规则；VALIDATED 不作为触发条件；注明 API < 29 时的恢复路径。
2. **§3.3 和 §3.2**：明确恢复入口只调 `tryConnect`（P2-2）。
3. **§3.5**：补上 onCreate 的 `startForeground` 保护（P2-3），并写明 catch 范围。
4. **§3.1**：写明"复核身份后再 stop"在同一次加锁内完成；可选：Connecting 加 socket 为空也纳入自愈（P3）。
5. **B 的基线**：WsDiagnostics 只在主进程运行（P2-4）；5 分钟检查是否保留精确闹钟，作为明确的对照项决定（P2-5）。

## 五、最小回归清单

### 确定性测试（假 socket 或注入回调）

1. 两个 `onMessage` 分别与 `stop()`、`stop()+start()` 交错：最终不会出现"Connected 但 socket 为空"。
2. 旧 socket 晚到的 UPDATE_CLIENT 和 DEVICE_INIT，不能停掉新通道，也不能覆盖 token。PUSH_NOTE 收到后再切换通道，仍然通知一次；重放同一个 mid 被去重。
3. 网络事件序列：
   - onAvailable（能力未就绪）→ caps → blocked(false)：只连一次；
   - 退避期间反复收到 caps 或 VALIDATED 抖动：不额外建连；
   - blocked true→false：连一次；
   - WS 未启用：一次都不连。
4. 前台入口：页面跳转、旋转不重复触发；Connecting 时不并发建连；切到厂商通道后回前台，WS 不复活。
5. 闹钟已入队后被取消：最多提前一次尝试，恢复不中断。
6. Worker 和 onCreate 抛出启动异常：只记一条 WARN，不重试，不崩溃，`CancellationException` 不被吞。
7. 日志：原因切换立即记录；同一状态每 5 分钟最多一条；没有 token 和正文；非主进程没有 WsDiagnostics 日志；`offlineMs` 没有基准时输出 unknown。

### 荣耀 AAP-AN00 实机

按 v3 §7 的表执行，另外补 3 项：

- 息屏 1 小时以上：核对是否出现 `blocked` 回调和 ECONNABORTED，以及解除阻断到 onOpen 的耗时；
- 划除任务、进程被回收后服务重建：检查有没有 `startForeground` 崩溃；
- 5 分钟检查用 `set` 和 `setExact` 各跑一次耗电与消息延迟对照。

以上都还没有在实机上验证。
