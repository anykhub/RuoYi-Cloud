<template>
  <div class="sso-callback">
    <div class="loading-container">
      <p v-if="loading">正在验证 SSO 登录状态，请稍候...</p>
      <p v-else-if="error" class="error-msg">{{ errorMsg }}</p>
      <p v-else>登录成功，正在跳转...</p>
    </div>
  </div>
</template>

<script>
// 假设这里引入了若依封装的 request 或者是 a 系统的 API 请求工具
import request from '@/utils/request'
// 假设这里引入了 Vuex 方便获取和设置用户信息
import { mapActions, mapGetters } from 'vuex'
// 引入 js-cookie 等操作 Token 的工具（根据若依实际情况，可能存在于 utils/auth）
import { setToken, getToken } from '@/utils/auth'

export default {
  name: 'SsoCallback',
  data() {
    return {
      loading: true,
      error: false,
      errorMsg: ''
    }
  },
  computed: {
    // 获取 vuex 中当前的 token
    ...mapGetters(['token'])
  },
  created() {
    this.handleSsoCallback()
  },
  methods: {
    ...mapActions(['GetInfo']),

    async handleSsoCallback() {
      // 1. 获取 URL 中的 short token
      const token = this.$route.query.token;

      // 2. 检查本地是否已经有有效的 JWT
      const localToken = getToken();
      if (localToken) {
        try {
          // 尝试获取用户信息，验证 token 是否有效
          await this.GetInfo();
          // 有效则直接跳转目标页 (如首页或传过来的 redirect 地址)
          this.redirectTarget();
          return;
        } catch (e) {
          // 获取信息失败说明 token 无效或过期，继续下面的逻辑进行 SSO 登录
          console.log('Local token invalid, proceeding with SSO login');
        }
      }

      if (!token) {
        this.showError('URL 中缺失 token 参数，无法完成单点登录');
        return;
      }

      // 3. 调用 A 系统的 /sso/login-by-token 接口
      try {
        const res = await request({
          url: '/sso/login-by-token',
          method: 'post',
          data: { token }
        });

        if (res.code === 200 && res.data && res.data.access_token) {
          // 4. 拿到若依 JWT 后存入本地
          setToken(res.data.access_token);

          // 更新 vuex 中的 token (根据若依实际实现可能在 setToken 或是需要 commit)
          this.$store.commit('SET_TOKEN', res.data.access_token);

          this.loading = false;

          // 获取用户信息并跳转
          await this.GetInfo();
          this.redirectTarget();
        } else {
          this.showError(res.msg || 'SSO 登录失败');
        }
      } catch (err) {
        this.showError('SSO 请求异常，请稍后重试');
        console.error(err);
      }
    },

    showError(msg) {
      this.loading = false;
      this.error = true;
      this.errorMsg = msg;
    },

    redirectTarget() {
      const redirect = this.$route.query.redirect || '/';
      this.$router.replace({ path: redirect });
    }
  }
}
</script>

<style scoped>
.sso-callback {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}
.loading-container {
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
  font-size: 16px;
}
.error-msg {
  color: #f56c6c;
}
</style>
