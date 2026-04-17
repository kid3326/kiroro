# Implementation Plan: Retail Dashboard Web Application

## Overview

리테일 P&L 대시보드 웹 애플리케이션의 구현 계획입니다. 백엔드(Java 17 + Spring Boot 3.x)와 프론트엔드(Vue.js 3 + Vite + TypeScript)를 분리하여 개발하며, 의존성 순서에 따라 데이터 계층 → 백엔드 핵심 로직 → API 계층 → 프론트엔드 → 인프라 순으로 진행합니다.

## Tasks

- [x] 1. 프로젝트 초기 설정 및 데이터베이스 스키마 구성
  - [x] 1.1 Spring Boot 백엔드 프로젝트 초기화
    - Spring Boot 3.x + Java 17 프로젝트 생성 (Gradle)
    - 의존성 추가: Spring Web, Spring Data JPA, Spring Security, Spring Scheduler, PostgreSQL Driver, Redis, Lombok, MapStruct
    - `application.yml` 프로파일 설정 (local, staging, production)
    - 패키지 구조 생성: controller, service, repository, domain, config, dto, exception
    - _Requirements: 16.1_

  - [x] 1.2 Vue.js 프론트엔드 프로젝트 초기화
    - Vite + Vue.js 3 + TypeScript 프로젝트 생성
    - 의존성 추가: PrimeVue, Apache ECharts (vue-echarts), Pinia, Vue Router, Axios
    - 디렉토리 구조 생성: components/, views/, stores/, services/, router/
    - PrimeVue 테마 및 글로벌 설정
    - _Requirements: 13.1_

  - [x] 1.3 PostgreSQL 데이터베이스 스키마 생성
    - Flyway 마이그레이션 설정
    - 사용자 및 세션 테이블 생성 (users, sessions)
    - 상품 계층 테이블 생성 (product_categories, product_subcategories, brands, products)
    - 거래 및 비용 테이블 생성 (sales_transactions, advertising_costs, inventory)
    - 집계 테이블 생성 (daily_aggregates, monthly_aggregates, budget_targets)
    - 필터, 알림, 리포트 테이블 생성 (saved_filters, alert_configs, alerts, report_schedules, report_history)
    - 감사 로그 및 설정 테이블 생성 (audit_logs, configurations)
    - sales_transactions, daily_aggregates, audit_logs에 월별 파티셔닝 적용
    - 설계 문서의 인덱스 전략에 따른 인덱스 생성
    - _Requirements: 14.1, 14.2, 14.3, 14.8_

  - [ ]* 1.4 데이터베이스 마이그레이션 테스트 작성
    - Flyway 마이그레이션 정상 실행 테스트
    - 파티셔닝 동작 확인 테스트
    - 인덱스 존재 여부 확인 테스트
    - _Requirements: 14.1, 14.2_

