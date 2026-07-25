# Ominidapt PD

Ominidapt PD 是一个基于 Jetpack Compose 的帕金森病 aDBS 患者端与医生端演示应用。项目在同一 Android 应用中提供患者症状反馈、数据报告、远程诊疗，以及医生患者管理、初始化调参、文件导出和实时观测流程。

## 技术栈

- Kotlin 2.2
- Jetpack Compose / Material 3
- Android Gradle Plugin 9.3
- `minSdk 24` / `targetSdk 36`

## 本地构建

1. 使用 Android Studio 打开项目根目录。
2. 在 `local.properties` 中配置本机 Android SDK。
3. 执行：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 验证

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat compileDebugAndroidTestKotlin
.\gradlew.bat testDebugUnitTest
```

界面设计与动效实现以 `app/src/main` 下的 Compose 代码为准。
