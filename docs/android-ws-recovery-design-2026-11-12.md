# Android WS 恢复优化方案 v4：先解决及时收消息

状态：v4已实施并完成模拟器基础验证，尚未完成荣耀实机/耗电验收。2026-11-12初稿；2026-11-13更新。
工作空间：~/.codex/worktrees/android-ws-recovery/monorepo；根仓库/App分支均为codex/android-ws-recovery；设计时App HEAD=e1fbf602；实施时已合并origin/release 291e8f2，Android版本1.8.37。
依据：PR #27、实际荣耀故障、Claude Code审核与事实复核。本版替代v3的实施建议；第二轮审核只采纳经复核的结论。
已有暂存的WsDiagnostics、前后台/网络/电源快照、历史退出原因作为待修正基线；保活精确闹钟按第6节调整，不直接照搬。上述暂存改动不在远端PR head中。实施与验证结果见第9节。

## 1. 唯一首要目标

用户选择WS通道后，在网络和系统允许应用运行时及时收到消息；连接失败后恢复入口不能断链或卡死，用户打开App时立即检查恢复。

诊断是验证修复和定位后续故障的手段，不建设新的监控平台，不把精准识别冻结作为第一轮交付条件。不能承诺穿透用户强行停止、Doze或OEM冻结。

实际案例（2026-11-12北京时间）：04:14:45客户端Software caused connection abort，服务端同秒关闭；04:29:54出现onTaskRemoved；04:59:21消息创建时手机无连接，05:14:45重连并收到补发，延迟约15分24秒。服务端分发约59ms。04:14和04:29的日志到05:08:20才被SLS接收；05:14:52有回到页面记录。无法据此认定冻结、持续断网或特定重连触发来源。

## 2. 本轮范围与暂缓内容

必须做：连接状态一致性、网络恢复入口、回前台恢复、网络可用性判断、必要日志与离线汇总、服务启动失败可见、相关日志脱敏、荣耀实机验收。

保留现有心跳、退避、无网30秒Handler、周期保活、服务初始化和SLS持久化机制。不上线未经验证的新唤醒方式。

暂不做：60秒独立后台观察器、周期主线程探针及抓栈；完整channelEpoch/connectionId/taskId体系；第二套本地检查点或环形缓存；自动四维判定引擎；全链路event schema；OkHttp DNS/TLS阶段埋点；额外公网探测；allow-while-idle；自动切厂商通道；心跳或WS协议变更。

例外：如确定性并发测试证明旧任务会破坏当前恢复，仅针对该问题增加最小任务序号/身份保护，不扩展为统一调度框架。

## 3. 必须修改的恢复路径

### 3.1 连接状态不能卡死

修改WsManager：

- start对enabled/disableConnect的更新、Socket身份检查和状态写入使用同一把现有锁。
- 文本和二进制onMessage在锁内校验enabled和当前Socket身份；移除其中无条件写Connected。由有效onOpen建立Connected状态。
- tryConnect发现Connected但socket为空时记录一次异常并回到NotConnect，继续连接；不得因显示状态错误而永久跳过。
- 异步派发的DEVICE_INIT/UPDATE_CLIENT等控制操作在执行前再次核对enabled及捕获的Socket身份；身份复核与写Token/停止通道动作必须处于同一同步或串行边界，所有start/stop调用者也遵守该边界。不得锁内检查后锁外再执行，也不能仅假设所有调用者都在主线程。已有效接纳的业务消息沿用mid去重和投递，不因稍后切换通道丢弃。
- 锁内只做必要的身份检查、状态修改及控制动作的原子提交，捕获不可变事件字段；Binder快照采样、日志持久化、堆栈格式化放锁外，避免采样时的新状态覆盖故障时状态。没有实测不得假定这些调用仅耗时毫秒级。
- Request构建/newWebSocket同步抛出预期异常时回滚Connecting及socket状态，按原有策略安排恢复，不能永久卡在Connecting。不要将正常异步握手中的瞬时状态误判为需要再次建连。

