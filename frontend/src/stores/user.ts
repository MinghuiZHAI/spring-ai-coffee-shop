import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/api/auth'
import { clearTokens, getRefreshToken, getToken, setTokens } from '@/api/client'
import { getProfile } from '@/api/user'

/**
 * 登录态 store（M1-6 批次 3）：token/refreshToken 持久化 localStorage（key 归
 * api/client.ts 管理，避免循环引用）；userInfo 不持久化——刷新后由路由守卫
 * 调 fetchProfile() 回填（401 时 client 已清 token 并跳登录）。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    refreshToken: getRefreshToken(),
    userInfo: null as UserInfo | null,
  }),

  getters: {
    isLoggedIn: (state) => state.token !== '',
    /** 等级名映射（V2《会员与积分规则》KB 文档口径，与后端 TINYINT 对应） */
    memberLevelLabel: (state) => {
      switch (state.userInfo?.memberLevel) {
        case 2:
          return 'L2 领航员'
        case 3:
          return 'L3 船长'
        default:
          return 'L1 航海员'
      }
    },
  },

  actions: {
    async login(phone: string, password: string) {
      const res = await authApi.login(phone, password)
      this.token = res.accessToken
      this.refreshToken = res.refreshToken
      this.userInfo = res.userInfo
      setTokens(res.accessToken, res.refreshToken)
      return res
    },

    /** 拉取最新资料（路由守卫刷新回填 / 资料编辑后同步） */
    async fetchProfile() {
      this.userInfo = await getProfile()
      return this.userInfo
    },

    /** 幂等登出：后端白名单清理尽力而为，本地登录态必定清除 */
    async logout() {
      try {
        if (this.refreshToken) {
          await authApi.logout(this.refreshToken)
        }
      } catch {
        // 后端已失效也视为登出成功
      } finally {
        clearTokens()
        this.token = ''
        this.refreshToken = ''
        this.userInfo = null
      }
    },
  },
})
