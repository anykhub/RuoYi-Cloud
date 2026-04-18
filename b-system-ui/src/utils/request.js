import axios from 'axios';

// 创建 axios 实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // api的base_url
  timeout: 10000 // 请求超时时间
});

// 标记是否正在刷新 token
let isRefreshing = false;
// 存储因等待 token 刷新而挂起的请求
let requests = [];

// A 系统 API 的基础路径，用于判断是否为请求 A 系统的接口报的 401
const A_SYSTEM_API_PREFIX = '/a-api';

// Request 拦截器
service.interceptors.request.use(
  config => {
    // 根据请求目标，携带相应的 Token
    // 如果是请求 A 系统的接口，则携带 A 系统的 Token；否则携带 B 系统的 Token
    if (config.url.startsWith(A_SYSTEM_API_PREFIX)) {
      const aToken = localStorage.getItem('A_SYSTEM_TOKEN');
      if (aToken) {
        config.headers['Authorization'] = 'Bearer ' + aToken;
      }
    } else {
      const bToken = localStorage.getItem('B_SYSTEM_TOKEN');
      if (bToken) {
        config.headers['Blade-Auth'] = 'bearer ' + bToken;
      }
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Response 拦截器
service.interceptors.response.use(
  response => {
    // 假设正常返回状态码判断逻辑
    return response.data;
  },
  error => {
    const { config, response } = error;

    // 如果没有 response，说明网络等其他错误，直接抛出
    if (!response) {
      return Promise.reject(error);
    }

    // 判断是否是 401 错误，并且是请求 A 系统的接口
    if (response.status === 401 && config.url.startsWith(A_SYSTEM_API_PREFIX)) {
      if (!isRefreshing) {
        isRefreshing = true;

        // 调用 B 系统后端的 /blade-sso/refresh-a-token 接口静默换取 A 系统的最新 JWT
        // 这里使用新的 axios 实例以免被拦截器再次拦截
        return axios.get('/blade-api/blade-sso/internal/refresh-a-token', {
          headers: {
            'Blade-Auth': 'bearer ' + localStorage.getItem('B_SYSTEM_TOKEN')
          }
        }).then(res => {
          const data = res.data;
          if (data.code === 200 && data.data && data.data.access_token) {
            const newToken = data.data.access_token;
            // 将新 JWT 存入 localStorage
            localStorage.setItem('A_SYSTEM_TOKEN', newToken);

            // 将新 token 设置到当前失败的请求 config 中，并重新发起请求
            config.headers['Authorization'] = 'Bearer ' + newToken;

            // 重新执行队列中挂起的请求
            requests.forEach(cb => cb(newToken));
            // 清空队列
            requests = [];

            // 重新发起当前请求
            return service(config);
          } else {
            console.error('刷新 A 系统 token 失败:', data.msg);
            // 刷新失败，重定向到登录页或其他处理
            return Promise.reject(error);
          }
        }).catch(err => {
          console.error('刷新 token 接口调用异常:', err);
          // 清空队列
          requests = [];
          return Promise.reject(err);
        }).finally(() => {
          // 刷新结束，重置标志位
          isRefreshing = false;
        });
      } else {
        // 正在刷新 token 时，返回一个未执行 resolve 的 promise
        // 把请求的回调存入队列中，当 token 刷新完成后，执行回调
        return new Promise((resolve) => {
          requests.push((token) => {
            config.headers['Authorization'] = 'Bearer ' + token;
            resolve(service(config));
          });
        });
      }
    }

    // 其他错误抛出
    return Promise.reject(error);
  }
);

export default service;
