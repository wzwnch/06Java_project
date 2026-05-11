# 短链接系统开发任务清单（SDD Tasks）

> 基于 SDD Spec + Plan 文档拆解，按阶段细化到最小可执行单元

---

## 阶段一：基础搭建

### 1.1 后端项目初始化

- [ ] 创建 Spring Boot 3.2.x 项目（Java 17）
- [ ] 配置 pom.xml 核心依赖（spring-boot-starter-web、validation、data-redis）
- [ ] 配置 pom.xml ORM 依赖（mybatis-plus-spring-boot3-starter、mysql-connector-j）
- [ ] 配置 pom.xml 工具依赖（hutool-all、lombok、mapstruct）
- [ ] 配置 pom.xml 中间件依赖（redisson、shardingsphere-jdbc）
- [ ] 配置 pom.xml 文档依赖（knife4j-openapi3-jakarta-spring-boot-starter）
- [ ] 创建 application.yml 主配置文件
- [ ] 创建 application-dev.yml 开发环境配置
- [ ] 创建 application-prod.yml 生产环境配置
- [ ] 配置 MySQL 数据源连接信息
- [ ] 配置 Redis 连接信息
- [ ] 配置 MyBatis-Plus 扫描路径和驼峰映射
- [ ] 创建项目启动类 ShortLinkApplication.java

### 1.2 前端项目初始化

- [ ] 创建 Vue 3项目
- [ ] 安装核心依赖（vue-router、pinia、axios）
- [ ] 安装 UI 框架（element-plus）
- [ ] 安装图表库（echarts）
- [ ] 安装工具库（dayjs）
- [ ] 配置 vite.config.ts（代理、别名、端口 3000）
- [ ] 配置 tsconfig.json 类型检查规则
- [ ] 创建 src/api 目录结构
- [ ] 创建 src/components 目录结构
- [ ] 创建 src/views 目录结构
- [ ] 创建 src/stores 目录结构
- [ ] 创建 src/router 目录结构
- [ ] 创建 src/utils 目录结构
- [ ] 创建 src/types 目录结构

### 1.3 数据库表创建

- [ ] 创建数据库 link_db（字符集 utf8mb4）
- [ ] 创建用户表 link_user（含字段、索引、注释）
- [ ] 创建短链接分组表 link_group（含字段、索引、注释）
- [ ] 创建短链接表 link_link（含字段、索引、注释）
- [ ] 创建短链接访问日志表 link_link_stats（含字段、索引、注释）
- [ ] 创建短链接统计汇总表 link_link_stats_today（含字段、索引、注释）
- [ ] 初始化用户名布隆过滤器数据（Redis）
- [ ] 初始化短链接码布隆过滤器数据（Redis）

### 1.4 后端基础模块搭建

- [ ] 创建统一返回结果类 R.java（code、msg、data）
- [ ] 创建业务异常类 BizException.java
- [ ] 创建全局异常拦截器 GlobalExceptionHandler.java
- [ ] 创建业务错误码枚举 BizCodeEnum.java（1000-5999 分段）
- [ ] 创建短链接状态枚举 LinkStatusEnum.java
- [ ] 创建删除标志枚举 DelFlagEnum.java
- [ ] 创建 Redis 配置类 RedisConfig.java（序列化配置）
- [ ] 创建 MyBatis 配置类 MybatisConfig.java（分页插件）
- [ ] 创建 WebMvc 配置类 WebMvcConfig.java（跨域、拦截器）
- [ ] 创建 Redisson 配置类 RedissonConfig.java（分布式锁）

### 1.5 前端基础模块搭建

- [ ] 封装 Axios 请求工具 request.ts（baseURL、超时、拦截器）
- [ ] 封装 Token 管理工具 auth.ts（存取、清除）
- [ ] 封装日期格式化工具 format.ts
- [ ] 创建通用类型定义 common.d.ts（分页、响应结构）
- [ ] 创建路由配置 index.ts（路由表、守卫）
- [ ] 创建用户状态管理 stores/user.ts
- [ ] 创建应用状态管理 stores/app.ts
- [ ] 创建全局样式文件 styles/global.css

