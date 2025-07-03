package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailServiceTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_LOGIN = "testuser";
    private static final String BASE_URL = "http://127.0.0.1:8080";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_CONTENT = "Test Content";

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JHipsterProperties.Mail mailProperties;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getBaseUrl()).thenReturn(BASE_URL);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService = new MailService(jHipsterProperties, javaMailSender, messageSource, templateEngine);
    }

    @Test
    void testSendEmailWithDefaultSettings() throws MessagingException {
        // Given
        String to = TEST_USER_EMAIL;
        String subject = TEST_SUBJECT;
        String content = TEST_CONTENT;
        boolean isMultipart = false;
        boolean isHtml = false;

        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void testSendEmailWithMultipartAndHtml() throws MessagingException {
        // Given
        String to = TEST_USER_EMAIL;
        String subject = TEST_SUBJECT;
        String content = TEST_CONTENT;
        boolean isMultipart = true;
        boolean isHtml = true;

        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void testSendEmailFromTemplate() {
        // Given
        User user = createTestUser();
        String templateName = "mail/activationEmail";
        String titleKey = "email.activation.title";
        String expectedTitle = "Activation Email";
        String expectedContent = "<html><body>Activation email content</body></html>";

        when(messageSource.getMessage(titleKey, null, Locale.forLanguageTag(user.getLangKey()))).thenReturn(expectedTitle);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(expectedContent);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(messageSource).getMessage(titleKey, null, Locale.forLanguageTag(user.getLangKey()));
        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void testSendActivationEmail() {
        // Given
        User user = createTestUser();
        String expectedContent = "<html><body>Activation email</body></html>";

        when(messageSource.getMessage("email.activation.title", null, Locale.forLanguageTag(user.getLangKey()))).thenReturn(
            "Activate your account"
        );
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(expectedContent);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendActivationEmail(user);

        // Then
        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetMail() {
        // Given
        User user = createTestUser();
        String expectedContent = "<html><body>Password reset email</body></html>";

        when(messageSource.getMessage("email.reset.title", null, Locale.forLanguageTag(user.getLangKey()))).thenReturn(
            "Password reset requested"
        );
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(expectedContent);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendPasswordResetMail(user);

        // Then
        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void testSendEmailWithMailException() {
        // Given
        String to = TEST_USER_EMAIL;
        String subject = TEST_SUBJECT;
        String content = TEST_CONTENT;

        doThrow(new MailException("Mail server error") {}).when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmail(to, subject, content, false, false);

        // Then
        verify(javaMailSender).send(mimeMessage);
        // Should log the exception but not rethrow it
    }

    @Test
    void testSendEmailFromTemplateWithException() {
        // Given
        User user = createTestUser();
        String templateName = "mail/activationEmail";
        String titleKey = "email.activation.title";

        when(messageSource.getMessage(titleKey, null, Locale.forLanguageTag(user.getLangKey()))).thenReturn("Activation Email");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body>Content</body></html>");
        doThrow(new MailException("Mail server error") {}).when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(javaMailSender).send(mimeMessage);
        // Should log the exception but not rethrow it
    }

    @Test
    void testConstructor() {
        // When
        MailService service = new MailService(jHipsterProperties, javaMailSender, messageSource, templateEngine);

        // Then
        assertThat(service).isNotNull();
    }

    @Test
    void testContextSetupInTemplate() {
        // Given
        User user = createTestUser();
        String templateName = "mail/testEmail";
        String titleKey = "email.test.title";

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        when(messageSource.getMessage(titleKey, null, Locale.forLanguageTag(user.getLangKey()))).thenReturn("Test Email");
        when(templateEngine.process(anyString(), contextCaptor.capture())).thenReturn("Email content");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("user")).isEqualTo(user);
        assertThat(capturedContext.getVariable("baseUrl")).isEqualTo(BASE_URL);
    }

    @Test
    void testSendEmailWithNullEmail() {
        // Given
        User user = createTestUser();
        user.setEmail(null);
        String templateName = "mail/testEmail";
        String titleKey = "email.test.title";

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(javaMailSender, never()).send(any(MimeMessage.class));
        verify(templateEngine, never()).process(anyString(), any(Context.class));
    }

    private User createTestUser() {
        User user = new User();
        user.setLogin(TEST_USER_LOGIN);
        user.setEmail(TEST_USER_EMAIL);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLangKey("en");
        user.setActivationKey("activation123");
        user.setResetKey("reset123");
        return user;
    }
}
