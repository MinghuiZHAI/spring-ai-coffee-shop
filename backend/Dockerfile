# ---- 阶段一：构建 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline            # 依赖层缓存
COPY src ./src
RUN mvn -B package -DskipTests

# ---- 阶段二：提取分层 ----
FROM eclipse-temurin:21-jre AS extractor
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ---- 阶段三：运行镜像（依赖层在前，业务层在后，改代码不重拉依赖）----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=extractor /app/app/dependencies/ ./
COPY --from=extractor /app/app/spring-boot-loader/ ./
COPY --from=extractor /app/app/snapshot-dependencies/ ./
COPY --from=extractor /app/app/application/ ./
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g"
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]