---

## 阶段二：核心逻辑开发

### 2.1 用户模块 - 实体与 DAO

- [ ] 创建用户实体类 UserDO.java（对应 link_user 表）
- [ ] 创建用户 Mapper 接口 UserMapper.java
- [ ] 创建用户 Mapper XML 文件 UserMapper.xml

### 2.2 用户模块 - DTO

- [ ] 创建用户注册请求 DTO UserRegisterReqDTO.java（username、password、phone、mail）
- [ ] 创建用户登录请求 DTO UserLoginReqDTO.java（username、password）
- [ ] 创建用户信息修改请求 DTO UserUpdateReqDTO.java
- [ ] 创建用户信息响应 DTO UserInfoRespDTO.java（敏感字段脱敏）

### 2.3 用户模块 - 工具类

- [ ] 创建敏感信息脱敏工具 SensitiveUtils.java（手机号、邮箱脱敏）
- [ ] 创建敏感信息加密工具 EncryptUtils.java（AES 加密/解密）
- [ ] 创建密码加密工具 PasswordUtils.java（BCrypt 加密/校验）
- [ ] 创建 JWT 工具 JwtUtils.java（生成、解析、校验 Token）

### 2.4 用户模块 - 业务层

- [ ] 创建用户服务接口 UserService.java
- [ ] 实现用户注册方法 register()（布隆过滤器校验、密码加密、写入数据库、创建默认分组）
- [ ] 实现用户登录方法 login()（查询用户、密码校验、生成 Token）
- [ ] 实现用户退出方法 logout()（使 Token 失效）
- [ ] 实现查询用户信息方法 getUserInfo()（脱敏返回）
- [ ] 实现修改用户信息方法 updateUserInfo()
- [ ] 实现检查用户名方法 checkUsername()（布隆过滤器 + 空值缓存防穿透）

### 2.5 用户模块 - 控制层

- [ ] 创建用户控制器 UserController.java
- [ ] 实现用户注册接口 POST /api/user/register
- [ ] 实现用户登录接口 POST /api/user/login
- [ ] 实现用户退出接口 POST /api/user/logout
- [ ] 实现查询用户信息接口 GET /api/user/info
- [ ] 实现修改用户信息接口 PUT /api/user/info
- [ ] 实现检查用户名接口 GET /api/user/check-username

### 2.6 用户模块 - 前端对接

- [ ] 创建用户类型定义 types/user.d.ts
- [ ] 创建用户接口 api/user.ts（register、login、logout、getUserInfo、updateUserInfo、checkUsername）
- [ ] 创建登录页面 views/login/index.vue
- [ ] 创建注册页面 views/register/index.vue
- [ ] 实现登录表单校验（用户名、密码格式）
- [ ] 实现注册表单校验（用户名、密码、手机号、邮箱格式）
- [ ] 实现登录成功后存储 Token 和用户信息
- [ ] 实现退出登录清除 Token 和用户信息

### 2.7 短链接分组模块 - 实体与 DAO

- [ ] 创建分组实体类 GroupDO.java（对应 link_group 表）
- [ ] 创建分组 Mapper 接口 GroupMapper.java
- [ ] 创建分组 Mapper XML 文件 GroupMapper.xml

### 2.8 短链接分组模块 - DTO

- [ ] 创建新增分组请求 DTO GroupCreateReqDTO.java（name、sortOrder）
- [ ] 创建修改分组请求 DTO GroupUpdateReqDTO.java（gid、name、sortOrder）
- [ ] 创建分组排序请求 DTO GroupSortReqDTO.java（gidList 排序列表）
- [ ] 创建分组响应 DTO GroupRespDTO.java

### 2.9 短链接分组模块 - 用户上下文

- [ ] 创建用户上下文工具 UserContext.java（ThreadLocal 存储）
- [ ] 创建用户上下文拦截器 UserContextInterceptor.java（从 Token 解析用户信息）
- [ ] 注册拦截器到 WebMvcConfig.java

### 2.10 短链接分组模块 - 业务层