- [x] 2. 사용자 인증 및 권한 관리 (백엔드)
  - [x] 2.1 User 엔티티 및 Repository 구현
    - User JPA 엔티티 생성 (id, username, password_hash, email, role, assigned_brand, is_active, failed_login_count, locked_until)
    - Session JPA 엔티티 생성 (id, user_id, token, expires_at, ip_address)
    - UserRepository, SessionRepository 인터페이스 구현
    - Role enum 정의 (CEO, EXECUTIVE, MARKETING, FINANCE, PRODUCT)
    - _Requirements: 3.1_

  - [x] 2.2 AuthenticationService 구현
    - 로그인 로직: 자격 증명 검증, bcrypt(cost=12) 비밀번호 비교
    - 세션 생성: 60분 타임아웃, Redis에 세션 저장
    - 동시 로그인 방지: 기존 세션 종료 후 새 세션 생성
    - 비밀번호 정책 검증: 8자 이상, 대소문자 + 숫자 포함
    - 계정 잠금: 5회 연속 실패 시 30분 잠금
    - 로그아웃 및 세션 만료 처리
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 15.3, 15.4_

  - [x] 2.3 AuthorizationService 및 Spring Security 설정
    - 역할별 데이터 접근 권한 매트릭스 구현
    - Finance/Executive: 비용 데이터 접근 허용
    - Marketing/Product: 비용 데이터 접근 거부
    - Marketing: 광고비 데이터 접근 허용
    - 전체 역할: 매출/판매량 데이터 접근 허용
    - 브랜드별 데이터 접근 제한 (assigned_brand 기반)
    - Spring Security FilterChain 설정 (세션 기반 인증)
    - CORS 설정
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 15.1_

  - [x] 2.4 AuthController REST API 구현
    - POST `/api/v1/auth/login` - 로그인
    - POST `/api/v1/auth/logout` - 로그아웃
    - GET `/api/v1/auth/session` - 세션 확인
    - 세션 만료 시 401 응답 및 리다이렉트 지원
    - _Requirements: 2.1, 2.2, 2.7_

  - [x] 2.5 AuditService 및 AuditLog 구현
    - AuditLog 엔티티 생성 (user_id, event_time, event_type, data_type, data_scope, query_type, access_result, ip_address)
    - AuditLogRepository 구현
    - AOP 기반 감사 로깅 인터셉터 구현 (모든 데이터 접근 시 자동 기록)
    - 감사 로그 내보내기 API
    - _Requirements: 3.7, 14.7, 15.7_

  - [ ]* 2.6 인증/권한 단위 테스트 작성
    - 로그인 성공/실패 테스트
    - 세션 타임아웃 테스트
    - 동시 로그인 방지 테스트
    - 역할별 데이터 접근 권한 테스트
    - 계정 잠금 테스트
    - 감사 로그 기록 테스트
    - _Requirements: 2.1, 2.4, 3.2, 3.3, 15.4_

- [x] 3. Checkpoint - 인증/권한 기능 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Mock API 클라이언트 및 데이터 수집
  - [x] 4.1 Mock API 서버 구현
    - Spring Boot 내 별도 프로파일로 Mock API 엔드포인트 구현
    - `/mock/api/sales` - 판매 데이터 반환
    - `/mock/api/costs` - 광고비 데이터 반환
    - `/mock/api/inventory` - 재고 데이터 반환
    - `/mock/api/pnl` - P&L 메트릭 반환
    - 토큰 기반 인증 적용
    - 현실적인 테스트 데이터 생성 (Faker 또는 수동 시드 데이터)
    - _Requirements: 1.1, 1.2_

  - [x] 4.2 ApiClient 서비스 구현
    - RestTemplate 기반 MockApiClient 구현
    - RetryTemplate 설정: 최대 3회 재시도, 지수 백오프
    - 응답 데이터 스키마 검증 (JSON Schema Validator)
    - 검증 실패 시 에러 로깅 및 캐시 데이터 사용
    - _Requirements: 1.2, 1.4, 1.5, 1.6_

  - [x] 4.3 Spring Scheduler 기반 데이터 수집 스케줄러 구현
    - `@Scheduled(fixedRate = 3600000)` 60분 주기 데이터 수집
    - 수집된 데이터를 sales_transactions, advertising_costs, inventory 테이블에 저장
    - 수집 완료 후 Data Aggregator 트리거
    - _Requirements: 1.3_

  - [ ]* 4.4 Mock API 클라이언트 단위 테스트 작성
    - 정상 데이터 수집 테스트
    - 스키마 검증 실패 테스트
    - 재시도 로직 테스트
    - 캐시 폴백 테스트
    - _Requirements: 1.4, 1.5, 1.6_

