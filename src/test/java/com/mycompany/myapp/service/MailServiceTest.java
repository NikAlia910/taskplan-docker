package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.User;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Unit tests for {@link MailService}.
 */
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JHipsterProperties.Mail mailProperties;

    @InjectMocks
    private MailService mailService;

    @Mock
    private MimeMessage mimeMessage;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLangKey("en");

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(mailProperties.getFrom()).thenReturn("noreply@localhost");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendEmail_shouldSendEmailSuccessfully() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = false;
        boolean isHtml = false;

        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailFromTemplate_shouldProcessTemplateAndSendEmail() {
        // Given
        String templateName = "mail/activationEmail";
        String titleKey = "email.activation.title";
        String processedTemplate = "<html><body>Activation Email</body></html>";

        when(messageSource.getMessage(eq(titleKey), any(), eq("en"), any())).thenReturn("Activate Account");
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(processedTemplate);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine).process(eq(templateName), any(Context.class));
        verify(messageSource).getMessage(eq(titleKey), any(), eq("en"), any());
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendActivationEmail_shouldSendActivationEmailToUser() {
        // Given
        when(templateEngine.process(eq("mail/activationEmail"), any(Context.class))).thenReturn(
            "<html><body>Activation Email</body></html>"
        );
        when(messageSource.getMessage(eq("email.activation.title"), any(), eq("en"), any())).thenReturn("Activate Account");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendActivationEmail(user);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/activationEmail"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("user")).isEqualTo(user);
        assertThat(context.getVariable("baseUrl")).isEqualTo("http://localhost:8080");

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendCreationEmail_shouldSendCreationEmailToUser() {
        // Given
        when(templateEngine.process(eq("mail/creationEmail"), any(Context.class))).thenReturn("<html><body>Creation Email</body></html>");
        when(messageSource.getMessage(eq("email.activation.title"), any(), eq("en"), any())).thenReturn("Account Created");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendCreationEmail(user);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/creationEmail"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("user")).isEqualTo(user);
        assertThat(context.getVariable("baseUrl")).isEqualTo("http://localhost:8080");

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetMail_shouldSendPasswordResetEmailToUser() {
        // Given
        when(templateEngine.process(eq("mail/passwordResetEmail"), any(Context.class))).thenReturn(
            "<html><body>Password Reset Email</body></html>"
        );
        when(messageSource.getMessage(eq("email.reset.title"), any(), eq("en"), any())).thenReturn("Password Reset");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendPasswordResetMail(user);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/passwordResetEmail"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("user")).isEqualTo(user);
        assertThat(context.getVariable("baseUrl")).isEqualTo("http://localhost:8080");

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_shouldHandleMailException() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        doThrow(new MailSendException("Mail server error")).when(javaMailSender).send(any(MimeMessage.class));

        // When & Then
        // The method should handle the exception gracefully and log it
        mailService.sendEmail(to, subject, content, false, false);

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailFromTemplate_shouldHandleUserWithoutEmail() {
        // Given
        User userWithoutEmail = new User();
        userWithoutEmail.setLogin("testuser");
        userWithoutEmail.setEmail(null);

        // When
        mailService.sendEmailFromTemplate(userWithoutEmail, "test", "test.key");

        // Then
        verifyNoInteractions(templateEngine);
        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendEmailFromTemplate_shouldUseDefaultLangKeyWhenUserLangKeyIsNull() {
        // Given
        user.setLangKey(null);
        when(templateEngine.process(eq("mail/test"), any(Context.class))).thenReturn("<html><body>Test Email</body></html>");
        when(messageSource.getMessage(eq("test.key"), any(), eq("en"), any())).thenReturn("Test Subject");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmailFromTemplate(user, "mail/test", "test.key");

        // Then
        verify(messageSource).getMessage(eq("test.key"), any(), eq("en"), any());
        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
