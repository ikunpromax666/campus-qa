// 通用响应类型（与后端 Result 对齐）
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应（与后端 PageResultVO 对齐）
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 用户信息（与后端 UserInfoVO 对齐）
export interface UserInfoVO {
  id: number
  nickname: string
  avatar: string
  bio: string
  role: number
  createTime: string
}

// 登录/注册响应（与后端 LoginVO 对齐）
export interface LoginVO {
  token: string
  nickname: string
}

// 分类（与后端 AdminCategoryVO 对齐，前台只用 id/name）
export interface CategoryVO {
  id: number
  name: string
  description: string
  sortOrder: number
  createTime: string
}

// 问题列表项（与后端 QuestionListVO 对齐）
export interface QuestionListVO {
  id: number
  title: string
  nickname: string
  avatar: string
  categoryName: string
  tags: string[]
  viewCount: number
  likeCount: number
  answerCount: number
  createTime: string
}

// 问题详情（与后端 QuestionDetailVO 对齐）
export interface QuestionDetailVO {
  id: number
  title: string
  content: string // Markdown 原文
  userId: number
  nickname: string
  avatar: string
  category: { id: number; name: string }
  tags: { id: number; name: string }[]
  viewCount: number
  likeCount: number
  answerCount: number
  status: number // 0 正常 / 1 已关闭 / 2 已删除
  isTop: number
  createTime: string
  likeStatus: number // 1 已点赞 0 未点赞（后端当前用户视角）
  isFavorited: boolean
  isOwner: boolean
}

// 回答项（与后端 AnswerVO 对齐）
export interface AnswerVO {
  id: number
  questionId: number
  userId: number
  nickname: string
  avatar: string
  content: string // Markdown 原文
  likeCount: number
  isAccepted: number // 1 被采纳
  likeStatus: number
  isOwner: boolean
  createTime: string
}

// 点赞/收藏切换结果（与后端 ToggleResultVO 对齐）
export interface ToggleResultVO {
  toggleStatus: number // 1 生效（已点赞/收藏） 0 取消
  targetId: number
  likeCount: number
}

// ---------- 管理后台 ----------
export interface AdminUserVO {
  id: number
  phone: string // 后端赋值 VO 时已脱敏：133****8052
  nickname: string
  avatar: string
  role: number // 0 普通用户 1 管理员
  status: number // 0 正常 1 禁用
  createTime: string
}

export interface AdminCategoryVO {
  id: number
  name: string
  description: string
  sortOrder: number
  createTime: string
}

export interface AdminTagVO {
  id: number
  name: string
  createTime: string
}

// 我的回答/赞过的回答列表项（与后端 MyAnswerVO 对齐）
export interface MyAnswerVO {
  id: number
  questionId: number
  questionTitle: string
  content: string // Markdown 原文，列表页截断展示
  likeCount: number
  isAccepted: number // 1 被采纳
  userId: number
  nickname: string
  avatar: string
  createTime: string
}

// 我的提问列表项（与后端 MyQuestionVO 对齐；作者视角多 status，无 nickname/avatar）
export interface MyQuestionVO {
  id: number
  title: string
  status: number // 0 正常 1 已关闭（已删除的不进列表）
  categoryName: string
  tags: string[]
  viewCount: number
  likeCount: number
  answerCount: number
  createTime: string
}
