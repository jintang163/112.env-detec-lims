<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { initDb } from '@/utils/db'

onLaunch(() => {
  // App 端启动时打开/创建离线 SQLite 库
  // #ifdef APP-PLUS
  initDb()
  // #endif
  console.log('LIMS 移动作业端启动')
})
</script>

<style lang="scss">
page {
  background: #f5f6fa;
  font-size: 28rpx;
}
.container { padding: 24rpx; }
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.row { display: flex; align-items: center; }
.between { justify-content: space-between; }
.muted { color: #999; font-size: 24rpx; }
.urgent { color: #fff; background: #f5222d; border-radius: 6rpx; padding: 2rpx 12rpx; font-size: 22rpx; }
.tag { display: inline-block; padding: 2rpx 14rpx; border-radius: 8rpx; font-size: 22rpx; }
.tag-blue { background: #e6f4ff; color: #1677ff; }
.tag-green { background: #f6ffed; color: #52c41a; }
.tag-orange { background: #fff7e6; color: #fa8c16; }
.tag-gray { background: #f5f5f5; color: #888; }
.tag-cyan { background: #e6fffb; color: #08979c; }
</style>