- [x] 5. P&L 데이터 집계 및 계산 엔진
  - [x] 5.1 DataAggregationService 핵심 P&L 계산 구현
    - calculateTotalRevenue: 전체 매출 합계
    - calculateNetRevenue: 총매출 - 반품 - 할인
    - calculateCOGS: FIFO/LIFO/가중평균 재고 평가법 지원
    - calculateGrossProfit: 순매출 - 매출원가
    - calculateEBITDA: 매출총이익 - 영업비용(이자/세금/감가상각 제외)
    - calculateOperatingProfit: EBITDA - 감가상각비
    - calculateNetProfit: 영업이익 - 이자 - 세금
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.8, 4.9, 4.10_

  - [x] 5.2 광고 메트릭 및 비교 분석 계산 구현
    - 채널별(Naver, Google, Meta, Others) 광고비 집계
    - ROAS 계산: 채널별 매출 / 광고비
    - CAC 계산: 총 광고비 / 신규 고객 수
    - YoY 비교: 현재 기간 - 전년 동기
    - MoM 비교: 현재 월 - 전월
    - 예산 대비 실적 차이 계산
    - _Requirements: 4.5, 4.6, 4.7, 4.11, 4.12, 4.13_

  - [x] 5.3 상품 계층 집계 및 재고 계산 구현
    - 4단계 계층(Category → Subcategory → Brand → SKU) 집계
    - 각 계층 레벨별 판매량/매출 집계
    - 상품 변형(색상/사이즈)별 판매 추적
    - 번들 상품 매출 비례 배분
    - 재고 회전율 계산: COGS / 평균 재고 가치
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [x] 5.4 사전 집계 배치 처리 구현
    - 일별 집계 배치: daily_aggregates 테이블에 저장
    - 월별 집계 배치: monthly_aggregates 테이블에 저장
    - 데이터 수집 완료 후 자동 집계 트리거
    - Redis 캐시 갱신
    - _Requirements: 8.5_

  - [ ]* 5.5 P&L 계산 단위 테스트 작성
    - 각 P&L 메트릭 계산 정확성 테스트
    - COGS 재고 평가법별 테스트
    - YoY/MoM 비교 계산 테스트
    - 상품 계층 집계 테스트
    - 번들 매출 배분 테스트
    - _Requirements: 4.1, 4.3, 4.6, 5.2, 5.4_

- [x] 6. Checkpoint - 데이터 수집 및 집계 엔진 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. 대시보드 API 및 필터링
  - [x] 7.1 DashboardController REST API 구현
    - GET `/api/v1/dashboard/summary` - P&L 요약 메트릭
    - GET `/api/v1/dashboard/revenue` - 매출 데이터 (시계열)
    - GET `/api/v1/dashboard/costs` - 비용 데이터 (권한 체크 포함)
    - GET `/api/v1/dashboard/advertising` - 광고비 데이터
    - GET `/api/v1/dashboard/products` - 상품별 데이터 (계층 지원)
    - GET `/api/v1/dashboard/inventory` - 재고 데이터
    - GET `/api/v1/dashboard/comparison` - YoY/MoM 비교
    - 공통 쿼리 파라미터: from, to, category, brand, sku, channel, granularity, page, size
    - 페이지네이션: 기본 size=50
    - _Requirements: 6.1, 7.3, 8.3_

  - [x] 7.2 FilterService 및 FilterController 구현
    - SavedFilter 엔티티 (user_id, name, filter_criteria as JSONB)
    - GET `/api/v1/filters` - 저장된 필터 목록
    - POST `/api/v1/filters` - 필터 저장
    - GET `/api/v1/filters/{id}` - 필터 로드
    - DELETE `/api/v1/filters/{id}` - 필터 삭제
    - GET `/api/v1/search/products` - 상품 검색 (이름/SKU, 500ms 이내 응답)
    - _Requirements: 7.1, 7.2, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 7.3 대시보드 API 통합 테스트 작성
    - 각 엔드포인트 응답 형식 테스트
    - 권한별 데이터 접근 테스트
    - 필터 적용 테스트
    - 페이지네이션 테스트
    - 상품 검색 성능 테스트
    - _Requirements: 3.2, 7.3, 7.7, 8.3_

