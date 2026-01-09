# Mora (모아) - 입법 청원 정보 제공 서비스

## 📖 프로젝트 소개

**Mora**는 복잡하고 어려운 입법 예고들을 누구나 쉽게 이해하고 참여할 수 있도록 돕는 웹 서비스입니다. AI를 활용하여 법률안을 요약하고, 관련 뉴스와 진행 상황을 시각적으로 제공하여 시민들의 정치 참여를 활성화하는 것을 목표로 합니다.

## ✨ 주요 기능

- **🔍 청원 검색 및 필터링**: 다양한 조건(최신순, 동의순, 분야별)으로 원하는 청원을 쉽게 찾아볼 수 있습니다.
- **🤖 AI 요약**: OpenAI(GPT)를 활용하여 복잡한 법률안 원문을 이해하기 쉬운 내용으로 요약해 제공합니다.
- **📰 관련 뉴스 제공**: 청원과 관련된 최신 뉴스를 함께 제공하여 폭넓은 이해를 돕습니다.
- **🏛️ 관련 법률안 정보**: 해당 청원과 관련된 다른 법률안 정보를 연결하여 제공합니다.
- **💬 커뮤니티**:
    - **댓글**: 청원에 대한 의견을 자유롭게 나누고 토론할 수 있습니다.
    - **공감/비공감**: 청원에 대한 자신의 입장을 표현할 수 있습니다.
    - **스크랩**: 관심 있는 청원을 자신의 프로필에 저장하고 언제든지 다시 볼 수 있습니다.
- **👤 사용자 관리**:
    - **회원가입 및 로그인**: 소셜 로그인(Google, Naver)을 지원하여 간편하게 이용할 수 있습니다.
    - **마이페이지**: 스크랩한 청원 목록, 내 정보 수정 등의 기능을 제공합니다.

## 🛠️ 기술 스택

- **Backend**:
    - Java 17
    - Spring Boot 3.4.1
    - Spring Security / OAuth2 / JWT
    - Spring Data JPA
- **Database**:
    - MySQL
    - Redis
- **API & Communication**:
    - Spring Cloud OpenFeign (for OpenAI API)
    - RESTful API
- **Web Scraping**:
    - Selenium
- **API Documentation**:
    - Swagger UI
- **Build Tool**:
    - Gradle

## 🚀 시작하기

### 1. 전제 조건

- Java 17
- Gradle 8.x 이상
- MySQL

### 2. 프로젝트 클론 및 설정

```bash
# 프로젝트 클론
git clone https://github.com/Club-PARD/00_BE.git
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
