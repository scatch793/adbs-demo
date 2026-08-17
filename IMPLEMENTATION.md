# Ominidapt PD 科研演示闭环

本仓库现在包含主 Android APK、Windows BLE 外设模拟器、FastAPI 服务、数据库模型、
对象存储适配、BLE 协议、脱敏数据工具和端侧算法。它只用于科研演示，不具备临床用途，
也没有任何真实植入设备适配器。

## 目录

- `app/`：单 APK 多角色主应用。身份由服务器返回，包含 Room 离线队列、
  Keystore 令牌、REST、患者/医生/管理员真实入口、BLE 中央端和端侧推理。
- `tools/ble_pc/`：电脑端可视化 BLE 外设，使用脱敏 P001 数据模拟植入设备。
- `simulator/`：保留的 Android 外设模拟器，不参与当前平板—电脑演示。
- `protocol/`：共享 OP v2 帧、分片/重组、CRC16、阻抗、遥测、LFP、场景、参数与 ACK。
- `backend/`：FastAPI、SQLAlchemy、Alembic、Argon2id、JWT、RBAC、WebSocket、
  算法、六种文件导出和审计。
- `deploy/`：PostgreSQL、Redis、MinIO、API 和 Celery Worker 的 Compose 配置。
- `tools/data_prep/`：P001 脱敏、连续场景生成与五维 GMM 训练。

## 1. 启动本机服务

无需 Docker 的快速模式：

```powershell
cd E:\Android\Project\backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e ".[test]"
.\.venv\Scripts\python.exe -m alembic upgrade head
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Docker 模式：

```powershell
cd E:\Android\Project\deploy
Copy-Item .env.example .env
# 先修改 .env 中的全部密码和密钥
docker compose --env-file .env -f docker-compose.yml up --build
```

浏览器访问 `http://127.0.0.1:8000/docs`。开发默认账号为 `admin`、`doctor`、
`patient`，临时密码分别为 `Admin-ChangeMe-2026`、
`Doctor-ChangeMe-2026`、`Patient-ChangeMe-2026`。首次登录必须改密。

## 2. 构建与安装 APK

```powershell
$env:JAVA_HOME="C:\Users\戴佳锦\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
$env:GRADLE_USER_HOME="E:\Android\Project\.gradle-user-home"
.\gradlew.bat :app:assembleDebug
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
主应用登录页中，模拟器使用 `http://10.0.2.2:8000`；真机使用电脑的局域网
IPv4 地址，例如 `http://192.168.1.20:8000`。Windows 防火墙需要允许 8000
端口。Debug 允许局域网 HTTP，Release 清单明确禁止明文流量。

## 3. Windows BLE 模拟器

```powershell
cd E:\Android\Project\tools\ble_pc
.\setup_simulator.ps1
.\run_simulator.ps1
```

可双击版本位于：

```text
tools/ble_pc/dist/Ominidapt-PD-Simulator/Ominidapt-PD-Simulator.exe
```

程序启动后自动广播固定 Ominidapt GATT 服务。界面可控制药物效应、运动强度、
四状态、模拟刺激响应、阻抗、电量、丢包、CRC、ACK拒绝和主动断连。原始身份、
日期、医院和源路径不会进入广播、日志或导出。

## 4. 平板—电脑演示顺序

1. 电脑启动 Docker 服务和 Windows 模拟器；电脑必须支持 BLE Peripheral。
2. 平板安装主 APK，使用医生或患者账号登录并按要求修改临时密码。
3. 医生或患者设备页连接“科研模拟设备”。应用只扫描固定服务 UUID，并读取设备信息；
   `simulated=true` 和 `clinicalUse=false` 未同时满足时立即断开。
4. 模拟器开始回放后，主手机按序号重建双通道 256 Hz 数据；CRC 错误、丢包、
   断线和重连次数都显示在患者首页。
5. 30 个窗口预热后，手机每秒进行五维特征、GMM、概率平滑和拒识，并通过
   Room 事件 ID 离线入队。恢复网络后 WorkManager 只补传一次。
6. 医生端可执行四状态初始化。演示模式每状态稳定5秒、采集30秒；科研模式稳定
   15秒、采集180秒。服务器实际计算 Fisher 频段、训练5维模型并返回质量报告。
7. 医生账号登录后选择患者，审核初始化模型和 `submitted` 参数建议。只有服务端安全规则通过且
   医生批准的建议才变成模拟命令。
8. 当前登录角色在已连接平板上轮询批准命令，再做一次端侧边界检查后写入模拟器；只有成功 ACK
   才回写 `acknowledged`，拒绝或 8 秒超时回写 `failed`。
9. 医生端可生成并下载 PDF、CSV、MAT、EDF、EML、ZIP 六种真实文件。

## 5. 自动验证

```powershell
cd E:\Android\Project\backend
.\.venv\Scripts\python.exe -m pytest

cd E:\Android\Project
.\tools\test_all.ps1
```

自动测试覆盖角色隔离、四状态初始化与审核、Fisher频段、幂等补传、安全拒绝、
批准/模拟下发/ACK、聊天、波形上传、六种导出、GP+Matérn+EI、协议分片黄金帧、
CRC 和 Python—Kotlin 特征/GMM 黄金向量。

30 分钟真实 BLE 连续性、系统通知权限、厂商后台限制和断线重连仍必须在目标
平板与电脑上实测；这类硬件验收不能由 JVM 测试替代。
