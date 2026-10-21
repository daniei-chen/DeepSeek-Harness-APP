# DeepSeek Harness APP 插件

合并后的单一插件包：

- Android WebView 手机适配层（抽屉、安全区、输入法、返回栈、图片直传、语音兜底）
- Host 文件操作 `/dsh-file-ops`（下载、zip、递归删除）
- BrowserSkill 的 `browser_*` 浏览器自动化工具

主题皮肤 `dsh-dream-skin` 保持独立，不并入此包。

## 安装

```bash
# 在 DSH profile 目录安装 tarball
pnpm add file:/path/to/dsh-android-deepseek-harness-app-plugin-0.1.0.tgz
```

把 `@dsh-android/deepseek-harness-app-plugin` 加入 profile 的 `dsh.profile.bundles`，并停用原来的：

- `@dsh-android/dsh-client-ui-responsive-bundle`
- `@dsh-android/dsh-file-ops-bundle`
- `@dsh-android/dsh-client-ui-responsive`
- `@wxg-prc-cpg/browser-skill-dsh-plugin`

重启 DSH 后，插件页只保留一个 **DeepSeek Harness APP 插件** 卡片。

BrowserSkill 的 host 配置（如 `bskPath`）通过本插件 config 传入；原来的浏览器工具名不变。
