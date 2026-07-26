package com.knowledge.base.userauth.service.impl;

import com.knowledge.base.userauth.service.EmailService;
import com.knowledge.base.userauth.service.SecurityConfigService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email sending service implementation
 *
 * <p>Sends HTML-formatted account activation emails and password reset verification code
 * emails; the system name shown in the email is loaded dynamically from the
 * {@code system.name} configuration.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private SecurityConfigService securityConfigService;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.frontend-url:http://localhost:3002}")
    private String frontendUrl;

    /**
     * Get the system name (read from the security config cache, defaults to "Knowledge Base System")
     */
    private String getSystemName() {
        String value = securityConfigService.getConfig("system.name");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return "Knowledge Base System";
    }

    @Override
    public void sendActivationEmail(String to, String username, String token) {
        String activationUrl = frontendUrl + "/activate?token=" + token;
        String systemName = getSystemName();

        String subject = "[" + systemName + "] Account Activation";
        String htmlContent = buildActivationEmailHtml(username, activationUrl, systemName);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendResetCodeEmail(String to, String code) {
        String systemName = getSystemName();
        String subject = "[" + systemName + "] Password Reset Code";
        String htmlContent = buildResetCodeEmailHtml(code, systemName);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send an HTML-formatted email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("Failed to send email, please try again later", e);
        }
    }

    /**
     * Build the HTML content of the activation email
     */
    private String buildActivationEmailHtml(String username, String activationUrl, String systemName) {
        return """
                <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                    <div style="background:linear-gradient(135deg,#1890ff,#722ed1);padding:20px;border-radius:8px 8px 0 0;">
                        <h2 style="color:#fff;margin:0;text-align:center;">%s</h2>
                    </div>
                    <div style="border:1px solid #e8e8e8;border-top:none;padding:30px;border-radius:0 0 8px 8px;">
                        <p style="font-size:16px;color:#333;">Hello, <strong>%s</strong>:</p>
                        <p style="font-size:14px;color:#666;line-height:1.8;">
                            Thank you for signing up for %s! Please click the button below to activate your account:
                        </p>
                        <div style="text-align:center;margin:30px 0;">
                            <a href="%s" target="_blank"
                               style="display:inline-block;padding:12px 40px;
                                      background:linear-gradient(135deg,#1890ff,#722ed1);
                                      color:#fff;text-decoration:none;border-radius:6px;
                                      font-size:16px;font-weight:600;">
                                Activate Account
                            </a>
                        </div>
                        <p style="font-size:12px;color:#999;">
                            If the button doesn't work, copy and paste the following link into your browser's address bar:<br/>
                            <span style="color:#1890ff;">%s</span>
                        </p>
                        <p style="font-size:12px;color:#999;margin-top:20px;">
                            This link is valid for 24 hours, so please activate your account soon.<br/>
                            If you did not sign up for this account, please ignore this email.
                        </p>
                    </div>
                </div>
                """.formatted(systemName, username, systemName, activationUrl, activationUrl);
    }

    /**
     * Build the HTML content of the password reset code email
     */
    private String buildResetCodeEmailHtml(String code, String systemName) {
        return """
                <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                    <div style="background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:20px;border-radius:8px 8px 0 0;">
                        <h2 style="color:#fff;margin:0;text-align:center;">%s - Password Reset</h2>
                    </div>
                    <div style="border:1px solid #e8e8e8;border-top:none;padding:30px;border-radius:0 0 8px 8px;">
                        <p style="font-size:16px;color:#333;">You requested a password reset. Your verification code is:</p>
                        <div style="text-align:center;margin:30px 0;">
                            <span style="display:inline-block;padding:16px 48px;
                                         background:#f0f5ff;border:2px dashed #2563eb;
                                         color:#2563eb;font-size:32px;font-weight:700;
                                         letter-spacing:8px;border-radius:8px;">
                                %s
                            </span>
                        </div>
                        <p style="font-size:12px;color:#999;">
                            This code is valid for 10 minutes, so please complete verification soon.<br/>
                            If you did not request a password reset, please ignore this email.
                        </p>
                    </div>
                </div>
                """.formatted(systemName, code);
    }
}