不新增第二套状态机；不因未收到业务消息就主动判断连接死亡。

### 3.2 网络恢复时主动尝试，而不是只打印日志

- 现有onAvailable保持恢复入口。
- 对当前默认网络，blocked由true变false时调用一次tryConnect("network-unblocked")；首次收到false且已处于等待网络状态时也允许一次检查。
- onCapabilitiesChanged仅在networkUnavailable=true（确实等待网络）且当前相关默认网络具备INTERNET、未被已知blocked限制时检查恢复。第一轮不以VALIDATED变化触发抢先重连，信号强度、重复能力变化不得反复建连。
- 记录/使用传入回调能力；onAvailable时同步查询的能力可能尚未就绪，不能当作最终网络结论。
- 校验当前WS是否启用、回调网络是否仍相关；按network记录blocked边沿，网络替换后丢弃旧边沿。所有恢复事件只调用tryConnect(source)，禁止调用会重新启用WS的start()/ensureWsRunning()；enabled与disableConnect门禁保持有效，已连接/连接中合并处理。
- 同一网络的重复事件不能反复取消退避。只允许实际网络恢复/切换提前触发一次，真实连接失败继续原有退避。
- 网络事件触发尝试不增加周期任务或设备唤醒。被blocked时保留恢复观察，不新增高频主动请求。API26–28无onBlockedStatusChanged回调，依赖可用网络事件、现有30秒Handler及周期保活兜底；不得承诺解除阻断时立即收到回调。

### 3.3 回前台立即检查WS

在应用实际后台转前台入口只调用一次tryConnect("foreground")，禁止调用start()/ensureWsRunning()重新启用WS；与已有前后台跟踪合并，避免每次页面跳转、旋转重复调用。该次真正的前台恢复允许提前尝试，不改变随后真实失败的退避策略。

此动作与设备活跃HTTP上报的一小时节流分离，也不能因KeepWsAliveService已运行且通知存在而跳过。WS未启用或正常在线时无连接副作用。

仅亮屏未进入App不等同于回前台，不能据此承诺执行foreground入口；该场景仍依赖网络事件和现有保活任务。

### 3.4 网络检测不按传输类型排除可用网络

将Wi-Fi/蜂窝/以太网白名单改为当前网络具备NET_CAPABILITY_INTERNET的基础判定，覆盖VPN等用户网络。

INTERNET表示网络声明支持互联网，不代表端到端可达；VALIDATED仅用于诊断，blocked用于诊断与真实解除时的恢复判断，不能仅因VALIDATED=false永久拒绝尝试。最终以真实WS握手成功/失败确认可达性。

### 3.5 服务启动失败必须看得见

在现有Worker调用启动服务的边界捕获并分类预期IllegalStateException（含适用版本的ForegroundServiceStartNotAllowedException）、SecurityException，上传限频WARN和必要快照，返回明确failure，不自动retry。不得宽泛捕获Exception吞掉CancellationException。

Worker的catch不能捕获随后异步执行的服务onCreate异常。分别检查onCreate以及运行中通知恢复等所有startForeground调用点，在服务内部处理适用的预期启动异常：失败后退出当前启动流程、清理已取得资源并stopSelf，不继续保活初始化，运行标记保持false。部分初始化失败也按此回滚，不吞掉非预期编程错误。

正常START_STICKY前台服务的系统重启不受Android12新增的后台启动限制；不能据此推断sticky重启会崩溃循环。应用主动后台启动、任务移除闹钟拉起和系统重启分别记录、分别验证；异常保护不代表已证实本次事故根因。

不把WorkManager当作绕过系统后台启动限制的手段；不因此新建无限重试。回前台时已有启动入口可再尝试。初始化失败需保持运行标记与实际状态一致，清理已取得资源。

onTaskRemoved保留既有路径；补一条计划/实际服务入口记录。无法启动时如实记失败，不扩大为新的保活方案。

## 4. 最少但够用的诊断

