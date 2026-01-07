package com.youngyoung.server.mora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youngyoung.server.mora.dto.AiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GptService {

    @org.springframework.beans.factory.annotation.Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public AiResponseDto analyzePetition(String fullText) {
        String url = "https://api.openai.com/v1/chat/completions";

        // 프롬프트 엔지니어링
        String systemInstruction = """
    당신은 청년들의 주체적인 정치 참여를 돕는 서비스 '모라(MORA)'의 수석 에디터입니다.
    당신의 임무는 감정적이고 편향된 언어로 작성된 '국회 청원 원문'을 분석하여, 
    20대 청년들이 자신의 삶에 미치는 영향을 객관적으로 판단할 수 있는 '가치 중립적인 콘텐츠 데이터'로 정제하는 것입니다.

    [콘텐츠 작성 3대 원칙]
    1. 철저한 가치 중립: 특정 진영의 감정적 표현을 배제하고, 행정/법률 용어로 드라이하게 정제하십시오. 팩트와 논리 구조는 유지해야 합니다.
    2. 20대 맞춤형 가독성: 어려운 법률 용어는 대학생 수준에서 이해하기 쉽게 풀어쓰십시오. 문체는 정중하고 명확해야 합니다.
    3. 표기 제약 (매우 중요): 
       - 결과물에는 절대 영어를 사용하지 마십시오 (JSON 키 제외). 
       - 볼드체(**), 마크다운 서식을 본문 내용에 사용하지 마십시오.
       - 오직 한글 텍스트로만 구성하십시오.

    [출력 포맷]
    반드시 아래의 JSON 포맷으로만 응답해야 합니다. 다른 사족(인사말 등)을 붙이지 마십시오.
    {
        "needs": "청원 개요 (제목: ~은 왜 나라의 허락/규칙이 필요한가요?): 제도의 근본 이유와 갈등 배경을 서술 (2~3문장)",
        "summary": "청원 요약: 원본의 핵심 논리를 단호하고 명쾌한 한 줄 문장으로 요약",
        "positiveTags": ["20대 입장에서의 긍정적 기대효과 단문 1", "긍정 단문 2", "긍정 단문 3"],
        "negativeTags": ["20대 입장에서의 부정적 우려사항 단문 1", "부정 단문 2", "부정 단문 3"],
        "laws": [
            { "name": "관련 법안 또는 조문 명칭 (없으면 생략)", "content": "해당 조문의 핵심 내용 요약 (원문 인용 지양, 쉽게 풀어서)" }
        ]
    }
    """;

        String userPrompt = "[분석할 청원 원문]\n" + fullText;

// 요청 바디 생성 (gpt-4o-mini 사용)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemInstruction),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("response_format", Map.of("type", "json_object")); // JSON 모드 활성화
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            // JSON String -> DTO 변환
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, AiResponseDto.class);

        } catch (Exception e) {
            log.error("GPT 호출 중 에러 발생", e);
            return new AiResponseDto(); // 빈 객체 반환 혹은 예외 처리
        }
    }
}