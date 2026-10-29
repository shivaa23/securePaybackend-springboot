package com.securepay.modules.message.service;

import com.securepay.common.audit.AuditLogService;
import com.securepay.modules.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * OTP / notification delivery. Replaces the legacy SMS gateway call ({@code Sms::send}) with
 * <b>email</b>: every message the legacy code would have texted is mailed instead.
 *
 * <p>Recipient: {@code securepay.otp.mail-to} when set (a fixed inbox, used while SMS is not
 * available), otherwise the user's own email. The intended mobile number and template id are
 * included in the body so the message is still traceable. An audit-log row (action SMS) is written
 * like the original.</p>
 *
 * <p>SMTP credentials come from {@code spring.mail.*} (env MAIL_HOST / MAIL_PORT / MAIL_USERNAME /
 * MAIL_PASSWORD) — nothing is hardcoded. If delivery fails and {@code securepay.otp.log-fallback}
 * is true (local only) the message is logged so development can continue.</p>
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final AuditLogService auditLogService;
    private final JavaMailSender mailSender;
    private final String mailTo;
    private final String mailFrom;
    private final boolean logFallback;

    public SmsService(AuditLogService auditLogService,
                      JavaMailSender mailSender,
                      @Value("${securepay.otp.mail-to:}") String mailTo,
                      @Value("${securepay.otp.mail-from:}") String mailFrom,
                      @Value("${securepay.otp.log-fallback:false}") boolean logFallback) {
        this.auditLogService = auditLogService;
        this.mailSender = mailSender;
        this.mailTo = mailTo;
        this.mailFrom = mailFrom;
        this.logFallback = logFallback;
    }

    /**
     * @param number     the mobile number the SMS was meant for (shown in the mail body)
     * @param message    the rendered message text (e.g. the OTP text)
     * @param templateId DLT template id (kept for traceability)
     * @return "MAIL_SENT", "MAIL_FAILED" or "NO_RECIPIENT"
     */
    public String send(String number, String message, String templateId, User user) {
        String to = !mailTo.isBlank() ? mailTo : (user == null ? null : user.getEmail());
        String result;
        if (to == null || to.isBlank()) {
            log.warn("No email recipient for notification (user={}); not sent", user == null ? null : user.getId());
            result = "NO_RECIPIENT";
        } else {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                if (!mailFrom.isBlank()) {
                    mail.setFrom(mailFrom);
                }
                mail.setTo(to);
                mail.setSubject("SecurePay verification message");
                mail.setText(message + "\n\n---\nIntended for mobile: " + number
                        + (templateId == null ? "" : "\nTemplate: " + templateId));
                mailSender.send(mail);
                result = "MAIL_SENT";
            } catch (Exception e) {
                log.warn("Email delivery to {} failed: {}", to, e.getMessage());
                if (logFallback) {
                    log.warn("[OTP-MAIL-FALLBACK to={} mobile={}] {}", to, number, message);
                }
                result = "MAIL_FAILED";
            }
        }
        if (user != null) {
            auditLogService.record(user.getId(), user.getRole(), "SMS", "sms",
                    "\"" + user.getMobile() + "\"", "\"" + result + "\"", null, "internal", "Success");
        }
        return result;
    }
}