- [x] 8. 내보내기 서비스
  - [x] 8.1 ExportService 구현 (Excel, PDF, PPT)
    - Apache POI 기반 Excel 내보내기: 데이터 카테고리별 시트 분리
    - iText/OpenPDF 기반 PDF 내보내기: 표지, 목차, 페이지 번호 포함
    - Apache POI (XSLF) 기반 PPT 내보내기: 차트당 1슬라이드 + 데이터 테이블
    - 차트 이미지 임베딩: 최소 300 DPI
    - 현재 필터 적용된 데이터 내보내기
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [x] 8.2 ExportController 및 파일 저장 구현
    - POST `/api/v1/export/excel` - Excel 내보내기 요청
    - POST `/api/v1/export/pdf` - PDF 내보내기 요청
    - POST `/api/v1/export/ppt` - PPT 내보내기 요청
    - GET `/api/v1/export/{id}/download` - 파일 다운로드
    - S3에 파일 저장, 24시간 유효 presigned URL 생성
    - _Requirements: 10.7_

  - [ ]* 8.3 내보내기 서비스 단위 테스트 작성
    - Excel 시트 구조 테스트
    - PDF 문서 구조 테스트
    - PPT 슬라이드 구조 테스트
    - presigned URL 생성 테스트
    - _Requirements: 10.1, 10.4, 10.5, 10.6_

- [x] 9. 알림 엔진
  - [x] 9.1 AlertEngine 서비스 구현
    - 매출 임계값 모니터링: 목표 대비 90% 미만 시 알림
    - 광고비 예산 모니터링: 예산 대비 95% 초과 시 알림
    - 판매량 이상 감지: 7일 이동평균 대비 20% 이탈 시 알림
    - 재고 부족 감지: 재주문점 이하 시 알림
    - 알림 심각도 분류: CRITICAL, IMPORTANT, INFORMATIONAL
    - 기본 임계값 설정 (초기 배포 시 히스토리 기반 추천)
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.8_

  - [x] 9.2 알림 발송 및 AlertController 구현
    - CRITICAL: 이메일(AWS SES) + SMS(AWS SNS) 발송
    - IMPORTANT: 이메일(AWS SES) 발송
    - INFORMATIONAL: 대시보드 내 알림
    - AlertConfig 엔티티 및 사용자별 임계값/알림 설정
    - GET `/api/v1/alerts` - 활성 알림 목록
    - PUT `/api/v1/alerts/config` - 알림 설정 변경
    - POST `/api/v1/alerts/{id}/acknowledge` - 알림 확인
    - _Requirements: 12.6, 12.7_

  - [ ]* 9.3 알림 엔진 단위 테스트 작성
    - 각 임계값 조건별 알림 트리거 테스트
    - 이상 감지 로직 테스트
    - 심각도 분류 테스트
    - 알림 발송 채널 테스트 (mock)
    - _Requirements: 12.1, 12.3, 12.5_

- [x] 10. 자동 리포트 스케줄링
  - [x] 10.1 ReportSchedulerService 구현
    - 일별/주별 리포트 스케줄 관리
    - "Monthly Executive Report" 템플릿: 매출, 이익, EBITDA, KPI 요약
    - "Weekly Marketing Report" 템플릿: 채널별 광고비, ROAS, CAC, 전환 메트릭
    - 수신자 설정: 이메일, 부서, 역할 기반
    - 스케줄 시간 커스터마이징
    - Spring Scheduler 기반 리포트 생성 및 이메일 발송 (AWS SES + PDF 첨부)
    - 10MB 초과 시 요약 이메일 + 다운로드 링크
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.9_

  - [x] 10.2 ReportController 및 리포트 이력 관리 구현
    - GET `/api/v1/reports/templates` - 리포트 템플릿 목록
    - POST `/api/v1/reports/schedules` - 스케줄 생성
    - GET `/api/v1/reports/history` - 리포트 이력 (90일 보관)
    - GET `/api/v1/reports/{id}/download` - 리포트 다운로드
    - ReportHistory 엔티티 관리
    - _Requirements: 11.8_

  - [ ]* 10.3 리포트 스케줄링 단위 테스트 작성
    - 리포트 템플릿 생성 테스트
    - 스케줄 실행 테스트
    - 이메일 발송 테스트 (mock)
    - 90일 이력 보관 테스트
    - _Requirements: 11.1, 11.4, 11.8_

