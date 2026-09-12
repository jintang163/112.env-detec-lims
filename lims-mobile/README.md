# LIMS 移动作业端（uni-app + Vue3）

面向采样员/现场人员：采样任务、委托详情、百度地图采样打点（BD-09）、离线缓存、移动审批、uCharts 看板。

## 运行

```bash
npm install
npm run dev:h5            # H5 调试 http://localhost:5174
npm run build:h5
npm run dev:mp-weixin     # 微信小程序, 用微信开发者工具打开 dist/dev/mp-weixin
# App: 用 HBuilderX 打开本目录运行(manifest 中配置百度地图 Android/iOS AK)
```

## 目录

```
src/
├── pages/login        登录
├── pages/tabbar       工作台(uCharts)/采样任务/客户/我的
├── pages/entrust      委托详情(检测项/点位/采样完成)
├── pages/sampling     地图打点采集(H5 百度WebGL, App/小程序原生map)
├── pages/approval     移动审批
├── utils/request.ts   uni.request 封装(JWT/401/离线缓存降级)
├── utils/coord.ts     WGS84→GCJ02→BD09
├── utils/db.ts        SQLite(plus.sqlite) + storage 降级
└── stores/user.ts     Pinia
```

## 离线策略

- 现场采集的点位先写入本地（App: SQLite 表 `offline_point`；H5/小程序: uni.storage），联网后在地图页「同步」批量上传。
- 任务/客户/详情接口开启 `offlineCache`，请求失败自动回退最近一次缓存并提示「当前为离线数据」。
