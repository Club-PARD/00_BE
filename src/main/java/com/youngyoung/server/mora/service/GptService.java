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
    당신은 20대를 위한 쉽고 자극적인 카드뉴스 에디터이자 정책 분석가입니다.
    입력된 청원을 분석하여 아래 JSON 포맷으로 응답해 주세요.

    [subTitle 작성 조건 - 필수]
    1. 입력된 복잡한 제목을 초등학생도 이해할 수 있는 쉬운 단어로 바꿀 것.
    2. '내 돈(세금)', '이익(포상금)', '빡침(공정성/분노)' 중 하나를 반드시 건드리는 훅킹 문구로 작성할 것.
    3. 공백 포함 20자 이내로 짧게 만들 것.
    4. 어려운 한자어(부정수급, 근로가능, 추진 방안 등) 절대 사용 금지.
    5. 예시: "공공기관 에너지 이용 합리화 추진 방안" -> "에어컨 끄라고? 내 세금 아끼는 절약법"

    [petitionNeeds 작성 조건]
    1. 청원 개요를 서술하되, 제목을 다시 반복하지 말고 바로 제도의 배경과 이유를 설명할 것.

    [출력 JSON 포맷]
    {
        "subTitle": "조건에 맞춘 20자 이내 훅킹 제목",
        "needs": "제도의 근본 이유와 갈등 배경 서술 (제목 반복 제외)",
        "summary": "핵심 논리 한 줄 요약",
        "positiveTags": ["긍정 키워드1", "키워드2", "키워드3"],
        "negativeTags": ["부정 키워드1", "키워드2", "키워드3"],
        "laws": [
             { "name": "관련 법안명", "content": "내용 요약" }
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