- [ ] 创建分组服务接口 GroupService.java
- [ ] 实现新增分组方法 createGroup()（校验名称唯一性）
- [ ] 实现查询分组列表方法 listGroups()（按排序值排序）
- [ ] 实现修改分组方法 updateGroup()
- [ ] 实现删除分组方法 deleteGroup()（校验分组下是否有短链接）
- [ ] 实现分组排序方法 sortGroups()

### 2.11 短链接分组模块 - 控制层

- [ ] 创建分组控制器 GroupController.java
- [ ] 实现新增分组接口 POST /api/group
- [ ] 实现查询分组列表接口 GET /api/group/list
- [ ] 实现修改分组接口 PUT /api/group
- [ ] 实现删除分组接口 DELETE /api/group/{gid}
- [ ] 实现分组排序接口 PUT /api/group/sort

### 2.12 短链接分组模块 - 前端对接

- [ ] 创建分组类型定义 types/group.d.ts
- [ ] 创建分组接口 api/group.ts
- [ ] 创建分组管理页面 views/group/index.vue
- [ ] 实现分组列表展示（表格、排序）
- [ ] 实现新增分组弹窗表单
- [ ] 实现修改分组弹窗表单
- [ ] 实现删除分组确认弹窗
- [ ] 实现分组拖拽排序功能

### 2.13 短链接管理模块 - 实体与 DAO

- [ ] 创建短链接实体类 LinkDO.java（对应 link_link 表）
- [ ] 创建短链接 Mapper 接口 LinkMapper.java
- [ ] 创建短链接 Mapper XML 文件 LinkMapper.xml

### 2.14 短链接管理模块 - DTO

- [ ] 创建新增短链接请求 DTO LinkCreateReqDTO.java（originUrl、gid、expireTime、customCode）
- [ ] 创建修改短链接请求 DTO LinkUpdateReqDTO.java
- [ ] 创建短链接查询请求 DTO LinkPageReqDTO.java（gid、分页参数）
- [ ] 创建短链接响应 DTO LinkRespDTO.java

### 2.15 短链接管理模块 - 工具类

- [ ] 创建短链接生成工具 LinkUtils.java（雪花 ID + Base62 编码）
- [ ] 创建 URL 校验工具 UrlUtils.java（格式校验）
- [ ] 创建网站信息获取工具 WebInfoUtils.java（获取标题、图标）

### 2.16 短链接管理模块 - 业务层

- [ ] 创建短链接服务接口 LinkService.java
- [ ] 实现新增短链接方法 createLink()（URL 校验、获取网站信息、生成短码、写入数据库、缓存预热）
- [ ] 实现分页查询短链接方法 pageLinks()
- [ ] 实现修改短链接方法 updateLink()
- [ ] 实现删除短链接方法 deleteLink()（逻辑删除，移入回收站）

### 2.17 短链接管理模块 - 控制层

- [ ] 创建短链接控制器 LinkController.java
- [ ] 实现新增短链接接口 POST /api/link
- [ ] 实现分页查询短链接接口 GET /api/link/page
- [ ] 实现修改短链接接口 PUT /api/link
- [ ] 实现删除短链接接口 DELETE /api/link/{shortCode}

### 2.18 短链接管理模块 - 前端对接

- [ ] 创建短链接类型定义 types/link.d.ts
- [ ] 创建短链接接口 api/link.ts
- [ ] 创建短链接管理页面 views/link/index.vue
- [ ] 实现短链接列表展示（表格、分页）
- [ ] 实现新增短链接弹窗表单（URL 输入、分组选择、有效期设置）
- [ ] 实现修改短链接弹窗表单
- [ ] 实现删除短链接确认弹窗
- [ ] 实现短链接复制功能

### 2.19 短链接跳转模块 - 业务层

- [x] 实现短链接跳转方法 redirect()（布隆过滤器校验、缓存查询、分布式锁防击穿、空值缓存防穿透、有效期校验）
- [x] 实现缓存预热方法 warmUpCache()（系统启动时预热热门短链接）

### 2.20 短链接跳转模块 - 控制层

