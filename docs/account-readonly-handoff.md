# 账号数据只读模块 — 交接文档

> 本文档供"新项目"的开发 AGENT 使用：新项目与 MaaYuan-Share-Backend 共用同一个 MongoDB 数据库，
> 账号数据由原项目负责写入（注册/登录/改密/关注），新项目**只读**消费用户数据。
> 本模块范围：实体 + 仓储 + 查询服务 + 公开信息 DTO。**不包含**注册、登录、JWT、邮件、Redis 缓存（可选）。

---

## 1. 数据库信息

- 库名：`MaaBackend`（示例连接串 `mongodb://192.168.31.21:27017/MaaBackend`，按实际环境配置；如有认证则加用户名密码）
- 账号相关集合（共 3 个，本方案只用到第 1 个）：
  | 集合 | 用途 | 本方案 |
  |---|---|---|
  | `maa_user` | 用户主表 | ✅ 只读使用 |
  | `user_fans` | 粉丝关系（userId -> fansList） | 不需要 |
  | `user_following` | 关注关系（userId -> followList） | 不需要 |

## 2. 数据契约：`maa_user`

MongoDB 文档字段名 = Kotlin 属性名（无 @Field 重命名），`_id` 即 `userId`：

| 字段 | 类型 | 说明 |
|---|---|---|
| _id / userId | String（ObjectId hex，24 位） | 主键，由 MongoDB 生成；对外 API 中叫 `id` |
| userName | String | 用户名，**普通索引（非唯一）** |
| email | String | 邮箱，**唯一索引** |
| password | String | BCrypt 哈希（只读方案不触碰） |
| status | Int | 0=未激活 / 1=激活 / 2=管理员 |
| pwdUpdateTime | Instant | 密码最后修改时间 |
| followingCount | Int | 关注数（写侧维护） |
| fansCount | Int | 粉丝数（写侧维护） |

关键语义（必须与原项目一致）：
1. userId 是 MongoDB 生成的 ObjectId 字符串，**禁止自造 ID 写入**。
2. email 按原样精确匹配（区分大小写）。
3. 用户名搜索**只返回 status=1 的已激活用户**，正则匹配、大小写不敏感。
4. 查不到用户时使用哨兵对象 `MaaUser.UNKNOWN` 兜底（userName="未知用户:("）。
5. `hasAdminPrivileges` 判定：status >= 2。
6. `MaaUser` 实体的索引注解必须与原项目**一字不差**（Spring Data 启动时会校验/创建索引，定义冲突会导致启动失败；`email` 唯一索引是数据安全的根基）。

## 3. 需要移植的代码（可直接照抄，包名可改，**字段与注解不要改**）

### 3.1 实体（原样）

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("maa_user")
data class MaaUser(
    @Id
    val userId: String? = null,
    @Indexed
    var userName: String,
    @Indexed(unique = true)
    val email: String,
    var password: String,
    var status: Int = 0,
    var pwdUpdateTime: Instant = Instant.MIN,
    var followingCount: Int = 0,
    var fansCount: Int = 0,
) : Serializable {
    companion object {
        @Transient
        val UNKNOWN: MaaUser = MaaUser(
            userId = "",
            userName = "未知用户:(",
            email = "unknown@unkown.unkown",
            password = "unknown",
        )
    }
}
```

依赖：`spring-boot-starter-data-mongodb`、`com.fasterxml.jackson.module:jackson-module-kotlin`、`java.time.Instant`。

### 3.2 仓储（只读方法）

```kotlin
interface UserRepository : MongoRepository<MaaUser, String> {
    fun findByUserId(userId: String): MaaUser?

