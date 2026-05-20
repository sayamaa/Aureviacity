package com.aurevia.cityexplorer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.aurevia.cityexplorer.model.ContactMessage;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;
    private final String recipientEmail;
    private final String senderEmail;

    public ContactEmailService(JavaMailSender mailSender,
                               @Value("${aurevia.contact.recipient-email}") String recipientEmail,
                               @Value("${spring.mail.username}") String senderEmail) {
        this.mailSender = mailSender;
        this.recipientEmail = recipientEmail;
        this.senderEmail = senderEmail;
    }

    public void sendContactMessage(ContactMessage contactMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setReplyTo(contactMessage.getEmail());
            helper.setSubject("Aurevia Contact: " + contactMessage.getSubject());
            helper.setText(buildEmailBody(contactMessage), false);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Unable to send contact email", ex);
        }
    }

    private String buildEmailBody(ContactMessage contactMessage) {
        return """
                New Aurevia contact message

                Name: %s
                Email: %s
                City/Page: %s
                Subject: %s

                Message:
                %s
                """.formatted(
                contactMessage.getFullName(),
                contactMessage.getEmail(),
                contactMessage.getCity() == null ? "Not provided" : contactMessage.getCity(),
                contactMessage.getSubject(),
                contactMessage.getMessage());
    }
}
