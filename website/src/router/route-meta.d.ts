import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 全屏认证页（无主导航） */
    layout?: 'auth' | 'main'
    /** 无需登录（登录、注册等） */
    public?: boolean
  }
}

export {}
