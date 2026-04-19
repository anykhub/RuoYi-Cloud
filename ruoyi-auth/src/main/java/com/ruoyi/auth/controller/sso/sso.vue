<template>
  <div class="sso-container">
    <div class="sso-message" v-if="loading">
      正在单点登录跳转中...
    </div>
    <div class="sso-message" v-else>
      {{ errorMessage }}
    </div>
  </div>
</template>

<script>
export default {
  name: "sso",
  data() {
    return {
      loading: true,
      errorMessage: ""
    }
  },
  created() {
    this.handleSSO();
  },
  methods: {
    handleSSO() {
      // 获取路由传入的所有参数（包含了从后端发来的完整用户信息及Token）
      const data = { ...this.$route.query, ...this.$route.params };
      const token = data.accessToken || data.access_token || data.token;
      const refreshToken = data.refreshToken || data.refresh_token;
      const redirect = data.redirect || '/';

      if (!token) {
        this.loading = false;
        this.errorMessage = "单点登录失败，缺少token参数";
        return;
      }

      try {
        // 1. 保存 accessToken
        // vuex mutation中会同时调用setToken将其存入Cookie
        this.$store.commit('SET_TOKEN', token);
        
        // 2. 如果存在 refreshToken 则一并保存
        if (refreshToken) {
          this.$store.commit('SET_REFRESH_TOKEN', refreshToken);
        }

        // 3. 将整个 userInfo 保存至 store 中
        this.$store.commit('SET_USER_INFO', data);

        // 4. 清除遗留页签和锁屏状态，以保证状态干净
        this.$store.commit('DEL_ALL_TAG');
        this.$store.commit('CLEAR_LOCK');
        
        // 5. 核心修复逻辑：在重定向之前，需要先向后端请求当前用户的角色权限菜单
        // 如果不提前加载，Saber 会因为找不到动态路由直接将其拦截至 404 页面
        this.$store.dispatch("GetMenu").then(menuData => {
          if (menuData && menuData.length !== 0) {
            // 调用 Avue-Router 注册动态路由表
            this.$router.$avueRouter.formatRoutes(menuData, true);
          }
          // 一切就绪环境加载完毕后，跳回原系统指定页面
          this.$router.replace({ path: redirect });
        }).catch(() => {
          // 如果获取菜单失败（网络或权限问题），尝试平滑跳转或者跳转登录
          this.$router.replace({ path: '/' });
        });
      } catch (error) {
        this.loading = false;
        this.errorMessage = "单点登录失败: " + error.message;
      }
    }
  }
}
</script>

<style scoped>
.sso-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5;
}
.sso-message {
  font-size: 20px;
  color: #333;
}
</style>
