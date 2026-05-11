# Trae 全栈强制规则（Vue3 + SpringBoot3 + MySQL8 + Redis8）
## 1. 全局原则
- 前后端严格分离
- 先数据库 → 后端接口 → 前端页面
- 代码可运行、无报错、可自测
- 不删除原有逻辑

## 2. 前端 Vue3 规范
- <script setup lang="ts">
- 组件大驼峰，变量小驼峰
- 禁止 any
- 接口统一放 api/
- 异步必须 try/catch
- 样式 scoped

## 3. 后端 SpringBoot3 规范
- Java 17 语法
- 分层：controller → service → mapper
- 统一返回结果 R.ok() / R.fail()
- MySQL 使用 MyBatis / MyBatis-Plus
- Redis 使用 StringRedisTemplate
- 接口 RESTful 风格
- 参数校验、全局异常处理

## 4. MySQL 8 规范
- 表名小写+下划线
- 主键 id 自增
- 必须包含 create_time、update_time
- 禁止编写危险 SQL

## 5. Redis 8 规范
- key 使用 : 分隔，如 user:1:info
- 必须设置过期时间
- 只做缓存，不存核心业务

## 6. Trae 行为约束
- 收到需求先确认：前端 / 后端 / 数据库
- 复杂需求先 /Plan
- 每次只改 1~3 个文件
- 不批量修改、不私自装依赖