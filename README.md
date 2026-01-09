# 🏛️ Mora (모아) - 입법 청원 정보 제공 서비스

> **"복잡한 청원, 3줄로 끝내다."**
> 대한민국 국회 국민동의청원과 청원24의 데이터를 한곳에 모아, AI로 알기 쉽게 분석하고 처리 결과를 실시간으로 알려주는 플랫폼입니다.

<br>

## 📖 프로젝트 소개

**Mora**는 흩어져 있는 청원 사이트(국회, 행정부)의 데이터를 통합 수집하여 제공합니다.
단순한 목록 제공을 넘어, **OpenAI(GPT-4o-mini)**를 활용해 2030 세대가 읽기 쉬운 **'카드뉴스 형태'**로 내용을 요약/분석하며, 사용자가 관심 있는 청원의 **처리 결과(심사, 답변 등)가 업데이트되면 이메일로 알림**을 제공하여 지속적인 정치 참여를 유도합니다.

<br>

## ✨ 주요 기능 (Key Features)

### 1. 🔍 하이브리드 데이터 수집 (Hybrid Data Collection)
- **국회 청원 (Type 1)**: Open API와 엑셀 파싱(Apache POI)을 결합하여 과거 데이터부터 실시간 현황까지 빈틈없이 수집합니다.
- **청원 24 (Type 0)**: API가 없는 정부 청원 사이트의 특성을 고려하여, **Selenium 기반의 동적 크롤링**으로 데이터를 확보합니다. (현재 크롤링 사용 가능 여부를 정확히 파악하지 못하여 기능만 있고 자정마다 실행은 되지 않고 있습니다.)

### 2. 🤖 AI 기반 카드뉴스 자동 생성
- 복잡하고 긴 청원 원문을 **GPT-4o-mini**에게 전달하여 다음과 같은 구조화된 데이터로 변환합니다.
    - **SubTitle**: 20대 타겟의 훅킹(Hooking) 소제목
    - **Summary**: 핵심 내용 3줄 요약
    - **Tags**: 긍정적 효과 / 부정적 우려사항 분석
    - **Needs**: 청원 개요

### 3. 🔔 스마트 상태 추적 및 알림 (Smart Batch System)
- **자정 배치(Midnight Batch)**: 매일 밤 실행되어 모든 청원의 상태(현재 국민동의청원 한정)를 자동으로 최신화합니다.
    - **달성 감지**: 기간이 끝나면 `마감` 상태로 변경합니다.
    - **결과 추적**: 국회 의안 시스템의 `위원회 회부`, `본회의 처리` 결과를 감지합니다.
    - **처리결과 감지**: 국민동의청원 OpenApi 내 `<청원 처리결과>` 텍스트를 업데이트합니다.
- **이메일 알림**: 청원 처리결과 상태 변화가 감지되면 해당 청원을 스크랩한 유저들에게 **비동기 이메일**을 발송합니다.
- (현재 국민동의청원 한정으로 모든 기능이 자정마다 돌아가며 결과추적, 처리결과 감지, 이메일 알림은 청원24도 자동으로 기능 구현은 되어있으나 자정마다 돌아가고 있지는 않음)

<br>

## 🛠️ 기술 스택 (Tech Stack)

### Backend
| Category | Technology | Description |
| :--- | :--- | :--- |
| **Framework** | **Spring Boot 3.4.1** | 핵심 서버 프레임워크 |
| **Language** | **Java 17** | 기본 개발 언어 |
| **Database** | **MySQL 8.0** | 메인 데이터베이스 (RDBMS) |
| **ORM** | **Spring Data JPA** | 객체-관계 매핑 및 쿼리 처리 (Hibernate 6.6.4) |
| **Security** | **Spring Security & OAuth2** | 인증/인가 및 구글 소셜 로그인 구현 |
| **API Docs** | **Swagger (SpringDoc)** | REST API 명세서 자동화 |

### Data Processing & AI (Core Features)
| Category | Technology | Description |
| :--- | :--- | :--- |
| **Crawling** | **Selenium** | 동적 웹 페이지(청원24, 국회 상세) 데이터 수집 및 봇 탐지 우회 |
| **Data Parsing** | **Apache POI** | 대용량 엑셀 데이터를 파싱하여 초기 데이터 적재 효율화 |
| **Batch** | **Spring @Async** | 크롤링, 상태 업데이트, 메일 발송 등 장기 실행 작업 비동기 처리 |
| **AI** | **OpenAI API (GPT-4o-mini)** | 청원 원문 분석, 훅킹(Hooking) 문구 생성, 긍정/부정 요인 추출 |

### Notification & Infrastructure
| Category | Technology | Description |
| :--- | :--- | :--- |
| **Email** | **JavaMailSender** | 청원 상태/결과 변경 시 비동기 알림 메일 발송 |
| **Server** | **AWS EC2 (Ubuntu)** | 클라우드 배포 환경 |
| **Web Server** | **Nginx** | 리버스 프록시 및 타임아웃 설정 |