- [x] 11. Configuration Parser 구현
  - [x] 11.1 ConfigurationParser 서비스 구현
    - Configuration 도메인 객체 정의
    - parse(): 설정 파일 문자열 → Configuration 객체 변환
    - prettyPrint(): Configuration 객체 → 일관된 들여쓰기/정렬의 설정 파일 문자열
    - validate(): 필수 필드 검증 (DB 연결 문자열, API 엔드포인트, 인증 설정)
    - 파싱 에러 시 라인 번호 및 문제 설명 포함 에러 메시지
    - 잘못된 필드 값에 대한 필드명 및 기대 형식 에러 반환
    - _Requirements: 17.1, 17.2, 17.3, 17.5, 17.6_

  - [x] 11.2 ConfigController REST API 구현
    - GET `/api/v1/config` - 현재 설정 조회
    - PUT `/api/v1/config` - 설정 업데이트
    - POST `/api/v1/config/validate` - 설정 검증
    - _Requirements: 17.1, 17.5_

  - [ ]* 11.3 Configuration Parser 단위 테스트 작성
    - 유효한 설정 파일 파싱 테스트
    - 잘못된 설정 파일 에러 메시지 테스트
    - round-trip 테스트: parse → prettyPrint → parse 결과 동등성 검증
    - 필수 필드 누락 검증 테스트
    - 잘못된 필드 값 검증 테스트
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_

- [x] 12. Checkpoint - 백엔드 전체 기능 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. 프론트엔드 공통 기반 및 인증 UI
  - [x] 13.1 Vue Router, Pinia 스토어, Axios 인터셉터 설정
    - Vue Router 설정: 로그인, 대시보드, 리포트, 알림 설정 라우트
    - 인증 가드: 세션 미인증 시 로그인 페이지 리다이렉트
    - Axios 인터셉터: 401 응답 시 로그인 페이지 리다이렉트, 요청 헤더에 세션 토큰 포함
    - authStore (Pinia): 로그인 상태, 사용자 정보, 역할 관리
    - _Requirements: 2.2, 15.1_

  - [x] 13.2 LoginView 구현
    - PrimeVue InputText, Password, Button 컴포넌트 활용
    - 로그인 폼: 사용자명, 비밀번호 입력
    - 로그인 실패 시 에러 메시지 표시
    - 계정 잠금 상태 안내 메시지
    - _Requirements: 2.1, 15.4_

  - [x] 13.3 공통 컴포넌트 구현
    - LoadingSkeleton.vue: 스켈레톤 로딩 UI
    - OfflineBanner.vue: 오프라인 상태 배너 (데이터 타임스탬프 표시)
    - AlertBanner.vue: 알림 배너 (심각도별 색상 구분)
    - _Requirements: 8.4, 9.3_

  - [ ]* 13.4 프론트엔드 공통 컴포넌트 단위 테스트 작성
    - LoginView 렌더링 및 폼 제출 테스트
    - 인증 가드 리다이렉트 테스트
    - 오프라인 배너 표시 테스트
    - _Requirements: 2.1, 2.2, 9.3_