- [x] 实现短链接跳转接口 GET /{shortCode}（302 重定向）
- [x] 配置 404 跳转页面路径
- [x] 配置过期跳转页面路径

### 2.21 回收站模块 - 实体与 DAO

- [ ] 复用 LinkMapper.java（查询 status=1 的数据）

### 2.22 回收站模块 - DTO

- [ ] 创建回收站查询请求 DTO RecyclePageReqDTO.java（分页参数）
- [ ] 创建恢复短链接请求 DTO RecycleRecoverReqDTO.java（shortCode）

### 2.23 回收站模块 - 业务层

- [ ] 创建回收站服务接口 RecycleService.java
- [ ] 实现分页查询回收站方法 pageRecycle()
- [ ] 实现恢复短链接方法 recover()
- [ ] 实现彻底删除方法 remove()（物理删除）

### 2.24 回收站模块 - 控制层

- [ ] 创建回收站控制器 RecycleController.java
- [ ] 实现分页查询回收站接口 GET /api/recycle/page
- [ ] 实现恢复短链接接口 PUT /api/recycle/recover
- [ ] 实现彻底删除接口 DELETE /api/recycle/{shortCode}

### 2.25 回收站模块 - 前端对接

- [ ] 创建回收站接口 api/recycle.ts
- [ ] 创建回收站页面 views/recycle/index.vue
- [ ] 实现回收站列表展示（表格、分页）
- [ ] 实现恢复短链接确认弹窗
- [ ] 实现彻底删除确认弹窗

### 2.26 监控统计模块 - 实体与 DAO

- [ ] 创建访问日志实体类 LinkStatsDO.java（对应 link_link_stats 表）
- [ ] 创建统计汇总实体类 LinkStatsTodayDO.java（对应 link_link_stats_today 表）
- [ ] 创建统计 Mapper 接口 StatsMapper.java
- [ ] 创建统计 Mapper XML 文件 StatsMapper.xml

### 2.27 监控统计模块 - DTO

- [ ] 创建访问日志查询请求 DTO StatsLogPageReqDTO.java
- [ ] 创建统计响应 DTO StatsRespDTO.java
- [ ] 创建今日统计响应 DTO StatsTodayRespDTO.java
- [ ] 创建历史统计响应 DTO StatsHistoryRespDTO.java

### 2.28 监控统计模块 - 工具类

- [ ] 创建 IP 解析工具 IpUtils.java（集成 ip2region）
- [ ] 创建 UA 解析工具 UserAgentUtils.java（解析浏览器、操作系统、设备）
- [ ] 创建 UV 标识生成工具 UvUtils.java（基于 Cookie/指纹）

### 2.29 监控统计模块 - 异步日志处理

- [ ] 创建访问日志 DTO LinkAccessLogDTO.java
- [ ] 创建访问日志生产者 StatsLogProducer.java（推送到 Redis List 队列）
- [ ] 创建访问日志消费者 StatsLogConsumer.java（批量拉取、解析、写入数据库）
- [ ] 配置消费者定时任务（每 100ms 拉取一次）

### 2.30 监控统计模块 - 业务层

- [ ] 创建统计服务接口 StatsService.java
- [ ] 实现记录访问日志方法 recordAccessLog()（解析 IP 地区、UA 信息、推送到队列）
- [ ] 实现更新统计数据方法 updateStats()（PV/UV/UIP 计数）
- [ ] 实现单链接统计方法 getLinkStats()
- [ ] 实现访问日志分页查询方法 pageAccessLog()
- [ ] 实现今日统计方法 getTodayStats()
- [ ] 实现历史统计方法 getHistoryStats()
- [ ] 实现分组统计方法 getGroupStats()
- [ ] 实现高频 IP 统计方法 getHighFreqIp()

### 2.31 监控统计模块 - 控制层

- [ ] 创建统计控制器 StatsController.java
- [ ] 实现单链接统计接口 GET /api/stats/link/{shortCode}
- [ ] 实现访问日志查询接口 GET /api/stats/log/page
- [ ] 实现今日统计接口 GET /api/stats/today
- [ ] 实现历史统计接口 GET /api/stats/history
- [ ] 实现分组统计接口 GET /api/stats/group/{gid}
- [ ] 实现高频 IP 接口 GET /api/stats/high-freq-ip

