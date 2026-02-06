package com.roadmate.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("RoadMate - Şifre Sıfırlama Kodu");
            helper.setText(buildResetEmailHtml(code), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("E-posta gönderilemedi: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String code) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; background: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb;">
                    <h2 style="text-align: center; color: #1f2937; margin-bottom: 8px;">RoadMate</h2>
                    <p style="text-align: center; color: #6b7280; font-size: 14px; margin-bottom: 24px;">Şifre Sıfırlama</p>
                    <p style="color: #374151; font-size: 15px; line-height: 1.6;">
                        Merhaba,<br><br>
                        Şifrenizi sıfırlamak için aşağıdaki 6 haneli kodu kullanın:
                    </p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #4f46e5; background: #eef2ff; padding: 16px 32px; border-radius: 8px;">
                            %s
                        </span>
                    </div>
                    <p style="color: #6b7280; font-size: 13px; text-align: center;">
                        Bu kod <strong>15 dakika</strong> içinde geçerliliğini yitirecektir.
                    </p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                    <p style="color: #9ca3af; font-size: 12px; text-align: center;">
                        Bu işlemi siz yapmadıysanız bu e-postayı görmezden gelebilirsiniz.
                    </p>
                </div>
                """.formatted(code);
    }
}
