# 项目智能体指引（Trae 全栈驾驭：Vue3 + SpringBoot3 + MySQL8 + Redis8）
## 项目基础信息
- 前端：Vue3 + TypeScript + Vite + Pinia + Axios
- 后端：SpringBoot 3.2.x + Java 17 + Maven
- 数据库：MySQL 8.4.8
- 缓存：Redis 8.6.2
- 项目类型：前后端分离全栈项目

## 运行命令
- 前端：cd frontend → npm run dev
- 后端：cd backend → mvn spring-boot:run
- 前端地址：http://localhost:3000
- 后端地址：http://localhost:8080
- API 前缀：/api/**

## 目录规范
### 前端 frontend/src
api/         接口请求
components/  公共组件
views/       页面
stores/      Pinia 状态
utils/       工具
router/      路由
types/       TS 类型

### 后端 backend/src/main/java/com/example
controller/   接口层
service/      业务层
impl/         业务实现
mapper/       DAO
entity/       实体类
config/       配置（MySQL/Redis）
common/       通用返回
utils/        工具

## 开发顺序
1. 设计 MySQL 表
2. 生成实体类、Mapper
3. 开发后端接口（带 Redis 缓存）
4. 对接前端页面

## Trae 约定
1. 必须区分：前端 / 后端 / 数据库
2. 复杂需求先 /Plan
3. 每次只改少量文件
4. 改完给：修改文件 + 测试方式
5. 不私自改配置、不破坏原有代码