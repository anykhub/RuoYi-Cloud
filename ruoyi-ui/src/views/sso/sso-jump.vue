<template>
  <div class="app-container">
    <el-card class="box-card" shadow="hover" @click.native="jumpToBSystem">
      <div slot="header" class="clearfix">
        <span>B 系统快捷入口</span>
      </div>
      <div class="text item">
        点击此卡片，自动登录并跳转至 B 系统。
      </div>
    </el-card>
  </div>
</template>

<script>
// 假设这是封装好的 request 或者是 API 函数
import request from '@/utils/request'

export default {
  name: 'SsoJump',
  data() {
    return {
      // 假设 B 系统的地址，实际项目中可能从环境变量或配置中获取
      bSystemUrl: process.env.VUE_APP_B_SYSTEM_URL || 'http://localhost:8081/b-system/login-sso'
    }
  },
  methods: {
    async jumpToBSystem() {
      try {
        // 请求 A 系统的后台生成短效 Token
        const res = await request({
          url: '/sso/generate-token',
          method: 'post'
        })

        if (res.code === 200 && res.data) {
          const token = res.data;
          // 拼接带有 token 的 B 系统地址
          const targetUrl = `${this.bSystemUrl}?token=${token}`;

          // 在新标签页打开
          window.open(targetUrl, '_blank');
        } else {
          this.$message.error('获取 SSO Token 失败');
        }
      } catch (error) {
        console.error('SSO 跳转异常', error);
        this.$message.error('SSO 跳转异常，请稍后重试');
      }
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.box-card {
  width: 300px;
  cursor: pointer;
  transition: all 0.3s;
}
.box-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.1);
}
.text {
  font-size: 14px;
  color: #666;
}
</style>
