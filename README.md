# ZootPlusBackendCenter

使用 Kotlin 编写的 ZOOT Plus 服务器后端

## 开发技术栈

- kotlin 2.1 (Java 21)
- SpringBoot 3
  - spring-security
  - springdoc-openapi
- MongoDB
- Redis

## 本地开发指南

1. 使用你喜欢的 IDE 导入此项目，复制 `/src/main/resources/application-template.yml` 到同目录下，命名为
   `application-dev.yml`，修改数据库配置以符合你自己配置的环境。
2. 下载安装 JDK 21 或者以上版本的
   jdk，可以考虑从 [zuluJDK](https://www.azul.com/downloads/?version=java-17-lts&package=jdk)
   或者 [libreicaJDK](https://bell-sw.com/pages/downloads/#/java-17-lts) 下载安装。 Jetbrains Idea 可以使用自带的 JDK
   管理器进行下载。
3. 你需要一个有redis和mongoDB的环境，如果你是windows用户，可以从 https://github.com/redis-windows/redis-windows/releases
   中下载使用。 您也可以直接使用 `./dev-docker/docker-compose.yml` 来启动 docker 服务。
4. 运行 `./gradlew bootRun`, windows 环境为 `./gradlew.bat bootRun`
5. 首次运行建议修改配置文件中的 `maa-copilot.task-cron.ark-level` 配置，这样可以将明日方舟中的关卡数据同步到你本地的
   mongodb 中，为了防止反复调用造成调试的麻烦，建议首次运行同步成功后再将配置修改回去

## 管理员账号配置

- 系统根据 `maa_user.status` 字段判定管理员，`status >= 2` 的账号会自动获得与作业作者等同的管理权限。
- 若需提升账号权限，可在 MongoDB 中执行更新：`db.maa_user.updateOne({userId: "<目标ID>"}, {$set: {status: 2}})`。
- 修改后建议清理缓存或重启服务，确保最新权限即时生效。

## 作业搜索与评分

- `GET /copilot/query` 的 `document` 参数支持标题、描述的分词匹配和忽略大小写的字面子串匹配。例如 `yuana` 可以匹配 `伏击阵1-yuanassist用`，单字关键词也会参与过滤；正则符号按普通字符处理。关卡、作者、标签、公开状态等筛选条件仍同时生效。
- `POST /copilot/rating` 写入评分后会清除对应详情缓存，并同步清除包含该作业的首页缓存，随后重新查询可获得更新后的点赞数。`Like` / `Dislike` / `None` 表示设置评分；重复设置同一种评分不累加，`None` 取消评分。
- 接口路径、请求及响应结构不变，不需要重新生成客户端。子串匹配使用 MongoDB 正则查询，数据量增长后需关注搜索耗时。

## 项目结构

- config # 存放 spring 配置
- common # 共享的逻辑
- controller # 交互层
  - request # 入参类型
  - response # 响应类型
- repository # 数据仓库层，用于和数据库交互
  - entity # 与数据库字段对应的类型
- service # 业务处理层，复杂或者公用逻辑放在这里
  - model # 应用内传输用类型放这里

## 编译与部署

1. 安装 JDK 21，可以考虑从 [zuluJDK](https://www.azul.com/downloads/?version=java-17-lts&package=jdk)
   或者 [libreicaJDK](https://bell-sw.com/pages/downloads/#/java-17-lts) 下载
2. clone 此项目 `git clone https://github.com/ZOOT-Plus/ZootPlusBackend.git`
3. 进入此项目目录 `cd ZootPlusBackend`
4. 编译项目 `./gradlew bootJar`，windows 环境下请使用 `gradlew.bat bootJar`
5. 获得编译后的 jar 文件 `cp ./build/libs/ZootPlusBackend-1.0-SNAPSHOT.jar .`
6. 复制一份配置文件 `cp ./build/resources/main/application-template.yml ./application-prod.yml`
7. 修改配置文件 `application-prod.yml`
8. 运行项目 `java -jar ZootPlusBackend-2.0.jar --spring.profiles.active=prod`

## native 编译（暂时废弃，如果希望协助维护，请查看native分支）

1. 安装 [GraalVM](https://www.graalvm.org/downloads/)
   Java21，并配置好环境变量，部分功能需要正确配置 `JAVA_HOME` 变量为 GraalVM 安装目录才能正常使用
2. 如果您处于 Windows 环境下，需要安装 `Visual Studio` 并且安装 C++ 组件，Linux 环境下则需要安装 `gcc` 工具链，Mac
   下需要安装 `xcode`
   工具链，详情查看 [native-image#prerequisites](https://www.graalvm.org/latest/reference-manual/native-image/#prerequisites)
3. 在该项目目录下，执行 `./gradlew nativeRun` 或者 `.\gradlew.bat nativeRun` 编译并运行该项目
4. 如果您希望产生 docker image，请执行 `./gradlew bootBuildImage`

## Join us!

QQ Group: 724540644