复用WsDiagnostics及已有重连、保活任务，不新增后台观察线程。WsDiagnostics.init限定主进程，生命周期注册也限定主进程；非主进程不得上报主进程process-start或读写其历史退出去重标记，避免:mipush等进程混淆会话。

### 4.1 必须保留的少量信息

- 失败：异常类、message、限频完整堆栈、连接来源及当前快照。
- 进入/退出无网、blocked和重要能力变化：首次及真实变化记录。
- 调度：原因、alarm/handler模式、delay、必要计划时间；原因/模式变化立即记录，同状态每五分钟汇总。
- 实际执行：现有重连与保活回调内更新计数和最大间隔；明显迟到才单独打印。
- 取消：仅停止、通道切换或前台/网络事件抢先恢复等重要取消原因；取消逾期任务时保留逾期量。正常在线取消不刷日志。
- 服务：创建、工作启动完成、系统重建、销毁、任务移除及启动失败。
- 恢复：source、总离线时长、无网检查次数、真实建连尝试次数、主要跳过原因、最大执行间隔、是否观察到blocked。新进程首次连接或stop/start后无离线基准时，offlineMs输出unknown，不用0表示未观察到的离线时间。
- 消息：只记mid/type及必要延迟，保留接收和通知提交结果的区分。不新增正文或Token日志；顺手清理现有相关完整Token/消息原文输出，包含PushChannelCoordinator打印PushChannelTarget data class时隐含的完整Token。

### 4.2 双时钟和事件时间

关键诊断添加eventTimeMs、elapsedRealtime、uptimeMillis，应用启动记录PID及短随机processSessionId；相同会话内可计算耗时，跨进程不直接相减。

Handler按uptime调度，Alarm按elapsed调度；两类任务记录各自的计划时钟。恢复汇总同时给出elapsed和uptime增量：差值代表观察区间累计深度睡眠量，不代表精确睡眠时点或已确认Doze。

SLS __time__/接收时间本次已出现延后，不能直接替代原事件时间。核查日志对象构造时机，保留显式事件时间；复用SLS现有持久化和补传，不增加第二份缓存，不保证崩溃前最后一条必定落盘。

不要求新增完整eventId/schemaVersion体系；字段名称固定、日志文字保持可检索，先满足人工排查。

### 4.3 控制日志本身的成本

- 相同网络能力变化在采集昂贵快照前去重，仅重要字段变化触发；重复信号变化不做整组Binder查询。
- 锁外采样，单项读取失败为unknown，不丢弃整组证据、不影响恢复。
- 正常在线不增加周期日志；已有消息日志只输出必要元数据。
- 离线同状态五分钟一条汇总；转折事件及时记录。失败风暴按预算保留首末、类型计数及省略条数，避免每次同类堆栈强制写盘。
- 不自动修改线上SLS索引，初期复用msg检索，结构化索引单独评估。

## 5. 能回答什么，不能回答什么

|证据|能够报告|不能据此断言|
|---|---|---|
|现有检查持续执行且观察到无网/blocked|应用执行时网络不可用或被限制|物理网络整段断开、期间从未冻结|
|有默认网络但握手失败|网络存在，WS端到端失败|网络完全正常、服务端必然故障|
|elapsed增长多、uptime增长少|观察区间存在深度睡眠|该进程被OEM冻结或Doze已确认|
|elapsed和uptime均大幅增长而任务迟到|设备清醒期间存在执行/调度停顿候选|主线程阻塞或全进程冻结已被区分|
|新会话+匹配的历史退出记录|进程退出并重建，附系统原因|没有退出记录就一定没退出|
|系统轨迹明确对应PID的freeze/unfreeze|系统证实冻结，可与网络事件对齐|冻结是全部延迟的唯一原因|
|只有日志空窗|证据不足，标记未知|直接判为冻结/断网|

没有独立观察器时，不承诺区分主线程阻塞和全进程冻结。ANR退出记录缺失不能证明主线程正常。网络、设备睡眠与执行停顿可并存。