- [x] 14. 대시보드 메인 뷰 및 차트 컴포넌트
  - [x] 14.1 DashboardView 레이아웃 구현
    - dashboardStore (Pinia): 대시보드 데이터 상태 관리, API 호출
    - KpiCard.vue: 주요 KPI 카드 (매출, 이익, EBITDA 등)
    - PnlSummary.vue: P&L 요약 테이블
    - 레이아웃: 상단 KPI 카드 → 차트 영역 → 데이터 테이블
    - above-the-fold 콘텐츠 2초 이내 렌더링 최적화
    - 차트/테이블 lazy loading 적용
    - _Requirements: 8.1, 8.2_

  - [x] 14.2 ECharts 시계열 차트 컴포넌트 구현
    - TimeSeriesChart.vue: 일별/주별/월별/분기별/연별 시계열 차트
    - BarComparisonChart.vue: 비교 막대 차트 (YoY, MoM, 예산 대비)
    - PieCompositionChart.vue: 구성비 파이 차트 (채널별 광고비, 카테고리별 매출)
    - DrillDownChart.vue: 데이터 포인트 클릭 시 상세 분석 드릴다운
    - 줌/팬 인터랙션 지원
    - 현재 기간 vs 이전 기간/예산 비교 오버레이
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 14.3 상품 계층 및 데이터 테이블 구현
    - ProductHierarchy.vue: 4단계 상품 계층 트리 (Category → Subcategory → Brand → SKU)
    - PrimeVue DataTable 활용: 페이지네이션 (50행/페이지), 정렬, 필터
    - 로딩 중 스켈레톤 스크린 표시
    - 5초 초과 쿼리 시 진행률 표시
    - _Requirements: 5.1, 8.3, 8.4, 8.6_

  - [ ]* 14.4 차트 컴포넌트 단위 테스트 작성
    - 각 차트 타입 렌더링 테스트
    - 시간 기간 변경 시 차트 업데이트 테스트
    - 드릴다운 인터랙션 테스트
    - _Requirements: 6.1, 6.2, 6.4_

- [x] 15. 필터링, 검색 및 내보내기 UI
  - [x] 15.1 필터 컴포넌트 구현
    - filterStore (Pinia): 필터 상태 관리
    - DateRangePicker.vue: PrimeVue Calendar 기반 날짜 범위 선택
    - MultiFilter.vue: 카테고리, 브랜드, SKU, 채널 다중 필터 (PrimeVue MultiSelect)
    - SavedFilters.vue: 필터 저장/로드/삭제 UI
    - 필터 적용 시 모든 차트/테이블 동시 업데이트
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 15.2 상품 검색 및 내보내기 다이얼로그 구현
    - 상품 검색: PrimeVue AutoComplete 활용, 이름/SKU 검색, 500ms 이내 결과 표시
    - ExportDialog.vue: 내보내기 형식 선택 (Excel/PDF/PPT), 진행 상태 표시, 다운로드 링크
    - _Requirements: 7.6, 7.7, 10.1_

  - [ ]* 15.3 필터/검색 컴포넌트 단위 테스트 작성
    - 날짜 범위 선택 테스트
    - 다중 필터 적용 테스트
    - 필터 저장/로드 테스트
    - 검색 결과 표시 테스트
    - _Requirements: 7.1, 7.2, 7.4, 7.7_

- [x] 16. Checkpoint - 프론트엔드 대시보드 핵심 기능 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. 리포트 및 알림 설정 UI
  - [x] 17.1 ReportView 구현
    - 리포트 템플릿 목록 표시
    - 스케줄 생성 폼: 템플릿 선택, 빈도(일별/주별), 시간, 수신자 설정
    - 리포트 이력 테이블: 생성일, 상태, 다운로드 링크
    - _Requirements: 11.1, 11.2, 11.3, 11.7, 11.8_

  - [x] 17.2 AlertConfigView 구현
    - alertStore (Pinia): 알림 상태 관리
    - 알림 임계값 설정 폼: 매출, 광고비, 이상 감지, 재고 임계값
    - 알림 채널 설정: 이메일, SMS, 푸시 알림 토글
    - 활성 알림 목록: 심각도별 색상, 확인 버튼
    - _Requirements: 12.5, 12.6, 12.7_

  - [ ]* 17.3 리포트/알림 UI 단위 테스트 작성
    - 스케줄 생성 폼 테스트
    - 알림 설정 폼 테스트
    - 알림 목록 렌더링 테스트
    - _Requirements: 11.1, 12.7_

