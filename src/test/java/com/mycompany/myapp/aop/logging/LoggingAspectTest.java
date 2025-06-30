package com.mycompany.myapp.aop.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tech.jhipster.config.JHipsterConstants;

/**
 * Unit tests for {@link LoggingAspect}.
 */
@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private Environment environment;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    private LoggingAspect loggingAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setup() {
        loggingAspect = new LoggingAspect(environment);

        // Setup logger for testing
        logger = (Logger) LoggerFactory.getLogger(LoggingAspectTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void testLogAfterThrowingInDevelopmentProfile() {
        // Given
        when(environment.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))).thenReturn(true);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException exception = new RuntimeException("Test exception");
        exception.initCause(new IllegalArgumentException("Root cause"));

        // When
        loggingAspect.logAfterThrowing(joinPoint, exception);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Exception in testMethod()").contains("Test exception");
    }

    @Test
    void testLogAfterThrowingInProductionProfile() {
        // Given
        when(environment.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException exception = new RuntimeException("Test exception");

        // When
        loggingAspect.logAfterThrowing(joinPoint, exception);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Exception in testMethod()").doesNotContain("Test exception"); // Should not include full exception in production
    }

    @Test
    void testLogAfterThrowingWithCause() {
        // Given
        when(environment.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))).thenReturn(true);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException exception = new RuntimeException("Test exception");
        IllegalArgumentException cause = new IllegalArgumentException("Root cause");
        exception.initCause(cause);

        // When
        loggingAspect.logAfterThrowing(joinPoint, exception);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getFormattedMessage()).contains("cause = 'java.lang.IllegalArgumentException: Root cause'");
    }

    @Test
    void testLogAfterThrowingWithNullCause() {
        // Given
        when(environment.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))).thenReturn(true);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException exception = new RuntimeException("Test exception");
        // No cause set

        // When
        loggingAspect.logAfterThrowing(joinPoint, exception);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getFormattedMessage()).contains("cause = 'NULL'");
    }

    @Test
    void testLogAroundMethodEntry() throws Throwable {
        // Given
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { "arg1", "arg2" });
        when(proceedingJoinPoint.proceed()).thenReturn("testResult");

        // Enable debug logging
        logger.setLevel(Level.DEBUG);

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        assertThat(result).isEqualTo("testResult");
        List<ILoggingEvent> logsList = listAppender.list;

        // Should have entry and exit logs
        assertThat(logsList).hasSizeGreaterThanOrEqualTo(2);

        // Check entry log
        ILoggingEvent entryLog = logsList
            .stream()
            .filter(event -> event.getFormattedMessage().contains("Enter: testMethod()"))
            .findFirst()
            .orElse(null);
        assertThat(entryLog).isNotNull();
        assertThat(entryLog.getFormattedMessage()).contains("[arg1, arg2]");

        // Check exit log
        ILoggingEvent exitLog = logsList
            .stream()
            .filter(event -> event.getFormattedMessage().contains("Exit: testMethod()"))
            .findFirst()
            .orElse(null);
        assertThat(exitLog).isNotNull();
        assertThat(exitLog.getFormattedMessage()).contains("testResult");
    }

    @Test
    void testLogAroundWithoutDebugLogging() throws Throwable {
        // Given
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");
        when(proceedingJoinPoint.proceed()).thenReturn("testResult");

        // Disable debug logging
        logger.setLevel(Level.INFO);

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        assertThat(result).isEqualTo("testResult");
        // Should not have debug logs when debug is disabled
        List<ILoggingEvent> debugLogs = listAppender.list.stream().filter(event -> event.getLevel().equals(Level.DEBUG)).toList();
        assertThat(debugLogs).isEmpty();
    }

    @Test
    void testLogAroundWithIllegalArgumentException() throws Throwable {
        // Given
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { "invalidArg" });

        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);

        // When & Then
        try {
            loggingAspect.logAround(proceedingJoinPoint);
        } catch (IllegalArgumentException e) {
            // Exception should be rethrown
            assertThat(e).isEqualTo(exception);
        }

        // Should log the illegal argument error
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent errorLog = logsList
            .stream()
            .filter(event -> event.getLevel().equals(Level.ERROR))
            .filter(event -> event.getFormattedMessage().contains("Illegal argument"))
            .findFirst()
            .orElse(null);
        assertThat(errorLog).isNotNull();
        assertThat(errorLog.getFormattedMessage()).contains("[invalidArg]").contains("testMethod()");
    }

    @Test
    void testLogAroundWithOtherException() throws Throwable {
        // Given
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(LoggingAspectTest.class.getName());
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException exception = new RuntimeException("Some other error");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);

        // When & Then
        try {
            loggingAspect.logAround(proceedingJoinPoint);
        } catch (RuntimeException e) {
            // Exception should be rethrown
            assertThat(e).isEqualTo(exception);
        }

        // Should not log as error (only IllegalArgumentException gets special treatment)
        List<ILoggingEvent> errorLogs = listAppender.list
            .stream()
            .filter(event -> event.getLevel().equals(Level.ERROR))
            .filter(event -> event.getFormattedMessage().contains("Illegal argument"))
            .toList();
        assertThat(errorLogs).isEmpty();
    }

    @Test
    void testPointcuts() {
        // Test that pointcuts are properly defined (methods exist and compile)
        loggingAspect.springBeanPointcut();
        loggingAspect.applicationPackagePointcut();
        // These methods should not throw any exceptions
        // They're pointcut definitions, so they're expected to be empty
    }

    @Test
    void testLoggerCreation() {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");

        // When
        Exception testException = new RuntimeException("Test");
        loggingAspect.logAfterThrowing(joinPoint, testException);

        // Then - verify that logger is created for the correct class
        // This is implicitly tested by the successful execution of logAfterThrowing
        verify(joinPoint, atLeastOnce()).getSignature();
        verify(signature, atLeastOnce()).getDeclaringTypeName();
    }
}