确需确认冻结时，在用户授权的实机复现中采集系统日志/Perfetto/bugreport；不在生产App中加入特权采集，不自动上传完整系统包。05:08有日志到达只说明该时刻存在上传活动，不能反推整段网络可用或WS可达。

## 6. 电量与系统限制

本轮新增周期线程/公网探测/WakeLock/闹钟数均为零；恢复来自既有任务和系统/前台事件。实际连接尝试仍受已有状态检查和退避约束。

现有持续PARTIAL_WAKE_LOCK、25秒ping和周期保活可能是主要耗电来源，不能因删了观察器就宣称低耗电。但本轮也不盲目删WakeLock：它与心跳存活和服务端判活相关，需独立实机对照。

第一轮五分钟保活检查沿用PR的普通set(ELAPSED_REALTIME_WAKEUP)，实施时回退暂存增强里有权限即setExact的策略；现有真实连接失败的重试调度保持原策略。没有实机收益证据，不将周期检查升级为精确闹钟。后续如需保留setExact，必须在同机、同设置下对比消息恢复与耗电，作为独立决策。

“本轮不增加闹钟”只描述任务数量；不能据此宣称策略、唤醒或耗电完全没有变化。普通和精确闹钟均不保证穿透Doze。

先验证消息恢复和稳定性，再以同机型、同设置、同发送节奏测试WakeLock/闹钟策略对照。通过batterystats、alarm、power与实际消息延迟记录判断，不编造耗电百分比。

普通set/setExact、Handler、WAKEUP闹钟不保证穿透Doze/OEM冻结，START_STICKY不保证重启，强行停止后通常需用户重新打开。首次交付不引入allow-while-idle，也不调整25秒心跳、WS协议或服务端超时。

## 7. 最小验收

### 确定性逻辑检查

- onMessage与stop/再次start并发，最终状态不能假Connected；两个消息回调均覆盖。
- 旧控制消息晚派发不能覆盖新路由或停止新通道；业务消息不误丢。
- onAvailable能力未就绪→caps→blocked(false)只形成一次有效建连；退避期间VALIDATED抖动、重复caps不得额外握手。blocked真实解除允许一次检查，旧network事件无效。API26–28单独验证现有检查兜底。
- 页面跳转/旋转不重复触发前台恢复；切厂商通道后网络恢复或回前台不能复活WS；Connecting时不并发建连。
- 无网回调遗漏/过早后，既有检查仍可持续恢复。
- 旧重试回调与停止/重新安排并发不能清空新的有效恢复任务；若测试失败，仅增加必要的任务身份保护。
- Worker启动调用失败、onCreate内前台提升失败、运行中补通知失败、部分初始化失败分别验证：明确结果、资源清理、标记一致、无无限重试；取消协程不被吞。正常sticky重启另测，不预设违反后台限制。
- 非主进程不产生主进程诊断或修改退出去重标记；锁外日志保留事件时的状态，不泄漏Token/正文（含data class输出）。双时钟、原因变化限频及offlineMs=unknown正确。
- 五分钟保活采用普通set；确认未新增周期任务或精确唤醒。同步建连异常可回滚且不永久卡Connecting。

### 荣耀AAP-AN00优先的实机检查

|场景|验收依据|
|---|---|
|息屏在线至少1小时，定时发送|每条mid对齐服务端创建与客户端接收，记录延迟分布和漏收|
|息屏断网后恢复网络|有有效恢复回调且系统允许运行时立即尝试，受控网络条件下验证握手完成及消息到达|
|Wi-Fi/蜂窝切换、可用时VPN|能重连，无风暴，不因传输类型误判无网|
|断线后用户回前台|直接检查连接，不受一小时活跃上报或剩余退避阻挡；已在连接中时不并发创建|
|省电/Doze后亮屏与回前台分别测试|记录真正触发来源和双时钟，不能把亮屏当作回前台|
|划除任务、进程被回收、服务重建|记录是否实际恢复、启动失败及退出原因；不预设必然拉活|
|强行停止再打开|标记系统限制，重新打开后恢复，不承诺停止期间收消息|
|高频收消息并切换通道|状态正确、切回后可连接、消息不误丢|

