package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Unit tests for {@link MailService}.
 */
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    private static final String[] LANGUAGES = { "en", "fr", "de", "it", "ja", "ko", "pl", "ru", "tr", "zh-cn", "zh-tw", "ar-ly" };

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private JHipsterProperties.Mail mailProperties;

    private MailService mailService;

    @BeforeEach
    void setup() {
        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("test@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService = new MailService(jHipsterProperties, javaMailSender, messageSource, templateEngine);
    }

    @Test
    void testSendEmail() throws MessagingException {
        // Given
        String to = "user@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = false;
        boolean isHtml = true;

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendEmailMultipart() throws MessagingException {
        // Given
        String to = "user@example.com";
        String subject = "Test Subject";
        String content = "Test Content with attachments";
        boolean isMultipart = true;
        boolean isHtml = false;

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendEmailWithException() {
        // Given
        when(javaMailSender.createMimeMessage()).thenThrow(new MailException("SMTP Error") {});

        // When
        mailService.sendEmail("user@example.com", "Test", "Content", false, true);

        // Then - should not throw exception, just log the error
        verify(javaMailSender).createMimeMessage();
    }

    @Test
    void testSendEmailFromTemplate() {
        // Given
        User user = createTestUser();
        String templateName = "mail/testEmail";
        String titleKey = "email.test.title";
        String expectedSubject = "Test Email Subject";
        String expectedContent = "<html><body>Test Email Content</body></html>";

        when(messageSource.getMessage(eq(titleKey), isNull(), any(Locale.class))).thenReturn(expectedSubject);
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(expectedContent);

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine, timeout(5000)).process(eq(templateName), any(Context.class));
        verify(messageSource, timeout(5000)).getMessage(eq(titleKey), isNull(), any(Locale.class));
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendEmailFromTemplateWithNullEmail() {
        // Given
        User user = createTestUser();
        user.setEmail(null);

        // When
        mailService.sendEmailFromTemplate(user, "mail/testEmail", "email.test.title");

        // Then - should not send email if user has no email
        verify(javaMailSender, never()).send(any(MimeMessage.class));
        verify(templateEngine, never()).process(anyString(), any(Context.class));
    }

    @Test
    void testSendActivationEmail() {
        // Given
        User user = createTestUser();
        String expectedSubject = "Account Activation";
        String expectedContent = "<html><body>Activation Email</body></html>";

        when(messageSource.getMessage(eq("email.activation.title"), isNull(), any(Locale.class))).thenReturn(expectedSubject);
        when(templateEngine.process(eq("mail/activationEmail"), any(Context.class))).thenReturn(expectedContent);

        // When
        mailService.sendActivationEmail(user);

        // Then
        verify(templateEngine, timeout(5000)).process(eq("mail/activationEmail"), any(Context.class));
        verify(messageSource, timeout(5000)).getMessage(eq("email.activation.title"), isNull(), any(Locale.class));
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendCreationEmail() {
        // Given
        User user = createTestUser();
        String expectedSubject = "Account Creation";
        String expectedContent = "<html><body>Creation Email</body></html>";

        when(messageSource.getMessage(eq("email.activation.title"), isNull(), any(Locale.class))).thenReturn(expectedSubject);
        when(templateEngine.process(eq("mail/creationEmail"), any(Context.class))).thenReturn(expectedContent);

        // When
        mailService.sendCreationEmail(user);

        // Then
        verify(templateEngine, timeout(5000)).process(eq("mail/creationEmail"), any(Context.class));
        verify(messageSource, timeout(5000)).getMessage(eq("email.activation.title"), isNull(), any(Locale.class));
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetMail() {
        // Given
        User user = createTestUser();
        String expectedSubject = "Password Reset";
        String expectedContent = "<html><body>Password Reset Email</body></html>";

        when(messageSource.getMessage(eq("email.reset.title"), isNull(), any(Locale.class))).thenReturn(expectedSubject);
        when(templateEngine.process(eq("mail/passwordResetEmail"), any(Context.class))).thenReturn(expectedContent);

        // When
        mailService.sendPasswordResetMail(user);

        // Then
        verify(templateEngine, timeout(5000)).process(eq("mail/passwordResetEmail"), any(Context.class));
        verify(messageSource, timeout(5000)).getMessage(eq("email.reset.title"), isNull(), any(Locale.class));
        verify(javaMailSender, timeout(5000)).send(mimeMessage);
    }

    @Test
    void testSendEmailFromTemplateWithDifferentLanguages() {
        // Test email sending with different language preferences
        for (String language : LANGUAGES) {
            // Given
            User user = createTestUser();
            user.setLangKey(language);
            String templateName = "mail/testEmail";
            String titleKey = "email.test.title";

            when(messageSource.getMessage(eq(titleKey), isNull(), eq(Locale.forLanguageTag(language)))).thenReturn(
                "Test Subject in " + language
            );
            when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn("Test Content in " + language);

            // When
            mailService.sendEmailFromTemplate(user, templateName, titleKey);

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine, timeout(5000)).process(eq(templateName), contextCaptor.capture());

            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getLocale()).isEqualTo(Locale.forLanguageTag(language));
        }
    }

    @Test
    void testEmailTemplateContext() {
        // Given
        User user = createTestUser();
        String templateName = "mail/testEmail";
        String titleKey = "email.test.title";

        when(messageSource.getMessage(anyString(), isNull(), any(Locale.class))).thenReturn("Test Subject");
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn("Test Content");

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine, timeout(5000)).process(eq(templateName), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("user")).isEqualTo(user);
        assertThat(context.getVariable("baseUrl")).isEqualTo("http://localhost:8080");
    }

    @Test
    void testSendEmailWithMessagingException() throws MessagingException {
        // Given
        doThrow(new MessagingException("MIME Error")).when(mimeMessage).setContent(anyString(), anyString());

        // When
        mailService.sendEmail("user@example.com", "Test", "Content", false, true);

        // Then - should handle exception gracefully
        verify(javaMailSender).createMimeMessage();
    }

    private User createTestUser() {
        User user = new User();
        user.setLogin("testuser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setLangKey("en");
        user.setActivated(true);
        return user;
    }
}
