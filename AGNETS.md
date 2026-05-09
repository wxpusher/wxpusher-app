# wxpusher-app — 移动客户端壳工程


## 固定首句

- 每次回复的第一句话必须是：`【识别到wxpusher-app/AGENTS.md】`

## 技术栈
- **架构**：Kotlin Multiplatform (KMP) + Android 原生 + iOS 原生
- **Android**：Kotlin、Gradle Kotlin DSL、AGP 8.6.0
- **iOS**：Swift/UIKit、CocoaPods、Xcode
- **KMP Shared**：Ktor（网络）、Coroutines（并发）、kotlinx-serialization（JSON）、kotlinx-datetime
- **Gradle**：8.9，Kotlin 2.2.21
- **JDK**：需要 JDK 21
## 项目定位
移动客户端壳工程，是用户直接接触的终端应用。主要职责：
1. **消息接收与展示**：通过厂商推送 + WebSocket 双通道接收消息
2. **厂商推送集成**：华为 HMS Push、荣耀 Push、OPPO Push、小米 Push、魅族 Push、VIVO Push、APNs
3. **WebView 桥接**：加载 H5 页面并提供原生能力（登录态注入、环境地址、Toast、跳转等）
4. **用户交互**：登录、扫码绑定、消息列表浏览、设置管理
## 详细功能
- **双通道推送**：`PushManager` 统一管理厂商推送与 WebSocket 长连接（`WsManager`）
- **WebView 桥接**：`WxpWebBridgeManager` 注册 action（`getLoginInfo`、`getEnvBaseUrl`、`showToast`
、`openUrl`、`payRequest` 等），部分 action 受白名单 host 限制（`WxpWebHostPolicy`）
- **H5 资源版本管理**：`AppFeVersionManager` 对比 `version.txt` 决定是否清理 WebView 缓存
- **环境切换**：`offline` flavor 提供 `TestPanelActivity`，可动态修改 `WxpConfig` 中的 API/WS/H5 地
址
- **MVP 架构**：Shared 层 `Contract + Presenter` 模式，跨 Android/iOS 共享业务逻辑
- **网络层**：`WxpNetworkService`（Ktor HttpClient），自动携带 `deviceToken`、`version`、`platform`
请求头
## 目录结构
```
wxpusher-app/
    build.gradle.kts              # 根构建（AGP + 华为/荣耀插件）
    settings.gradle.kts           # 模块声明 + 仓库配置
    gradle.properties             # JVM/Kotlin 编译参数
    gradle/
        libs.versions.toml        # 中心化版本管理
        wrapper/                  # Gradle 8.9 Wrapper
    shared/                       # KMP 跨平台共享模块
        build.gradle.kts          # KMP + CocoaPods + Serialization
        src/
            commonMain/           # 跨平台业务代码
                kotlin/.../
                    WxpConfig.kt              # 全局配置（baseUrl/wsUrl/appFeUrl）
                    api/WxpApiService.kt      # REST API 封装
                    base/common/              # 网络服务、数据服务、平台抽象
                    page/                     # MVP Presenter + Contract
                    web/                      # WebView 策略、H5 版本管理
            androidMain/          # Android 平台实现
            iosMain/              # iOS 平台实现 + cinterop
    androidApp/                   # Android 原生壳
        build.gradle.kts          # Android 构建配置
        debug.jks                 # Debug 签名
        src/
            androidMain/kotlin/.../
                app/              # Application 入口（WxPusherApplication）
                page/             # Activity/Fragment
                    web/bridge/   # WebView 桥接管理器 + handlers
                push/             # 厂商推送 + WS 长连接
                    ws/connect/   # WebSocket 连接管理（WsManager）
                config/           # 配置管理（ConfigManager）
                common/           # 常量（WxpConstants/WxpSaveKey）
                dialog/           # 弹窗
                utils/            # 工具类
            androidOffline/       # offline flavor（TestPanelActivity + 独立 Manifest）
            androidProd/          # prod flavor
    iosApp/                       # iOS 原生壳
        Podfile                   # CocoaPods（Toaster/MJRefresh/WechatOpenSDK/shared）
        Gemfile                   # Bundler 锁定
        wxpusher/
            WxPusher-iOS/         # App Delegate、Scene、Assets、Info.plist
            Page/                 # 各 ViewController + WebView 桥接
            Common/               # 工具与服务
            KtSwiftBridge/        # KMP ↔ Swift 互调桥接（.h/.m）
```
## 构建命令
```bash
cd wxpusher-app
./gradlew assembleOfflineDebug --no-daemon   # 构建 offline Debug APK
./gradlew assembleProdDebug --no-daemon      # 构建 prod Debug APK
./gradlew lint --no-daemon                   # Lint 检查
./gradlew test --no-daemon                   # 单元测试（当前无测试源码）
./gradlew check --no-daemon                  # 完整检查
```
## 注意事项
- Product Flavor 维度 `env`：`offline`（开发测试）和 `prod`（生产），开发使用 `offline`
- `compileSdk = 35`，`minSdk = 26`，`targetSdk = 34`，仅支持 `arm64-v8a`
- Debug 签名使用仓库内 `debug.jks`（密码 `smjcco`）；Release 签名需要 `secrets/android/key.propertie
s`
- Lint 会报 Kotlin metadata 版本不匹配警告（2.2.0 vs 2.0.0），不影响构建
- 当前无单元测试源码，`test` 任务全部 `NO-SOURCE`
- iOS 部署目标 `platform :ios, '13.0'`，通过 Xcode 工程构建，不走 Gradle
- `build.gradle.kts` 中 AGP 版本为 8.6.0，`libs.versions.toml` 中为 8.5.2，两处并存