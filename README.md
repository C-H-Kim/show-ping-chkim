# ShowPing V3 - Live Commerce Refactoriing & Performance Tuning

**🏆 SSG I&C 5차수 부트캠프 최우수 프로젝트 선정**

> ⚠️ 이 레포지토리는 부트캠프 팀 프로젝트를 기반의 **개인적으로 성능 개선을 진행한 V3 버전**입니다.

> 원본/팀 리팩토링 버전은 아래 링크를 참고해주세요.
> - V1: Bootcamp Final Project (원본) - [링크](https://github.com/ShowPingPrj/show-ping-live)
> - V2: Team Refactoring Version - [링크](https://github.com/ShowPingPrj/show-ping-refactoring)

---

## 1. Overview

**ShowPing**은 실시간 라이브 스트리밍을 통해 상품을 소개하고
시청자가 채팅과 장바구니/결제를 통해 바로 구매할 수 있는 **라이브 커머스 서비스**입니다.

- 판매자는 라이브 방송을 생성하고 상품 정보를 등록합니다.
- 시청자는 실시간 스트리밍/채팅을 보면서 상품을 장바구니에 담고 결제할 수 있습니다.
- 관리자는 회원, 주문, 방송 내역을 관리합니다.

이 레포지토리(V3)는 아래 두 가지를 목표로 합니다.

1. DB 검색 방식 개선(인덱스 튜닝)을 통한 쿼리 성능 최적화
2. 캐시 도입을 통한 응답 시간 개선

---

## 2. Version History

이 프로젝트는 다음과 같은 단계로 발전했습니다.
### 🔹 V3 - 개인 성능 개선 Version (이 레포지토리)
- 개발 기간 : 2026.01 ~ 진행 중
- V1, V2를 기반으로 **개인적으로 추가 개선 작업을 진행한 버전**
- 주요 내용
    - **DB 성능 개선(쿼리/인덱스 튜닝 + 부하 테스트)**
    - **캐시 도입을 통한 응답 시간 개선**(진행 중)

### 🔹 V2 - Team Refactoring Version
- 개발 기간 : 2025.05 ~ 2025.10 (5개월)
- 팀 단위로 코드 구조/품질 개선 및 기능 보완
- 주요 내용
  - 도메인/레이어 구조 재정리
  - 공통 응답 구조, 예외 처리 정비
  - 일부 비즈니스 로직 리팩토링
- V2 레포지토리에서 **팀 전체가 함께 유지보수 가능한 형태**로 만드는 데 집중

### 🔹 V1 - Bootcamp Final Project
- 개발 기간 : 2025.02 ~ 2025.03 (2개월)
- 부트캠프에서 팀 단위로 진행한 **최초 버전**
- 핵심 기능 위주로 빠르게 구현
    - 회원가입/로그인
    - 라이브 방송 생성 및 시청
    - 채팅, 장바구니, 주문/결제 등

---

## 3. Tech Stack
### 🖥 Backend
- JAVA 17
- Spring Boot 3.x
- Spring Data JPA(Hibernate)
- Spring Security(JWT + Cookie 기반 인증)
- QueryDSL

### 💾 Database & Cache
- MySQL(AWS RDS)
- Redis(Refresh Token 용도)

### ☁ Infra & DevOps
- Docker
- Nginx(Reverse Proxy / HTTPS)
- AWS
  - EC2
  - RDS(MySQL)
  - S3(정적 리소스/업로드 파일)
- CI/CD (GitHub Actions)

### 🛠 Tools & Collaboration
- JMeter(성능/부하 테스트)
- Swagger(API 명세)
- Figma
- Notion
- Slack

---

## 4. What I Improved(개선 포인트)

### 4.1 DB 성능 개선 - 쿼리/인덱스 튜닝

**상황(S)**
- `LIKE %키워드%` 방식의 상품 검색을 사용함에 따라, 데이터 증가 시 인덱스를 활용하지 못하는 Full Table Scan 발생 확인
- 약 12만 건의 데이터 환경에서 희귀 키워드 검색 시 불필요하게 수만 건의 행을 스캔하여 DB I/O 부하 급증

**접근 방법(A)**
1. **쿼리/실행 계획 분석**
    - `EXPLAIN`을 이용해 검색 쿼리 실행 계획 분석
    - `WHERE` 조건 + `ORDER BY` + `LIMIT` 조합을 기준으로 실제 사용하는 인덱스 확인

2. **인덱스 및 쿼리 튜닝**
    - FULLTEXT 인덱스 추가 설계(`product_name` 컬럼)
    - `SELECT *` 제거, 실제로 필요한 컬럼만 조회하도록 쿼리 슬림화

3. **성능 테스트로 검증**
    - 검색어를 5개의 그룹으로 나누어서 측정
      - 흔한 상품명
      - 희귀 상품명
      - 흔한 브랜드명
      - 희귀 브랜드명
      - 매칭 0건 검색어
    - `EXPLAIN ANALYZE`를 이용해 실제 쿼리 실행 시간과 스캔 행 수 측정
    - JMeter로 검색 API에 대해 테스트
    - 개선 전/후의 50th percentile, 90th percentile 응답 시간, 쿼리 실행 시간, 스캔 행 수를 비교

**결과(R)**
- 상품 데이터 12만 건 기준, 검색 조회 90th 쿼리 실행 시간
<br>`272.88ms -> 47.11ms`(**약 82.74% 개선**)
- 스캔 행 수
<br>`320,288개 -> 39,039개`(**약 87.81% 감소**)
- 90th 응답 시간
<br>`404.47ms -> 307.96ms`(**약 23.86% 개선**)

**학습(L)**
- 성능 문제는 단순히 나중에 튜닝으로 해결하기보다<br>
초기 설계 단계에서 인덱스 전략을 함께 고민해야 한다는 것을 체감
- 데이터가 매우 흔한 경우(LIKE로도 금방 찾는 경우),<br>
오히려 FULLTEXT 인덱스를 읽고 정렬하는 오버헤드가 더 클 수 있음을 확인
- 무조건적인 인덱스 적용보다 데이터의 특성(희귀도, 분포도 등)에 맞는<br>
검색 전략이 성능에 미치는 영향을 직접 체득

---

## 5. Architecture
<img width="4052" height="1802" alt="시스템 아키텍처_전체_4팀(채팅 기반 라이브 스트리밍 서비스)" src="https://github.com/user-attachments/assets/699728c5-c1c9-435c-a263-c46b531d1ae1" />