    @Query("{ 'userName': { '\$regex': ?0, '\$options': 'i' }, 'status': 1 }")
    fun searchUsers(userName: String, pageable: Pageable): Page<MaaUserInfo>
}
```

说明：
- 批量查询直接用继承自 MongoRepository 的 `findAllById(ids: Iterable<String>)`。
- 原项目还有 `findByEmail` / `existsByUserName`，只读方案不需要。
- `searchUsers` 返回 `Page<MaaUserInfo>` 是 Spring Data 的 DTO 投影；若你的项目版本不支持，
  可改为返回 `Page<MaaUser>` 再逐条 `MaaUserInfo(user)` 转换，查询条件和分页逻辑不变。

### 3.3 公开信息 DTO

```kotlin
data class MaaUserInfo(
    val id: String,
    val userName: String,
    val activated: Boolean = false,
    val followingCount: Int = 0,
    val fansCount: Int = 0,
) {
    constructor(user: MaaUser) : this(
        id = user.userId!!,
        userName = user.userName,
        activated = user.status == 1,
        followingCount = user.followingCount,
        fansCount = user.fansCount,
    )
}
```

注意：`activated = (status == 1)` 是原项目的判定逻辑，保持口径一致。

### 3.4 查询服务（从原项目 UserService 抽取的只读部分）

```kotlin
@Service
class UserQueryService(
    private val userRepository: UserRepository,
) {
    /** 按 id 查用户，查不到返回 UNKNOWN 哨兵（避免到处判空） */
    fun findByUserIdOrDefault(id: String): MaaUser =
        userRepository.findByUserId(id) ?: MaaUser.UNKNOWN

    /** 批量查询；UserDict 提供 get(id) 与 getOrDefault(id) */
    fun findByUsersId(ids: Iterable<String>): UserDict =
        UserDict(userRepository.findAllById(ids).toList())

    /** 单个用户的公开信息 */
    fun get(userId: String): MaaUserInfo? =
        userRepository.findByUserId(userId)?.run(::MaaUserInfo)

    /** 用户名模糊搜索（仅已激活用户，分页） */
    fun search(userName: String, pageable: Pageable): Page<MaaUserInfo> =
        userRepository.searchUsers(userName, pageable)

    /** 管理员判定：status >= 2 */
    fun hasAdminPrivileges(userId: String?): Boolean =
        !userId.isNullOrBlank() && findByUserIdOrDefault(userId).status >= 2

    class UserDict(users: List<MaaUser>) {
        private val userMap = users.associateBy { it.userId!! }
        fun entries() = userMap.entries
        operator fun get(id: String): MaaUser? = userMap[id]
        fun getOrDefault(id: String) = get(id) ?: MaaUser.UNKNOWN
    }
}
```

可选优化（原项目有，新项目按需）：
- 原项目用 Caffeine 做本地用户缓存（`expireAfterAccess 3 天`），**注意：共用库但各自 JVM，原项目用户改名后，你的缓存最长 3 天读到旧名**。建议要么不做缓存、要么缩短 TTL；缓存对象不要存 password。

### 3.5 建议暴露的 API（按新项目需要取舍，语义对齐原项目）

- `GET /user/info?userId={id}` → `MaaUserInfo`；查不到返回 404
- `GET /user/search?userName={name}&page=1&size=10` → 分页 `MaaUserInfo`（size 上限 50）
- 服务内部用 `findByUsersId` 批量查作者名/头像等

## 4. 硬性约束（不要做）

1. **只读** `maa_user`：禁止 insert/update/delete；账号的一切写入由原项目负责。
2. **不要新建自己的用户集合**（如 `app_user`），否则用户源分叉。
3. **不要实现**注册 / 登录 / 改密 / 邮箱验证码 / JWT / Spring Security（本方案范围外）。
4. **不要改动数据库既有索引**；实体索引注解与原项目保持一致。
5. 建议配置 `spring.data.mongodb.auto-index-creation: true`（与原项目一致，保证索引定义幂等）。
6. 序列化注意：原项目全局使用 `SNAKE_CASE` 属性命名（`user_name`、`following_count`），如需接口对拍保持一致，按你项目规范取舍。

## 5. 风险与注意事项

- **缓存陈旧**：本地缓存用户信息最多 3 天陈旧（原项目行为），可接受或缩短 TTL。
- **字段演进**：`maa_user` 以后加字段是安全的；改/删字段必须与原项目协调发版。
- **性能**：`searchUsers` 是 `$regex` 前缀无关的模糊匹配，注意调用频率与数据量（可加前缀锚定优化，但查询行为要与原项目一致时需谨慎）。
- **email 唯一索引已存在于库中**：新项目实体若声明 `@Indexed(unique = true)` 与之完全一致则幂等，不会出问题。

## 6. 验证建议

1. 连上同一库，用现有任意 userId 调 `get` 验证能查到真实用户。
2. `search` 验证：未激活用户（status=0）不会出现在结果里；中文/大小写匹配与源项目一致。
3. 写一个集成测试：对 `maa_user` 只读的仓储只调用查询方法。