<br>

## 💡 기술적 도전 및 해결 (Technical Highlights)

### 1. 동적 DOM 구조의 '청원24' 크롤링 정밀화
- **문제**: '청원24' 사이트는 페이지마다 제목과 날짜 요소의 위치가 미세하게 다르거나, 불필요한 뱃지 텍스트가 섞여 있어 정확한 파싱이 어려웠습니다.
- **해결**: 특정 클래스명에 의존하는 대신, **"처리기관"이라는 고정 텍스트를 기준으로 XPath의 `preceding`(역방향 탐색)** 문법을 사용하여 제목 위치를 동적으로 추적하는 로직을 구현했습니다. 최후의 수단으로 전체 텍스트 파싱 로직을 추가하여 안정성을 확보했습니다.

### 2. 글로벌 배포 환경의 Timezone 동기화
- **문제**: 로컬 개발 환경(KST)과 달리 AWS EC2(UTC) 배포 시 청원 날짜가 9시간 전으로 기록되는 데이터 정합성 문제가 발생했습니다.
- **해결**:
    1. Ubuntu 서버 타임존 변경 (`timedatectl set-timezone Asia/Seoul`)
    2. JVM 실행 옵션 추가 (`-Duser.timezone=Asia/Seoul`)
    3. 애플리케이션 시작 시점(`@PostConstruct`)에 `TimeZone.setDefault()` 설정
    -> **3중 안전장치**를 통해 데이터의 시간 정확성을 확보했습니다.

### 3. 대량 작업의 비동기 처리 최적화
- **문제**: 수천 건의 청원 상태를 업데이트하고 이메일을 발송하는 과정에서 단일 스레드 처리 시 타임아웃이 발생했습니다.
- **해결**: **`@Async`** 어노테이션을 활용하여 크롤링, 상태 체크, 이메일 발송을 별도의 스레드 풀에서 병렬로 처리하도록 아키텍처를 개선했습니다. Selenium 인스턴스 또한 재사용 로직을 적용하여 리소스 점유를 줄였습니다.

<br>

## 🚀 시작하기 (Getting Started)

### 1. 전제 조건
- Java 17
- Gradle 8.x 이상
- MySQL 8.0

### 2. 프로젝트 클론 및 설정
```bash
git clone [https://github.com/Club-PARD/00_BE.git](https://github.com/Club-PARD/00_BE.git)
cd mora
```

### 3. `application.yml` 설정

`src/main/resources/` 경로에 `application.yml` 파일을 생성하고 아래와 같이 데이터베이스, JWT, OAuth2 클라이언트 정보 등을 설정합니다.

```yaml
spring:
  application:
    name: youngyoung.server.mora

  logging:
    level:
      root: info

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://{DB}:3306/YY_mora?serverTimezone=UTC&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: {name}
    password: {PW}

  mail:
    host: smtp.gmail.com
    port: 587
    username: "{EMAIL}"
    password: "{PW}"
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

  jpa:
    show-sql: true
    database-platform: org.hibernate.dialect.MySQLDialect
    database: mysql
    hibernate:
      ddl-auto: update
    generate-ddl: false
    properties:
      hibernate:
        format_sql: true
        enable_lazy_load_no_trans: true

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: {ID}
            client-secret: {SECRET}
            redirect-uri: {BASE}/login/oauth2/code/google
            scope:
              - profile
              - email

openai:
  api:
    key: {KEY}
  model: gpt-4o-mini

open-api:
  assembly:
    key: {KEY}

logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.web.client.RestTemplate: DEBUG
    org.springframework.security.oauth2.client: DEBUG
```

### 4. 애플리케이션 실행

애플리케이션이 실행되면 `https://00-fe.vercel.app`에서 확인할 수 있습니다.

## 📝 API 엔드포인트

Swagger UI를 통해 모든 API 엔드포인트와 명세를 확인할 수 있습니다.
- **Swagger UI**: `https://moragora.site/swagger-ui/index.html#/`

### 주요 엔드포인트:

- **Petition (`/petition`)**:
    - `GET /{id}`: 청원 상세 정보 조회
    - `GET /cardNews`: 청원 목록(카드 뉴스) 조회
    - `POST /likes`: 청원 공감/비공감 처리
    - `POST /comment`: 댓글 작성
    - `GET /comment/{id}`: 댓글 조회
    - `POST /scrap/{id}`: 청원 스크랩
- **User (`/user`)**:
    - `POST /signUp`: 회원가입
    - `GET /me`: 내 정보 조회
    - `GET /scrap`: 스크랩한 청원 조회
    - `DELETE /delete`: 회원 탈퇴