### 2.32 监控统计模块 - 前端对接

- [x] 创建统计类型定义 types/stats.d.ts
- [x] 创建统计接口 api/stats.ts
- [x] 创建监控统计页面 views/stats/index.vue
- [x] 实现今日统计卡片展示（PV/UV/UIP）
- [x] 实现历史趋势图表（ECharts 折线图）
- [x] 实现访问日志列表展示（表格、分页）
- [x] 实现地区分布图表（ECharts 地图）
- [x] 实现设备/浏览器/操作系统饼图

---

## 阶段三：接口与页面完善

### 3.1 后端接口文档

- [x] 配置 Knife4j API 文档
- [x] 为所有接口添加 @Tag 注解（模块分组）
- [x] 为所有接口添加 @Operation 注解（接口说明）
- [x] 为所有 DTO 添加 @Schema 注解（字段说明）
- [x] 验证 API 文档访问 /doc.html

### 3.2 前端路由配置

- [ ] 配置登录页路由 /login
- [ ] 配置注册页路由 /register
- [ ] 配置仪表盘路由 /dashboard
- [ ] 配置短链接管理路由 /link
- [ ] 配置分组管理路由 /group
- [ ] 配置回收站路由 /recycle
- [ ] 配置监控统计路由 /stats
- [ ] 配置路由守卫（未登录跳转登录页）

### 3.3 前端布局组件

- [ ] 创建布局组件 components/Layout.vue（侧边栏、头部、内容区）
- [ ] 创建侧边栏菜单组件 components/Sidebar.vue
- [ ] 创建头部组件 components/Header.vue（用户信息、退出按钮）
- [ ] 创建分页组件 components/Pagination.vue
- [ ] 创建确认弹窗组件 components/ConfirmDialog.vue

### 3.4 前端仪表盘页面

- [ ] 创建仪表盘页面 views/dashboard/index.vue
- [ ] 实现今日数据概览卡片
- [ ] 实现最近创建的短链接列表
- [ ] 实现访问趋势图表

---

## 阶段四：异常处理与边界场景

### 4.1 全局异常处理

- [ ] 完善全局异常拦截器（BizException、ValidationException、Exception）
- [ ] 统一返回错误码和错误信息
- [ ] 隐藏堆栈信息，记录错误日志

### 4.2 参数校验

- [ ] 为所有请求 DTO 添加 Jakarta Validation 注解（@NotBlank、@Size、@Pattern 等）
- [ ] 为控制器方法添加 @Validated 注解
- [ ] 自定义手机号校验注解 @Phone
- [ ] 自定义 URL 校验注解 @Url

### 4.3 缓存策略实现

- [ ] 实现布隆过滤器初始化（用户名、短链接码）
- [ ] 实现布隆过滤器添加元素方法
- [ ] 实现布隆过滤器判断存在方法
- [ ] 实现空值缓存防穿透（5 分钟过期）
- [ ] 实现分布式锁防缓存击穿（Redisson）
- [ ] 实现缓存预热（系统启动时加载热门短链接）
- [ ] 实现缓存更新策略（先更新数据库，再删除缓存）

### 4.4 用户模块边界场景

- [ ] 处理用户名已存在场景（返回错误码 1001）
- [ ] 处理用户名格式不合法场景（返回错误码 1002）
- [ ] 处理密码格式不合法场景（返回错误码 1003）
- [ ] 处理手机号已绑定场景（返回错误码 1004）
- [ ] 处理登录密码错误场景（返回错误码 1005）
- [ ] 处理 Token 过期/无效场景（返回 401）
- [ ] 处理缓存穿透场景（布隆过滤器 + 空值缓存）

### 4.5 短链接模块边界场景

