export interface LoginPayload {
  account: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResult {
  ok: boolean;
  message: string;
  token?: string;
  userId?: string;
}

const mockDelay = (ms = 700) => new Promise((resolve) => setTimeout(resolve, ms));

// 这里是登录请求骨架: 后续接入真实接口时替换内部逻辑
export async function loginRequest(payload: LoginPayload): Promise<AuthResult> {
  await mockDelay();

  // TODO: 接入后端登录 API
  // 示例: return http.post('/api/auth/login', payload)
  console.info("login payload preview", payload);

  return {
    ok: true,
    message: "登录流程已触发（当前为演示骨架）。",
    token: "mock-token",
    userId: "mock-user-id",
  };
}

// 这里是注册请求骨架: 可扩展验证码、邀请码、组织信息
export async function registerRequest(payload: RegisterPayload): Promise<AuthResult> {
  await mockDelay();

  // TODO: 接入后端注册 API
  // 示例: return http.post('/api/auth/register', payload)
  console.info("register payload preview", payload);

  return {
    ok: true,
    message: "注册流程已触发（当前为演示骨架）。",
  };
}
