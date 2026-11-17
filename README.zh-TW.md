# ThreadsPulse for Android

[English](README.md) | **繁體中文**

Meta Threads（`com.instagram.barcelona`）的 Android 增強模組與工具。目前版本：**1.27.0**

本專案為 iOS 知名增強插件 [ThreadsPulse](https://github.com/mumuisdog/ThreadsPulse) 的 Android 移植版本。支援 **Root（LSPosed）** 與 **免 Root（LSPatch / GitHub Actions 自動打包）** 雙模式。

---

## 功能特性

| 分類 | 功能 | 說明 |
|---|---|---|
| **廣告與隱私** | 去除廣告 | 隱藏首頁動態裡的贊助（Sponsored）串文與廣告插播 |
| | 停用截圖偵測 | 截圖或螢幕錄影時，Threads 不會偵測到，並移除畫面錄影限制 |
| | 阻擋分析回傳 | 阻擋應用程式收集並回傳瀏覽停留時間、點擊次數等分析紀錄 |
| | 移除網址追蹤碼 | 複製或分享串文時，自動去除 `xmt`、`igshid`、`utm_*` 等追蹤碼，並解開 `l.threads.com` 跳轉網址 |
| **媒體下載** | 內建下載按鈕 | 於串文選單直接呼叫 ThreadsPulse 下載全部圖片與高畫質影片 |
| | 獨立串文下載器 | 支援在 App 內貼上任何 Threads 網址，或從系統「分享」選單直接傳送至 ThreadsPulse 解析下載 |
| | 原始尺寸圖片 | 從 Threads 伺服器獲取最高解析度原圖，自動分類儲存至系統相簿 |
| | 高畫質影片 | 支援 DASH 串流與 Progressive MP4 影片解析（最高可達 1080p） |
| | 多檔案並行下載 | 單則多圖／多影片串文支援最多 4 個線程並行下載，快速完成 |
| | 專屬相簿分類 | 自動於裝置相簿中建立「Threads」專屬相簿進行分類管理 |
| **播放器** | 停用自動播放 | 瀏覽動態時影片不會自動播放，點選後才會開始播放 |
| | 預設播放速度 | 支援 1.0x / 1.25x / 1.5x / 2.0x 預設影片播放倍速調整 |
| **首頁動態** | 保留動態位置 | 從背景切回應用時，首頁維持在原有捲動位置，不重新整理、不強制跳回頂部 |
| | 停用下拉重新整理 | 避免在動態頂端滑動時誤觸下拉重新整理 |
| | 標籤點選控制 | 點選底部首頁標籤時維持原有瀏覽位置，不捲回頂部且不觸發刷新 |
| **推薦內容** | 隱藏推薦帳號 | 隱藏首頁動態與個人檔案頁面的「為你推薦」推薦卡片 |
| | 隱藏追蹤建議 | 隱藏搜尋與探索頁面中的「追蹤建議」與建議對象 |
| | 隱藏興趣詢問 | 不再彈出「你對此串文是否有興趣？」的調查提示 |
| **私訊隱私** | 不顯示看過 (Ghost) | 查看私訊對話時不發送已讀回條（對方無法得知你已看過） |
| | 不顯示輸入中 | 輸入文字時對方不會看見正在輸入的「…」動畫 |
| | 回覆後標記已讀 | 可選模式：僅在正式發送回覆後才傳送已讀狀態 |
| **介面與互動** | 按讚確認 | 點擊愛心時彈出確認提示，防止手滑誤點按讚 |
| | 開啟外部瀏覽器 | 點擊串文內之連結時，改用系統預設瀏覽器開啟 |
| | 長按首頁鍵開啟設定 | 在 Threads 底部導覽列長按首頁圖示即可直達 ThreadsPulse 設定介面 |
| | 備份與還原 | 支援將所有偏好設定匯出為 JSON 格式，換機或重裝時一鍵匯入 |

---

## 🎨 主題配色

與 iOS 版本完全一致，提供 6 款精美主題配色：
- 珊瑚 (Coral)
- 海洋 (Ocean)
- 薄荷 (Mint)
- 薰衣草 (Lavender)
- 櫻花 (Sakura)
- 琥珀 (Amber)

可在 ThreadsPulse 應用內隨時切換。

---

## 🚀 安裝與使用方式

### 方式一：Root 用戶（推薦 LSPosed）

1. 確保手機已安裝 Magisk / KernelSU / APatch，並啟用 **LSPosed**。
2. 從 GitHub Releases 下載並安裝最新版 `ThreadsPulse-Release.apk`。
3. 開啟 LSPosed 管理器，勾選啟用 **ThreadsPulse** 模組，並將作用範圍設定為 **Threads (`com.instagram.barcelona`)**。
4. 強制停止 Threads 應用後重新打開，即可開始使用。

---

### 方式二：免 Root 用戶（GitHub Actions 自動編譯打包）

本專案支援使用 GitHub Actions 自動將 ThreadsPulse 模組注入至 Threads 官方 APK 中（透過 LSPatch）：

1. 在本倉庫頁面進入 **Actions** 分頁。
2. 選擇左側工作流 **Build Patched Threads APK (LSPatch Non-Root)**。
3. 點選右側 **Run workflow**：
   - 可在輸入框填入已解密/乾淨的 Threads 官方 APK 下載網址（例如來自 APKMirror 等可直接下載之直連網址）。
   - 或者在倉庫的 **Settings → Secrets and variables → Actions** 中新增 Secret `THREADS_APK_URL`。
4. GitHub Actions 會自動完成模組編譯、LSPatch 注入與 APK 簽名，並在 Artifacts 產生可直接安裝的 `Threads_Pulse_Patched.apk`。
5. 下載並安裝該 APK 即可在免 Root 環境下使用完整功能。

---

### 方式三：本機手動使用 LSPatch（免 Root）

1. 下載官方 Threads APK 與 `ThreadsPulse-Release.apk`。
2. 在手機上安裝 [LSPatch](https://github.com/LSPosed/LSPatch)（或使用 Shizuku）。
3. 選擇「修補應用程式」→ 選擇 Threads APK → 嵌入模組選擇 `ThreadsPulse`。
4. 產生修補後的 APK 並安裝即可。

---

## 🛠️ 本機編譯

需求環境：JDK 17、Android SDK 34。

```bash
# Clone 倉庫
git clone https://github.com/csie1b/ThreadsPulse-Android.git
cd ThreadsPulse-Android

# Linux / macOS
chmod +x ./gradlew
./gradlew assembleRelease

# Windows (PowerShell)
.\gradlew.bat assembleRelease
```

編譯完成的 APK 位於 `app/build/outputs/apk/release/`。

---

## 📄 開源協議與聲明

- 本專案僅供個人學習與 Android 逆向開發技術研究使用。
- 靈感與功能設計致敬 iOS 著名插件 [ThreadsPulse](https://github.com/mumuisdog/ThreadsPulse)。
- 本專案與 Meta 或 Instagram 無任何商業附屬關係。