- [ ] 处理目标 URL 格式不合法场景（返回错误码 2001）
- [ ] 处理短链接码重复场景（重新生成）
- [ ] 处理短链接已过期场景（跳转过期页面）
- [ ] 处理短链接不存在场景（跳转 404 页面）
- [ ] 处理目标网站无法访问场景（使用默认标题和图标）
- [ ] 处理缓存击穿场景（分布式锁）
- [ ] 处理缓存穿透场景（空值缓存）
- [ ] 处理分组不存在场景（返回错误码 3001）

### 4.6 回收站模块边界场景

- [x] 处理恢复时短链接码已被占用场景（返回错误码 4001）
- [x] 处理彻底删除后再次操作场景（返回错误码 4002）
- [x] 处理回收站为空场景（返回空列表）

### 4.7 监控统计模块边界场景

- [x] 处理无访问数据场景（返回零值）
- [x] 处理时间范围超过限制场景（返回错误码 5001）
- [x] 处理 Redis 队列积压场景（告警通知）
- [x] 处理 IP 解析失败场景（标记为"未知"）

### 4.8 系统级异常处理

- [ ] 处理数据库连接失败场景（返回 500，告警通知）
- [ ] 处理 Redis 连接失败场景（降级直连数据库）
- [ ] 处理参数校验失败场景（返回 400，提示具体错误）
- [ ] 处理未登录访问受保护接口场景（返回 401）
- [ ] 处理无权限访问场景（返回 403）
- [ ] 处理系统内部异常场景（返回 500，统一错误码）

---

## 阶段五：测试与优化

### 5.1 单元测试

- [ ] 编写用户注册方法单元测试
- [ ] 编写用户登录方法单元测试
- [ ] 编写短链接生成方法单元测试
- [ ] 编写短链接跳转方法单元测试
- [ ] 编写分组 CRUD 方法单元测试
- [ ] 编写统计数据方法单元测试

### 5.2 集成测试

- [x] 编写用户模块接口集成测试
- [x] 编写短链接模块接口集成测试
- [x] 编写分组模块接口集成测试
- [x] 编写回收站模块接口集成测试
- [x] 编写监控统计模块接口集成测试

### 5.3 缓存测试

- [x] 测试布隆过滤器防穿透效果
- [x] 测试分布式锁防击穿效果
- [x] 测试缓存预热效果
- [x] 测试缓存更新一致性

### 5.4 性能测试

- [ ] 测试短链接跳转响应时间（目标 P99 < 100ms）
- [ ] 测试短链接创建响应时间（目标 P99 < 500ms）
- [ ] 测试用户注册响应时间（目标 P99 < 500ms）
- [ ] 测试并发短链接跳转（目标 10000 QPS）
- [ ] 测试统计数据查询响应时间（目标 P99 < 1s）

### 5.5 安全测试

- [ ] 测试密码加密存储（不可逆）
- [ ] 测试敏感信息脱敏返回
- [ ] 测试 Token 有效期控制
- [ ] 测试 SQL 注入防护
- [ ] 测试 XSS 攻击防护

### 5.6 前端测试

- [ ] 测试登录流程（正常、异常）
- [ ] 测试注册流程（正常、异常）
- [ ] 测试短链接创建流程
- [ ] 测试短链接跳转流程
- [ ] 测试分组管理流程
- [ ] 测试回收站操作流程
- [ ] 测试监控统计展示

### 5.7 优化调整

- [ ] 优化数据库索引（根据慢查询日志）
- [ ] 优化缓存过期时间配置
- [ ] 优化分页查询性能
- [ ] 优化前端首屏加载速度
- [ ] 优化图表渲染性能

---

## 任务统计

| 阶段 | 任务数 |
|------|--------|
| 阶段一：基础搭建 | 58 |
| 阶段二：核心逻辑 | 116 |
| 阶段三：接口与页面完善 | 19 |
| 阶段四：异常处理与边界场景 | 35 |
| 阶段五：测试与优化 | 27 |
| **总计** | **255** |

---

**文档版本**：v1.0  
**创建日期**：2026-04-25  
**关联文档**：SDD-短链接系统需求规范文档.md、SDD-短链接系统技术实现方案.md  
**文档状态**：待执行
