# qh-dianping

一个基于 Spring Boot 3 的本地生活/点评后端项目，包含用户、店铺、博客、关注、优惠券与秒杀下单等能力，并结合 Redis、Kafka、ShardingSphere 做高并发与数据分片支持。

## 项目概览

- **项目名**：`qh-dianping`
- **技术定位**：点评类后端服务（REST API）
- **运行端口**：默认 `8081`
- **JDK 版本**：`17`
- **构建工具**：`Maven`

## 技术栈

- **基础框架**：Spring Boot 3.4、Spring MVC、Validation、AOP
- **数据访问**：MyBatis-Plus 3.5、MySQL 9、ShardingSphere-JDBC 5.5
- **缓存与并发**：Redis、Redisson、Lua 脚本、Caffeine
- **消息队列**：Kafka（含秒杀相关消费者）
- **认证与安全**：JWT、登录/Token 刷新拦截器
- **可观测与文档**：Actuator、Prometheus、SpringDoc、Knife4j

## 核心能力

- 用户验证码登录、Token 登录态维护
- 店铺查询（含缓存能力）
- 博客发布/点赞/关注流查询
- 关注关系管理
- 普通券与秒杀券管理
- 秒杀下单、下单幂等、分布式锁控制
- 秒杀订单补偿与对账任务（Kafka + 任务服务）

## 目录结构（核心）

```text
qh-dianping/
├─ pom.xml
├─ src/main/java/com/qhdp
│  ├─ controller/           # API 控制器
│  ├─ service/              # 业务接口与实现
│  ├─ mapper/               # MyBatis Mapper
│  ├─ entity/dto/vo/        # 数据对象
│  ├─ config/               # Spring 与中间件配置
│  ├─ kafka/consumer/       # Kafka 消费者
│  ├─ servicelocker/        # 分布式锁抽象与实现
│  ├─ ratelimit/            # 限流组件
│  └─ utils/                # 工具类与拦截器
└─ src/main/resources
   ├─ application.yml
   ├─ application-pri.yml
   ├─ shardingsphere.yaml
   ├─ mapper/               # XML 映射
   └─ lua/                  # Redis Lua 脚本
```

## 主要接口分组

- `UserController`：`/user/**`
- `ShopController`：`/shop/**`
- `ShopTypeController`：`/shop-type/**`
- `BlogController`：`/blog/**`
- `FollowController`：`/follow/**`
- `VoucherController`：`/voucher/**`
- `VoucherOrderController`：`/voucher-order/**`
- `UploadController`：`/upload/**`

## 配置说明

项目当前通过 `application.yml` + `application-pri.yml` 组合配置：

- `application.yml`：公共配置（端口、Redis/Kafka/MyBatis 等）
- `application-pri.yml`：私有环境变量（数据库、Redis、Kafka、AI key 等）
- `shardingsphere.yaml`：分库分表与广播表规则

建议：

1. 将私密信息改为环境变量读取（如数据库密码、API Key）。
2. 不要将真实密钥提交到代码仓库。

## 本地启动

### 1) 准备依赖环境

- JDK `17`
- MySQL（按 `shardingsphere.yaml` 准备 `qhdp_0`、`qhdp_1`）
- Redis
- Kafka

### 2) 配置参数

根据本机环境修改：

- `src/main/resources/application-pri.yml`
- `src/main/resources/shardingsphere.yaml`

### 3) 构建与运行

```bash
mvn clean package
mvn spring-boot:run
```

应用启动后默认访问：`http://localhost:8081`

## 测试

当前项目存在基础 SpringBoot 上下文测试类：

- `src/test/java/com/qhdp/qhDianPingApplicationTests.java`

可执行：

```bash
mvn test
```

## 中间件与高并发设计要点

- 基于 Redis + Lua 处理秒杀核心原子逻辑
- Redisson 分布式锁 + 自定义 `@ServiceLock` 注解做并发控制
- `@RepeatExecuteLimit` 实现重复执行限制（幂等防护）
- Kafka 消费者处理异步下单/失效/死信等场景
- ShardingSphere 对核心业务表分片路由

## 后续可完善项

- 补充数据库初始化脚本与样例数据
- 补充接口文档访问地址与鉴权说明
- 增加集成测试、压测与容灾演练文档
- 增加容器化部署（Docker Compose / K8s）说明
