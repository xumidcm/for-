# RootWebViewDemo

Android 8.1+（API 27）单机 WebView Demo：

- WebView 加载 `assets/index.html`
- JS 暴露全局函数 `root_cmd(mycmd)`
- 原生使用 `su -c` 执行命令
- 返回数组：`[stdout, stderr, statusCode]`
- 默认 Demo 执行 `whoami`

## 关键文件
- `app/src/main/java/com/example/rootwebviewdemo/MainActivity.java`
- `app/src/main/java/com/example/rootwebviewdemo/JsBridge.java`
- `app/src/main/java/com/example/rootwebviewdemo/RootShellExecutor.java`
- `app/src/main/assets/index.html`
- `.github/workflows/android-pr-build.yml`

## 构建
本仓库 CI 在 PR 上执行：

```bash
gradle :app:assembleDebug
```

> 说明：命令执行依赖设备 ROOT 授权；未 ROOT 设备会返回错误信息与非 0 状态码。
