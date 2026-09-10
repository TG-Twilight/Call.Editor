# 原生工程构建说明

项目介绍与功能见 [简体中文 README](../README.md)。应用包名 `com.twilight.calleditor`。版本配置以 `app/build.gradle.kts` 为准，安装包与更新说明见 [GitHub Releases](https://github.com/TG-Twilight/Call.Editor/releases)。

## 开发构建

需要完整 JDK 21、Android SDK Platform 36、Build Tools 37.0.0。通过 `ANDROID_HOME` 或本机 `local.properties` 配置 SDK。Windows 示例：

```powershell
./gradlew.bat :app:testReleaseUnitTest :app:assembleRelease :app:lintRelease
```

Linux/macOS 使用 `./gradlew`。第一次下载 Gradle 和依赖需要联网；缓存就绪后可加 `--offline`。标准构建输出 `app/build/outputs/apk/release/app-release-unsigned.apk`，不自动使用 Debug 证书。

Gradle 版本见 `gradle/wrapper/gradle-wrapper.properties`；插件、依赖及 SDK 配置见 `build.gradle.kts` 和 `app/build.gradle.kts`。

## 维护者签名（Windows）

`build.ps1` 执行 release 测试、构建、lint、16 KB zipalign、签名、证书校验和 SHA-256 记录，输出到仓库根目录的 `dist/`。默认离线；`-Online` 允许 Gradle 下载依赖，但不会自动设置 Java 代理。

配置 `REVIA_KEYSTORE` 和 `REVIA_KEY_ALIAS`，或在被 Git 忽略的 `native/signing.local.properties` 中设置 `KeystorePath` 与 `KeyAlias`。密码只从 Windows 用户级环境变量 `REVIA_KS_PASS` 读取，不写入配置文件。

```powershell
# 在仓库根目录执行；本机签名信息须事先配置。
& ./native/build.ps1
```

脚本优先使用标准安装目录下 Android Studio 的完整 JBR，否则使用 `JAVA_HOME`。签名工具从 `ANDROID_HOME`、`ANDROID_SDK_ROOT` 或 Windows 默认 SDK 安装目录解析，固定使用 Build Tools 37.0.0。

正式证书 SHA-256：`EA385AFC82E19824EEA8A8868ED365DF03E9886BBBA33E47E1E04B43EC65CB67`。脚本会拒绝其他证书。仓库不分发维护者密钥；本地未签名构建不需要密钥。

## 数据兼容

系统 CallLog 是记录来源。编辑只更新变化字段；恢复追加并去重。包名调整不修改备份协议标识，仍使用 `com.android.calleditor.calllog`、version 1。

已确认的六种特殊类型统一使用中文名称：呼入、未接、拒接可带黑名单归类；黑名单号码、骚扰电话、广告推销为拦截分类，不能据此判断当时是否接通。列表支持分类筛选，编辑页可选择这些类型，名称不受机型限制；只有尚未确认含义的类型才显示原始数值。修改记录分类不会修改系统黑名单。

小布代接的扩展字段读取与编辑目前适配 RMX5200 / Android 16，独立依据 `features` 中的 `0x20000000` 和非空 `virtual_call_id` 识别。已有可读标志及关联编号的记录可切换代接标记，保留其他位与关联编号，不生成或验证小布内容；修改其他字段时不写入这些扩展列。基本字段备份仍不包含小布标记、关联编号、文字或录音，不能用于完整恢复小布通话。

新包名与早期 `com.android.calleditor` 是两个独立应用，不覆盖迁移私有设置。不得以卸载或清数据方式处理升级问题。

写入中的配置重建／系统强杀、跨设备电话账户映射及更多设备兼容仍需完善。短信功能尚未实现。
