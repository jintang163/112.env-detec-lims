# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build
# 先拷贝 pom 利用 Docker 层缓存下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:8-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx768m"
COPY --from=build /build/target/lims-backend.jar /app/app.jar
# 本地存储目录(使用 local 存储类型时);挂载卷见 compose
RUN mkdir -p /app/lims-files
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
