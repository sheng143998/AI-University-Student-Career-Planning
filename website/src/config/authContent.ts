export interface AuthPageContent {
  heroTagline: string;
  heroTitle: string;
  heroSubtitle: string;
  primaryActionText: string;
  secondaryActionText: string;
  tips: string[];
}

export interface AuthContentMap {
  login: AuthPageContent;
  register: AuthPageContent;
}

/*
  内容定义区说明：
  1) 可以直接修改下面文案，快速替换网页主标题、副标题、按钮文案。
  2) 可以按业务新增字段，例如: supportEmail、featureList、announcement。
  3) 新增字段后，在 LoginPage.vue / RegisterPage.vue 里通过 content.xxx 渲染出来。
*/
export const authContent: AuthContentMap = {
  login: {
    heroTagline: "Navigate Your Career with Intelligence",
    heroTitle: "职引 AI",
    heroSubtitle: "你的首位 AI 职业规划导师，精准定位每一个职场梦想。",
    primaryActionText: "启动登录协议",
    secondaryActionText: "没有账号？去创建职引 AI 身份",
    tips: [
      "建议使用企业邮箱，便于团队协作权限同步.",
      "登录后可在控制台查看最近任务轨迹。",
      "支持后续接入短信、邮箱和第三方 OAuth 登录。",
    ],
  },
  register: {
    heroTagline: "COSMIC ONBOARDING",
    heroTitle: "创建你的职引AI身份",
    heroSubtitle: "只需几步，启用属于你的未来控制台。",
    primaryActionText: "完成注册校验",
    secondaryActionText: "已有账号？返回登录",
    tips: [
      "密码建议至少 8 位，包含字母与数字。",
      "可在注册后补充头像、组织与偏好设置。",
      "将来可扩展邀请码、审批流、试用套餐逻辑。",
    ],
  },
};
