import request from '../utils/request'
import type {
  Result,
  PageResult,
  LoginVO,
  UserInfoVO,
  CategoryVO,
  QuestionListVO,
  QuestionDetailVO,
  AnswerVO,
  MyAnswerVO,
  MyQuestionVO,
  ToggleResultVO,
  AdminUserVO,
  AdminCategoryVO,
  AdminTagVO,
  HotQuestionVO,
  AnswererRankVO
} from '../types'

// ---------- 用户模块 ----------
export const userApi = {
  login: (data: { phone: string; password: string }) =>
    request.post<Result<LoginVO>>('/user/login', data),

  register: (data: { phone: string; password: string; confirmPassword: string; nickname: string }) =>
    request.post<Result<LoginVO>>('/user/register', data),

  getInfo: () => request.get<Result<UserInfoVO>>('/user/info'),

  updateInfo: (data: { nickname?: string; avatar?: string; bio?: string }) =>
    request.put<Result<void>>('/user/info', data),

  updatePassword: (data: { oldPassword: string; newPassword: string; confirmPassword: string }) =>
    request.put<Result<void>>('/user/password', data),

  // FormData 直接交给 axios：Content-Type（multipart/form-data + boundary）由 axios 自动设置，
  // 手动写死 'multipart/form-data' 反而会丢 boundary，导致后端解析不到文件
  uploadAvatar: (formData: FormData) => request.post<Result<string>>('/user/avatar', formData)
}

// ---------- 分类模块 ----------
export const categoryApi = {
  list: () => request.get<Result<CategoryVO[]>>('/category/list')
}

// ---------- 标签模块 ----------
export interface TagVO {
  id: number
  name: string
  createTime?: string
}

export const tagApi = {
  list: () => request.get<Result<TagVO[]>>('/tag/list')
}

// ---------- 问题模块 ----------
export interface QuestionPageParams {
  page: number
  size: number
  categoryId?: number
  keyword?: string
  sort?: 'time' | 'hot'
}

export const questionApi = {
  page: (params: QuestionPageParams) =>
    request.get<Result<PageResult<QuestionListVO>>>('/question/page', { params }),

  getDetail: (id: number) => request.get<Result<QuestionDetailVO>>(`/question/${id}`),

  publish: (data: { title: string; content: string; categoryId: number; tagIds: number[] }) =>
    request.post<Result<void>>('/question/publish', data),

  // 编辑问题（作者）：字段与发布一致；后端局部更新 + 标签"先删后插"，均在事务内
  update: (
    id: number,
    data: { title: string; content: string; categoryId: number; tagIds: number[] }
  ) => request.put<Result<void>>(`/question/${id}`, data),

  // 我发表的提问（作者视角，含已关闭，不含已删除）
  my: (params: { page: number; size: number }) =>
    request.get<Result<PageResult<MyQuestionVO>>>('/question/my', { params }),

  // 逻辑删除（status=2），后端校验作者/管理员
  remove: (id: number) => request.delete<Result<void>>(`/question/${id}`),

  // 关闭问题（status=1），后端校验作者 + 拒绝重复关闭
  close: (id: number) => request.put<Result<void>>(`/question/${id}/close`)
}

// ---------- 回答模块 ----------
export const answerApi = {
  // 注意：后端是 POST /answer/list + body（GET 语义但用了 POST，既有设计）
  list: (questionId: number) =>
    request.post<Result<PageResult<AnswerVO>>>('/answer/list', { questionId }),

  publish: (data: { questionId: number; content: string }) =>
    request.post<Result<void>>('/answer/publish', data),

  // 编辑回答（仅回答作者，后端校验归属）
  update: (id: number, content: string) =>
    request.put<Result<void>>(`/answer/${id}`, { content }),

  // 后端是散参数 query 绑定（POST /answer/accept?answerId=&questionId=），用 params 传
  accept: (answerId: number, questionId: number) =>
    request.post<Result<void>>(`/answer/accept?answerId=${answerId}&questionId=${questionId}`),

  // 我发表的回答（GET + query 参数，符合 HTTP 语义）
  my: (params: { page: number; size: number }) =>
    request.get<Result<PageResult<MyAnswerVO>>>('/answer/my', { params }),

  // 逻辑删除，后端校验回答作者/管理员
  remove: (id: number) => request.delete<Result<void>>(`/answer/${id}`)
}

// ---------- 互动模块 ----------
// targetType：1 问题 2 回答
export const interactionApi = {
  like: (targetId: number, targetType: 1 | 2) =>
    request.post<Result<ToggleResultVO>>('/interaction/like', { targetId, targetType }),

  favorite: (questionId: number) =>
    request.post<Result<ToggleResultVO>>('/interaction/favorite', { questionId }),

  // 我收藏的问题（收藏时间倒序）
  myFavorites: (params: { page: number; size: number }) =>
    request.get<Result<PageResult<QuestionListVO>>>('/interaction/my/favorites', { params }),

  // 我赞过的问题（点赞时间倒序）
  myLikedQuestions: (params: { page: number; size: number }) =>
    request.get<Result<PageResult<QuestionListVO>>>('/interaction/my/likes/questions', { params }),

  // 我赞过的回答（点赞时间倒序）
  myLikedAnswers: (params: { page: number; size: number }) =>
    request.get<Result<PageResult<MyAnswerVO>>>('/interaction/my/likes/answers', { params })
}

// ---------- 管理后台模块 ----------
// 后端所有 /admin/** 接口都在 Controller 类上标了 @RequireAdmin，
// 非管理员的请求一律 403 —— 前端菜单隐藏只是体验，权限由后端兜底
export const adminApi = {
  // ---- 用户管理 ----
  users: (params: { page: number; size: number; keyword?: string; status?: number }) =>
    request.get<Result<PageResult<AdminUserVO>>>('/admin/user/list', { params }),

  // 启用/禁用用户（后端拒绝操作自己：50001）
  updateUserStatus: (id: number, status: number) =>
    request.put<Result<void>>(`/admin/user/${id}/status`, { status }),

  // ---- 分类管理 ----
  categories: () => request.get<Result<AdminCategoryVO[]>>('/admin/category/list'),

  createCategory: (data: { name: string; description?: string; sortOrder?: number }) =>
    request.post<Result<void>>('/admin/category', data),

  updateCategory: (id: number, data: { name: string; description?: string; sortOrder?: number }) =>
    request.put<Result<void>>(`/admin/category/${id}`, data),

  // 分类下有正常状态问题时后端拒绝（20008）
  deleteCategory: (id: number) => request.delete<Result<void>>(`/admin/category/${id}`),

  // ---- 标签管理 ----
  tags: () => request.get<Result<AdminTagVO[]>>('/admin/tag/list'),

  createTag: (name: string) => request.post<Result<void>>('/admin/tag', { name }),

  // 标签被引用时后端拒绝（20009）
  deleteTag: (id: number) => request.delete<Result<void>>(`/admin/tag/${id}`)
}

// ---------- 排行榜模块 ----------
// 两个榜单都是公开 GET（游客可看），limit 后端默认 10
export const rankApi = {
  hot: (limit = 10) =>
    request.get<Result<HotQuestionVO[]>>('/rank/hot', { params: { limit } }),
  answerer: (limit = 10) =>
    request.get<Result<AnswererRankVO[]>>('/rank/answerer', { params: { limit } }),

  // 手动重算（仅管理员，后端 @RequireAdmin；重算是全表扫描高成本操作）
  refreshHot: () => request.post<Result<void>>('/rank/refresh'),
  refreshAnswerer: () => request.post<Result<void>>('/rank/answerer/refresh')
}
