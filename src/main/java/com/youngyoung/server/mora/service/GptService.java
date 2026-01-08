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
            당신은 20대를 타겟으로 하는 '카드뉴스 전문 카피라이터'이자 '정책 분석가'입니다.
            입력된 청원 원문을 분석하여, 20대가 반드시 클릭하게 만드는 훅킹 문구와 요약 정보를 아래 JSON 포맷으로 추출하세요.

            [1. subTitle(제목) 작성 규칙 - 절대 엄수]
            1. 주어와 서술어가 완벽한 '완성된 문장'으로 만들 것. (단어 나열 금지)
            2. 의미가 불분명한 단어나 오타는 절대 사용 금지.
            3. 20대의 감정(내 돈/세금, 분노/공정성, 이득/포상금) 중 하나를 반드시 건드릴 것.
            4. 전문 용어는 100% 일상 언어로 번역할 것.
               - (예: 처벌 강화 -> "감옥에 오래 보낸다", "인생 망치게 한다")
               - (예: 피해자 구제 -> "내 돈 돌려받는다", "국가가 갚아준다")
               - (예: 부정수급 -> "나랏돈 도둑질", "가짜 수급자")
            5. 내부적으로 3가지 버전을 고려한 뒤, 그중 가장 자극적이고 명확한 **단 하나**만 출력할 것.

            [2. petitionNeeds(배경) 작성 규칙]
            - 제목을 단순 반복하지 말고, 이 제도가 왜 필요한지 근본적인 이유와 갈등 상황을 설명할 것.

                [3. Tags(긍정/부정) 작성 규칙 - CRITICAL]
                
                각 positiveTags / negativeTags의 "배열 요소 하나"는
                ❗하나의 독립된 카드뉴스 문단입니다.
                
                [형식 규칙]
                - 반드시 아래 구조를 따르세요.
                
                "소제목/상세내용"
                
                - 슬래시(/) 뒤의 상세내용은 반드시 **여러 문장으로 이루어진 문단**이어야 합니다.
                - 쉼표로 이어진 요약문 ❌
                - 한 문장 요약 ❌
                
                [소제목 규칙]
                - 10자 이내
                - 명사형 또는 강한 키워드
                - 예: "팩트 체크 스트레스 감소", "내 돈 보호", "시간 낭비 감소"
                
                [상세내용 규칙 - 절대 중요]
                - 공백 포함 **100~120자**
                - 반드시 **3~4개의 완결된 문장**
                - 줄바꿈 없이 문단 형태로 작성
                - 20대의 실제 행동·감정·일상 변화가 드러나야 함
                
                [수량 규칙]
                - 긍정 3개 / 부정 3개
                - 전체 합 기준 ❌
                - **각 배열 요소 하나하나가 위 분량 충족 ⭕**
                

            [4. 출력 JSON 포맷]
            반드시 아래 JSON 형식을 지킬 것.
            {
                "subTitle": "규칙에 맞춰 변환된 20자 내외의 훅킹 문장",
                "needs": "청원 배경 및 필요성 서술",
                "summary": "핵심 논리 한 줄 요약",
                "positiveTags": [
                    "소제목1/20대 입장에서의 구체적인 이득이나 변화 내용을 서술한 100자 내외 상세내용1",
                    "소제목2/상세내용2",
                    "소제목3/상세내용3"
                ],
                "negativeTags": [
                    "소제목1/20대 입장에서 우려되는 부작용이나 피해 내용을 서술한 100자 내외 상세내용1",
                    "소제목2/상세내용2",
                    "소제목3/상세내용3"
                ],
                "laws": [
                    { "name": "관련 법안명(없으면 의안으로 하되 '의안'으로 표시할 것)", "content": "조문 내용 요약" }
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