每项同时记录进程会话、重连来源、检查次数、日志量。受控前台/网络恢复场景以回调到尝试的响应无额外退避等待为逻辑门槛，实际到达延迟与现网旧版基线对比；测试前固定目标阈值，失败如实保留，不能仅“有日志”就算通过。

没有新增设备唤醒/公网探测，正常在线新增周期日志为零；相同离线状态日志按预算汇总。模拟器结果不替代荣耀OEM回归。

## 8. 实施与发布顺序

1. 先实现状态一致性、网络/前台恢复入口及网络检测，避免先做诊断工程。
2. 在现有任务中补关键转折日志、双时钟、失败分类及恢复汇总，减少现有无必要日志。
3. 完成确定性并发检查和荣耀实机验收，拿实际到达延迟验证收益。
4. 更新release基线并解决冲突，保留最新业务功能，版本号高于当前release；明确纳入哪些暂存改动，不能把本地审核当作远端PR已包含。重复必要回归。
5. 小范围验证恢复与耗电，再决定是否需要另开WakeLock/闹钟优化。若仍出现无法解释的长空窗，优先实机系统取证，再决定增加针对性诊断。

本轮仅Android客户端，无Server/desktop协议改动。未来若调整心跳、判活、ACK或重放，另评估服务端和desktop及nginx/LB边界。

## 参考

- 第一轮审核：android-ws-recovery-claude-review-2026-11-12.md。
- 第二轮审核：android-ws-recovery-claude-review-round2-2026-11-13.md（含复核修正；本版已纳入通过复核的边界）。
- Sticky服务重启：https://developer.android.com/reference/android/app/Service#START_STICKY
- Android时钟：https://developer.android.com/reference/android/os/SystemClock
- 网络状态：https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- Alarm调度：https://developer.android.com/develop/background-work/services/alarms
- Doze：https://developer.android.com/training/monitoring-device-state/doze-standby
- 后台前台服务限制：https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

## 9. 2026-11-13实施与审核记录

- 在原codex/android-ws-recovery分支实施，合并release 291e8f2，解决版本及设置页冲突并保留最新厂商订阅功能，Android版本1.8.37/10837。原有暂存诊断改动已纳入，五分钟保活使用普通set。
- 完成状态一致性、网络/前台恢复、旧控制消息隔离、INTERNET判定、服务启动预期异常及取消处理、运行标记回滚、双时钟/会话/离线汇总、网络能力采样前去重和日志脱敏。
- Claude Code在执行沙箱外只读审核，未发现高置信度P1；指出服务关键日志遗漏时钟/会话（P2），已补齐并保留这些低频生命周期事件的同步持久化路径。锁内Token注册日志改为debug；JSON解析失败禁止原始异常泄漏。详见android-ws-recovery-code-review-2026-11-13.md。
- Android API35模拟器11项instrumentation测试全部通过，无跳过。使用真实本地MockWebServer握手和消息、实际网络开关与系统回调、真实通知提交及mid去重；其余为确定性状态/队列交错测试，不宣称并发压力或OEM验收。
- offlineDebug/prodDebug构建通过。lint任务成功，报告0错误；工具存在Kotlin2.2元数据兼容提示，静态分析能力有限，具体警告数和最终结果见审核记录。
- 远端测试环境/ws实际返回HTTP502，未能完成测试服端到端验证；未对服务端部署或改配置。测试Application隔离生产SDK启动与请求，不向生产发送测试消息。
- 尚待荣耀AAP-AN00实机息屏1h、Doze/冻结、划除任务、服务启动受限及耗电对照。不因模拟器通过而认定本次荣耀事故已彻底解决。不保证OEM冻结或用户强行停止期间接收。