- [x] 18. PWA 및 모바일 지원
  - [x] 18.1 Service Worker 및 PWA 설정
    - vite-plugin-pwa 설정
    - Service Worker 등록: 오프라인 캐시 전략 (Cache First for static, Network First for API)
    - Web App Manifest 설정: 아이콘, 테마 색상, 홈 화면 설치
    - Push Notification 구현: CRITICAL 알림 시 푸시 알림 발송
    - _Requirements: 13.1, 13.5, 13.6_

  - [x] 18.2 오프라인 모드 및 캐시 서비스 구현
    - cacheService.ts: LocalStorage 기반 데이터 캐싱 (50MB 제한)
    - 오프라인 감지: navigator.onLine 이벤트 리스너
    - 오프라인 시 캐시 데이터 로드 + 오프라인 배너 표시
    - 온라인 복구 시 최신 데이터 자동 갱신
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 18.3 반응형 레이아웃 및 모바일 뷰 구현
    - MobileDashboardView.vue: 모바일 전용 간소화 뷰 (KPI만 표시)
    - CSS 미디어 쿼리: 모바일/태블릿/데스크톱 브레이크포인트
    - 터치 제스처: pinch-to-zoom, swipe 차트 네비게이션
    - 오프라인 시 읽기 전용 모드
    - _Requirements: 13.2, 13.3, 13.4, 13.7_

  - [ ]* 18.4 PWA 및 오프라인 모드 단위 테스트 작성
    - 캐시 서비스 저장/로드 테스트
    - 50MB 제한 테스트
    - 오프라인 감지 테스트
    - 반응형 레이아웃 테스트
    - _Requirements: 9.1, 9.5, 13.2_

- [x] 19. Checkpoint - 프론트엔드 전체 기능 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 20. 데이터 아카이빙 및 백업
  - [x] 20.1 데이터 아카이빙 서비스 구현
    - 2년 이상 데이터를 S3 Glacier로 이동하는 배치 작업
    - 5년 이상 데이터 자동 삭제
    - 파티션 자동 생성 스케줄러 (월별 파티션 사전 생성)
    - _Requirements: 14.3, 14.4_

  - [x] 20.2 백업 및 복원 설정
    - 일별 자동 백업 설정 (pg_dump 기반 또는 RDS 스냅샷)
    - 백업 보관: 30일 일별, 12개월 월별, 5년 연별
    - 백업 완료 후 무결성 검증 (테스트 복원)
    - AES-256 데이터 암호화 설정 (RDS 암호화)
    - _Requirements: 14.5, 14.6, 14.8_

  - [ ]* 20.3 아카이빙 및 백업 단위 테스트 작성
    - 아카이빙 대상 데이터 선별 테스트
    - 파티션 생성 테스트
    - _Requirements: 14.3, 14.4_

