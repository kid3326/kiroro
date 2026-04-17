# ============================================
# Backend Dockerfile: Multi-stage build
# Stage 1: Gradle build → Stage 2: JRE 17 runtime
# ============================================

# --- Stage 1: Build ---
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle wrapper 및 빌드 설정 복사 (캐시 활용)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Gradle wrapper JAR 다운로드 (없는 경우)
RUN chmod +x gradlew && \
    if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then \
      GRADLE_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/') && \
      wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip && \
      unzip -q -j /tmp/gradle.zip "gradle-${GRADLE_VERSION}/lib/plugins/gradle-wrapper-*.jar" -d gradle/wrapper/ && \
      mv gradle/wrapper/gradle-wrapper-*.jar gradle/wrapper/gradle-wrapper.jar 2>/dev/null || true && \
      rm -f /tmp/gradle.zip; \
    fi

# 의존성 다운로드 (소스 변경 시 캐시 재사용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre

WORKDIR /app

# 보안: 비-root 사용자로 실행
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

# JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 비-root 사용자로 전환
USER appuser

# JVM 최적화 옵션
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
