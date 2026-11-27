# HyperControl

面向小米 HyperOS 的 Xposed 模块，将功能开关直接集成到系统设置中，无需独立桌面入口。

**当前版本：`1.0.0-202601127` · versionCode：`1` · 架构：`arm64-v8a`**

## 功能

### 状态栏

- 隐藏媒体灵动岛、移动数据活动指示器、Wi-Fi 活动指示器与 Wi-Fi 标准数字。
- 连接 Wi-Fi 时隐藏移动网络类型。
- 交换移动信号与 Wi-Fi 图标的位置。
- 自定义移动网络类型文本。

### 系统框架

- 允许截屏受保护内容。
- Passkey 兼容性修复。
- 去除安全音量限制。
- 核心破解 N：提供应用降级、签名与安装校验等高级开关，具体选项及风险以模块页面提示为准。

### 应用功能

- **微信**：平板设备伪装、选择图片时自动勾选原图、禁止发送正在输入状态、实验性屏蔽朋友圈广告。
- **电量与性能**：禁止杀应用、停止云控同步。
- **HTML 查看器**：禁用云控更新。
- **下载管理**：阻止迅雷日志及相关目录创建。
- **小米服务框架**：解锁焦点认证。

### 设置入口

提供部分系统隐藏页面、应用广告设置页面与 Google 设置的快捷入口。快捷入口是否可用取决于 ROM 和已安装应用，**不等同于自动关闭全部广告**。

## 使用要求

- 小米 HyperOS 系统；项目最低 Android 版本为 **Android 15 / API 35**。
- **ARM64** 设备，当前 APK 仅包含 `arm64-v8a` 原生库。
- 支持 **libxposed API 102** 的 Xposed 框架；仅支持旧版 API 的框架不适用。
- 模块内停止应用、重启 SystemUI、重启设备等操作需要 Root 授权。

模块依赖系统及应用内部实现，不能保证适配所有 HyperOS、微信或系统应用版本。DexKit 用于部分目标的定位，不代表 ROM 升级后一定兼容。

## 安装与启用

1. 从本仓库的 **Releases** 下载 `HyperControl-1.0.0-202601127-release.apk` 并安装。
2. 在支持的 Xposed 管理器中启用 HyperControl，检查相关作用域。
3. 重启设备，进入 **设置 → HyperControl**；入口位于“我的设备”之后。
4. 按需开启功能，再按页面提示重启目标应用或设备。系统框架相关功能建议重启设备后验证。

模块没有独立的桌面启动 Activity。若设置中没有入口，请先检查模块是否加载、系统设置作用域及框架日志。

### 作用域

| 目标 | 包名 / 元数据标识 | 用途 |
| --- | --- | --- |
| 系统框架 | `system`（运行时包名 `android`） | 系统框架功能、Passkey |
| 系统设置 | `com.android.settings` | 模块入口与配置、Passkey |
| 系统界面 | `com.android.systemui` | 状态栏相关功能 |
| 电量与性能 | `com.miui.powerkeeper` | 后台与云控 |
| HTML 查看器 | `com.android.htmlviewer` | 云控更新 |
| 下载管理 | `com.android.providers.downloads` | 迅雷日志目录 |
| 手机管家 | `com.miui.securitycenter` | Passkey 相关适配 |
| 扫一扫 | `com.xiaomi.scanner` | Passkey 相关适配 |
| 小米服务框架 | `com.xiaomi.xmsf` | 焦点认证 |
| 微信 | `com.tencent.mm` | 微信功能 |

Passkey 功能需同时启用**系统框架、系统设置、手机管家、扫一扫**四个作用域，并重启设备；相关 Google 凭据功能还依赖可用的 Google Play 服务。

## 风险与限制

- 建议先备份数据，一次只调整少量开关，保留在恢复环境中停用模块的手段。
- 签名、共享用户、安装校验等开关会降低系统安全性，不建议无差别全部开启。
- “允许系统应用使用隐藏 API”在页面中已明确警告：**小米 / Redmi 勿开，可能卡开机**。
- 禁止杀应用可能增加耗电；取消安全音量限制后请自行控制音量。
- 微信适配受客户端版本影响，朋友圈广告屏蔽为实验功能，不保证全部生效。
- 本次版本号重置为 `1`。从更高 versionCode 的旧版本更新时，系统可能拒绝覆盖安装；请先备份，再自行决定是否卸载旧版。
- 本次 Release APK 使用 **AOSP testkey** 签名，属于公开测试密钥，不应将其当作专属的发布身份或安全凭据；旧版本签名不一致时也可能无法覆盖安装。

## 从源码构建

### 工具链

- JDK 21。
- Android SDK Platform 35、Build Tools 35.0.0。
- Gradle Wrapper 8.11.1、Android Gradle Plugin 8.9.2、Kotlin 2.1.20。
- 当前配置：`compileSdk = 35`、`minSdk = 35`、`targetSdk = 37`。

设置 `ANDROID_HOME`，或在未纳入版本控制的 `local.properties` 中填写 SDK 路径。首次构建需要联网下载依赖。

仓库当前的 `gradlew` 包含本地 JDK 路径；在其他机器上可以直接使用 Wrapper 主类，避免依赖该路径：

```sh
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/android-sdk
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain :app:assembleRelease
```

ARM64 Linux 主机需要可执行的 AAPT2，必要时追加 `-Pandroid.aapt2FromMavenOverride=/path/to/aapt2-wrapper`。

默认 Release 产物位于 `app/build/outputs/apk/release/`，未配置发布签名；自行分发前请使用自己的密钥完成对齐、签名及验证。不要把私钥、访问令牌或 `local.properties` 提交到仓库。

## 源码结构

```text
app/src/main/java/com/cinexaft/hypercontrol/
├── base/                   模块入口、设置集成、配置页面与重启服务
├── hook/
│   ├── downloads/          下载管理
│   ├── html/               HTML 查看器
│   ├── power/              后台与云控
│   ├── systemframework/    系统框架、Passkey 与核心破解
│   ├── systemui/           状态栏
│   ├── wechat/             微信
│   └── xms/                小米服务框架
└── ui/                     功能页面与快捷入口
```

- 应用 ID：`com.cinexaft.hypercontrol`
- Xposed 入口：`com.cinexaft.hypercontrol.base.Entry`
- 模块元数据：`app/src/main/resources/META-INF/xposed/`
- `device-stubs/`：编译所需的宿主接口桩。
- 功能配置主要存储于 `Settings.Global`，键名前缀为 `hypercontrol_`。

## 问题反馈

反馈时请提供设备型号、ROM / Android 版本、Xposed 框架及 API 版本、相关应用版本、复现步骤和日志。提交日志前请移除账号、令牌及其他个人信息。构建成功不代表所有功能已完成实机验证。

## 许可证

GNU General Public License v3.0，详见 `LICENSE`。