- [x] 21. Docker 컨테이너화 및 CI/CD 파이프라인
  - [x] 21.1 Docker 설정
    - 백엔드 Dockerfile: Java 17 + Spring Boot JAR 빌드
    - 프론트엔드 Dockerfile: Node.js 빌드 → Nginx 서빙
    - docker-compose.yml: 프론트엔드, 백엔드, PostgreSQL, Redis 로컬 개발 환경
    - .dockerignore 설정
    - _Requirements: 16.1_

  - [x] 21.2 GitHub Actions CI/CD 파이프라인 구현
    - CI 워크플로우: 코드 푸시 시 테스트 실행, Docker 이미지 빌드
    - main 브랜치 푸시 시: 테스트 → 빌드 → ECR 푸시 → staging 배포
    - staging 검증 후 수동 승인 → production 배포
    - ECR(Elastic Container Registry) 이미지 저장소 설정
    - _Requirements: 16.3, 16.4, 16.5_

  - [x] 21.3 AWS ECS Fargate 인프라 설정
    - ECS 클러스터, 태스크 정의, 서비스 설정
    - ALB(Application Load Balancer) 설정: 프론트엔드/백엔드 라우팅
    - Auto Scaling 설정: CPU 70% 초과 5분 시 스케일 아웃, 최대 30명 동시 사용자 지원
    - RDS PostgreSQL 인스턴스 설정
    - ElastiCache Redis 인스턴스 설정
    - S3 버킷 설정: 내보내기 파일, 콜드 스토리지
    - _Requirements: 16.2, 16.6, 16.9_

  - [x] 21.4 모니터링 및 에러 추적 설정
    - AWS CloudWatch 로그 그룹 및 메트릭 설정
    - CloudWatch 알람: CPU, 메모리, 응답 시간 모니터링
    - Sentry 연동: Spring Boot + Vue.js 에러 추적 설정
    - _Requirements: 16.7, 16.8_

  - [ ]* 21.5 Docker 빌드 및 CI 파이프라인 테스트
    - Docker 이미지 빌드 테스트
    - docker-compose 로컬 환경 기동 테스트
    - CI 워크플로우 문법 검증
    - _Requirements: 16.1, 16.3_

- [x] 22. 보안 강화 및 컴플라이언스
  - [x] 22.1 보안 설정 구현
    - HTTPS/TLS 1.2+ 설정 (ALB SSL 인증서)
    - Spring Security CSRF 보호 설정
    - 민감 데이터 암호화: 비밀번호(bcrypt), 금융 데이터, 개인정보 (AES-256)
    - API Rate Limiting 설정
    - _Requirements: 15.1, 15.2, 2.6, 2.7_

  - [x] 22.2 컴플라이언스 기능 구현
    - 개인정보 수집 동의 화면 및 동의 기록 저장
    - 거래 기록 5년 보관 정책 적용
    - 데이터 접근 로그 내보내기 기능 (컴플라이언스 감사용)
    - _Requirements: 15.5, 15.6, 15.7_

  - [ ]* 22.3 보안 테스트 작성
    - HTTPS 강제 리다이렉트 테스트
    - 비밀번호 암호화 테스트
    - 권한 없는 데이터 접근 차단 테스트
    - 감사 로그 내보내기 테스트
    - _Requirements: 15.1, 15.2, 15.4, 15.7_

- [x] 23. 프론트엔드-백엔드 통합 및 E2E 연결
  - [x] 23.1 프론트엔드-백엔드 API 연동 완성
    - apiService.ts: 모든 백엔드 API 엔드포인트 연동 확인
    - 인증 플로우 E2E 연결: 로그인 → 세션 → 대시보드 → 로그아웃
    - 대시보드 데이터 로딩 플로우 연결
    - 필터 적용 → 차트/테이블 업데이트 플로우 연결
    - 내보내기 요청 → 다운로드 플로우 연결
    - 알림 설정 → 알림 수신 플로우 연결
    - _Requirements: 2.1, 6.2, 7.3, 10.1, 12.7_

  - [ ]* 23.2 통합 테스트 작성
    - 인증 플로우 통합 테스트
    - 대시보드 데이터 로딩 통합 테스트
    - 필터 적용 통합 테스트
    - 내보내기 플로우 통합 테스트
    - _Requirements: 2.1, 7.3, 10.1_

- [x] 24. Final Checkpoint - 전체 시스템 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- `*` 표시된 작업은 선택 사항이며, 빠른 MVP를 위해 건너뛸 수 있습니다
- 각 작업은 관련 요구사항 번호를 참조하여 추적 가능합니다
- Checkpoint 작업에서 모든 테스트를 실행하고 문제가 있으면 사용자에게 확인합니다
- 백엔드는 Java 17 + Spring Boot 3.x, 프론트엔드는 Vue.js 3 + Vite + TypeScript를 사용합니다
- 단위 테스트는 백엔드 JUnit 5 + Mockito, 프론트엔드 Vitest + Vue Test Utils를 사용합니다
