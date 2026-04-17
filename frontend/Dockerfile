# ============================================
# Frontend Dockerfile: Multi-stage build
# Stage 1: Node.js build → Stage 2: Nginx serving
# ============================================

# --- Stage 1: Build ---
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 설치 (캐시 활용)
COPY package.json package-lock.json* yarn.lock* ./
RUN npm install

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# --- Stage 2: Nginx ---
FROM nginx:1.25-alpine

# 기본 설정 제거 후 커스텀 설정 복사
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드 결과물 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 보안: 비-root 사용자로 실행하기 위한 권한 설정
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# 헬스체크
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
