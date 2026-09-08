# 资源说明

- 应用截图：本项目自行拍摄，仅展示无私人数据的页面。
- `native/app/src/main/res/drawable-nodpi/avatar_openai.png`：下载自 [OpenAI 的 GitHub 公开头像](https://github.com/openai.png?size=128)，用于 GPT-6-Astra 贡献者条目的机构头像。图形标识属于其权利人，不表示 OpenAI 为本项目提供官方背书。
- 应用内电话等图标：本项目的 Android VectorDrawable／Compose ImageVector。
- 桌面图标：复用应用内 `AppIcon.Calls` 电话听筒路径，采用纯色背景与独立实心前景；普通自适应图标随系统浅／深色切换，Android 12 起使用系统 Monet 配色，Android 13 起提供供桌面主题取色的单色层。桌面图标不受应用内主题开关控制。
- 关于页九瓣装饰形状：使用 AndroidX Compose Material3 `MaterialShapes.Cookie9Sided`。原形状来源为 [AndroidX MaterialShapes](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialShapes.kt)，版权归 Android Open Source Project，遵循 [Apache License 2.0](APACHE-2.0.txt)。

截图共用 `readme/screenshots/`；README 翻译分别为 `README_en-US.md` 和 `README_zh-TW.md`。README 翻译不代表应用界面已经支持对应语言。
