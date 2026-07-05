# MyFirstGisApp

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/ArcGIS_SDK-100.15.6-1A73E8" />
  <img src="https://img.shields.io/badge/Min_SDK-26-orange" />
  <img src="https://img.shields.io/badge/License-MIT-green" />
</p>

<p align="center">
  A mobile GIS application built with ArcGIS Runtime SDK for Android, featuring POI visualization, spatial query, and interactive map tools.
  <br/>
  基于 ArcGIS Runtime SDK 开发的 Android 移动端 GIS 应用，实现 POI 可视化、空间查询与交互式地图工具。
</p>

---

## English

### Features

| Feature | Description |
|---|---|
| **POI Visualization** | Displays 8 categories of POIs (landmarks, traffic, commercial, shopping, dining, business, lodging, living services) with color-coded markers |
| **Layer Control** | Toggle individual POI layers on/off via a collapsible panel |
| **Basemap Switching** | Switch between Topographic, Imagery, Streets, and Dark Gray basemaps |
| **POI Search** | Filter POIs by keyword; auto-navigates to the first match |
| **Tap to Identify** | Tap any POI to view its full attribute details in a bottom sheet |
| **Statistics Dashboard** | Count POIs in the current viewport, grouped by category, with a proportion bar chart |
| **Map Bookmarks** | Long-press anywhere to drop a bookmark pin; tap it to fly-to or delete |
| **My Location** | Real-time GPS location display with one-tap centering |

### Tech Stack

- **Language**: Kotlin
- **GIS SDK**: ArcGIS Runtime SDK for Android 100.15.6
- **UI**: ConstraintLayout · CardView · Material Bottom Sheet · FAB
- **Min SDK**: API 26 (Android 8.0)

### Getting Started

#### Prerequisites

- Android Studio Hedgehog or later
- Android device / emulator (API 26+)
- An [ArcGIS Developer](https://developers.arcgis.com/) account with a valid API key

#### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/tyxo0o315/MyFirstGisApp.git
   ```

2. Open the project in Android Studio

3. Create `local.properties` in the project root (if it does not exist) and add your key:
   ```properties
   arcgis.api.key=YOUR_ARCGIS_API_KEY_HERE
   ```

4. Sync Gradle, then **Run** on your device

### Project Structure

```
app/src/main/
├── java/com/example/myfirstgisapp/
│   └── MainActivity.kt          # All app logic
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── bottom_sheet_poi.xml
    │   └── bottom_sheet_stats.xml
    ├── drawable/                 # Vector icons & shape backgrounds
    └── values/                  # Colors, strings, themes
```

---

## 中文

### 功能介绍

| 功能 | 说明 |
|---|---|
| **POI 可视化** | 展示 8 类 POI（地名地址、通行设施、商务住宅、购物服务、餐饮服务、公司企业、住宿服务、生活服务），不同颜色圆点区分类别 |
| **图层控制** | 可折叠面板独立开关每个 POI 图层 |
| **底图切换** | 支持地形图、卫星图、街道图、暗色图四种底图 |
| **POI 搜索** | 关键词过滤，自动定位到第一个匹配结果 |
| **点击识别** | 点击地图上的 POI，底部弹出详情卡片（名称、地址、电话等） |
| **统计仪表盘** | 统计当前视野内各类别 POI 数量，并以比例条形图展示 |
| **地图书签** | 长按地图任意位置添加书签图钉；点击图钉可飞行定位或删除 |
| **实时定位** | 显示 GPS 实时位置，点击 FAB 一键居中 |

### 技术栈

- **开发语言**：Kotlin
- **GIS 框架**：ArcGIS Runtime SDK for Android 100.15.6
- **UI 组件**：ConstraintLayout · CardView · Material Bottom Sheet · FAB
- **最低支持**：Android API 26（Android 8.0）

### 快速开始

#### 前置要求

- Android Studio Hedgehog 或更高版本
- Android 真机 / 模拟器（API 26+）
- [ArcGIS 开发者账号](https://developers.arcgis.com/) 及有效 API Key

#### 配置步骤

1. 克隆仓库
   ```bash
   git clone https://github.com/tyxo0o315/MyFirstGisApp.git
   ```

2. 用 Android Studio 打开项目

3. 在项目根目录新建 `local.properties`（如已存在则追加），填入你的 API Key：
   ```properties
   arcgis.api.key=YOUR_ARCGIS_API_KEY_HERE
   ```

4. Sync Gradle 后，运行到设备即可

### 安全说明

ArcGIS API Key 通过 `local.properties` 注入 `BuildConfig`，该文件已加入 `.gitignore`，**不会上传至版本库**，可放心使用。

---

## License

MIT © 2026 [tyxo0o315](https://github.com/tyxo0o315)
