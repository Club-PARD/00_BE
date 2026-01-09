package com.youngyoung.server.mora.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    // ★ 비동기 처리: 배치가 이메일 발송을 기다리지 않고 다음 로직으로 넘어감
    @Async
    public void sendUpdateNotification(String toEmail, String userName, String petitionTitle, String result, Long petitionId) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Mora] 📢 청원 처리 결과가 업데이트 되었습니다!");

            // HTML 형식의 이메일 본문
            String htmlContent = String.format("""
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #333;">안녕하세요, %s님!</h2>
                    <p>회원님이 관심 등록(스크랩)하신 청원의 <strong>처리 결과</strong>가 업데이트 되었습니다.</p>
                    <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                    <h3 style="color: #0066ff;">%s</h3>
                    <p style="font-size: 16px;"><strong>[의결 결과]</strong> <span style="color: #ff3b30;">%s</span></p>
                    <br>
                    <p>자세한 내용은 아래 링크에서 확인하실 수 있습니다.</p>
                    <a href="https://00-fe.vercel.app/petition/%d" 
                       style="display: inline-block; padding: 10px 20px; background-color: #0066ff; color: white; text-decoration: none; border-radius: 5px;">
                       청원 보러가기
                    </a>
                    <br><br>
                    <p style="color: #999; font-size: 12px;">본 메일은 발신 전용입니다.</p>
                </div>
                """, userName, petitionTitle, result, petitionId); // TODO: 도메인 수정 필요

            helper.setText(htmlContent, true); // true = HTML 모드

            javaMailSender.send(mimeMessage);
            log.info("이메일 발송 성공: {} -> {}", toEmail, petitionTitle);

        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
        }
    }
}