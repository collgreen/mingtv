# MINGTV

基于 [TVBoxOS-Mobile](https://github.com/takagen99/Box) 二次开发的手机看电视应用，完全免费。

---

## 功能特性

- 点播、直播订阅，支持多种订阅格式
- 内置多播放器（系统 / IJK / ExoPlayer）
- 广告过滤
- 直播 EPG 节目表
- DLNA 投屏
- 深色 / 浅色主题跟随系统
- **App 热更新**：每次启动自动检测新版本，有更新弹窗提示
- **GitHub 代理**：内置 ghproxy.com 等镜像，国内直连 GitHub 不再卡顿

---

## 快速上手

### 安装

前往 [Releases](https://github.com/collgreen/mingtv/releases) 下载最新 APK 安装。

国内下载慢可以用代理加速（在 App 设置里也可以配置）：
```
https://ghproxy.com/https://github.com/collgreen/mingtv/releases/download/v1.2/MINGTV_v1.2_release.apk
```

### 添加订阅（点播）

在 App 首页右上角 → 订阅管理 → 添加，填入订阅地址。

### 添加直播源

设置 → 直播源，填入 m3u 或 m3u8 地址。

---

## 热更新说明

### App 版本更新

应用每次启动时会自动请求仓库根目录的 `update.json`，与本地 `versionCode` 比较。有新版本则弹出提示，点击下载跳转浏览器。

**发布新版本操作步骤：**

1. 打包新 APK，上传到 GitHub Releases
2. 修改仓库根目录的 `update.json`：

```json
{
  "versionCode": 33,
  "versionName": "1.3",
  "changelog": "修了几个闹不清楚的 bug，顺手优化了一下启动速度",
  "downloadUrl": "https://github.com/collgreen/mingtv/releases/download/v1.3/MINGTV_v1.3_release.apk"
}
```

3. 同步修改 `app/build.gradle` 里的 `versionCode` 和 `versionName`

> `versionCode` 必须是整数且比旧版本大，App 以此判断是否需要更新。

---

### 点播 / 直播配置热更新

把 JSON 配置文件放到仓库（如 `tvbox/config.json`），通过 raw 链接使用：

**直连（境外 / 科学上网）：**
```
https://raw.githubusercontent.com/collgreen/mingtv/main/tvbox/config.json
```

**国内加速（推荐）：**
```
https://ghproxy.com/https://raw.githubusercontent.com/collgreen/mingtv/main/tvbox/config.json
```
```
https://mirror.ghproxy.com/https://raw.githubusercontent.com/collgreen/mingtv/main/tvbox/config.json
```

在 App 的"订阅管理"页填入上面的地址即可。修改仓库里的 JSON 文件后，App 重新加载订阅就能取到最新内容，**不需要重新安装**。

---

## GitHub 代理设置

「设置 → GitHub代理」里可选代理节点，主要影响 App 内的更新检测和下载地址跳转。

| 选项 | 适合人群 |
|------|---------|
| 直连 | 境外 / 已科学上网 |
| ghproxy.com | 国内常用，一般够用 |
| mirror.ghproxy.com | ghproxy 挂了时备用 |
| gh.api.99988866.xyz | 再备用 |

> 订阅地址里的代理前缀需要**用户自己手动拼接**（见上方说明），设置里的代理选项不会自动替换已保存的订阅地址。

---

## 构建

```bash
# 需要 Android Studio 或 JDK 8 + Android SDK
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/MINGTV_v<版本>_release_<日期>.apk`

- minSdk 24（Android 7.0）
- targetSdk 30
- 当前只打 arm64-v8a 包

签名用的是仓库里的 `TVBoxOSC.jks`，密码在 `app/build.gradle`。**正式对外发布建议换成自己的签名。**

---

## 免责声明

本软件完全免费，不提供、不制作、不收集任何视频内容或订阅源。用户自行添加的订阅地址与开发者无关，由此产生的法律责任由用户自行承担。

本软件不采集任何用户数据，仅供个人学习和技术研究使用，禁止用于商业用途。

---

## License

基于上游开源项目修改，遵循原项目开源协议。
