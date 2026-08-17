# P001 脱敏与模型准备

脚本读取现有患者 MAT 文件，生成只保存在本机、可复现的 P001 回放集。
`private_data/` 已加入 `.gitignore`，不得提交或上传。

```powershell
backend\.venv\Scripts\python.exe tools\data_prep\create_p001.py `
  --source "E:\生医工大赛\患者数据" `
  --long-source "E:\生医工大赛\生医工大赛线下评选\实时采集\处理文件_去伪迹" `
  --output private_data\p001

$env:PYTHONPATH=(Get-Location).Path
backend\.venv\Scripts\python.exe tools\data_prep\train_p001_model.py `
  --dataset private_data\p001 `
  --output private_data\p001\model_v1.json

backend\.venv\Scripts\python.exe tools\data_prep\publish_model.py `
  --server http://127.0.0.1:8000 `
  --username doctor `
  --password "修改后的医生密码"
```

输出包括四状态均衡片段、连续场景、校验清单、脱敏检查报告和版本化模型。
清单与报告不包含源文件名、原始日期、医院名称或源路径。输出仍属于科研数据，
未经新的授权不得公开发布。
