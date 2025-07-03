package com.mycompany.myapp.service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.mycompany.myapp.service.EmailAlreadyUsedException;
import com.mycompany.myapp.service.InvalidPasswordException;
import com.mycompany.myapp.service.UsernameAlreadyUsedException;
import org.junit.jupiter.api.Test;

class ServiceExceptionTest {

    @Test
    void testEmailAlreadyUsedException() {
        // When
        EmailAlreadyUsedException exception = new EmailAlreadyUsedException();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Email is already in use!");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testEmailAlreadyUsedExceptionThrow() {
        // When/Then
        assertThatExceptionOfType(EmailAlreadyUsedException.class)
            .isThrownBy(() -> {
                throw new EmailAlreadyUsedException();
            })
            .withMessage("Email is already in use!");
    }

    @Test
    void testUsernameAlreadyUsedException() {
        // When
        UsernameAlreadyUsedException exception = new UsernameAlreadyUsedException();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Login name already used!");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUsernameAlreadyUsedExceptionThrow() {
        // When/Then
        assertThatExceptionOfType(UsernameAlreadyUsedException.class)
            .isThrownBy(() -> {
                throw new UsernameAlreadyUsedException();
            })
            .withMessage("Login name already used!");
    }

    @Test
    void testInvalidPasswordException() {
        // When
        InvalidPasswordException exception = new InvalidPasswordException();

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Incorrect password");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testInvalidPasswordExceptionThrow() {
        // When/Then
        assertThatExceptionOfType(InvalidPasswordException.class)
            .isThrownBy(() -> {
                throw new InvalidPasswordException();
            })
            .withMessage("Incorrect password");
    }

    @Test
    void testExceptionInheritance() {
        // Test that all exceptions inherit from RuntimeException
        assertThat(EmailAlreadyUsedException.class).isAssignableTo(RuntimeException.class);
        assertThat(UsernameAlreadyUsedException.class).isAssignableTo(RuntimeException.class);
        assertThat(InvalidPasswordException.class).isAssignableTo(RuntimeException.class);
    }

    @Test
    void testExceptionStackTrace() {
        // Given
        EmailAlreadyUsedException exception = new EmailAlreadyUsedException();

        // When
        StackTraceElement[] stackTrace = exception.getStackTrace();

        // Then
        assertThat(stackTrace).isNotNull();
        assertThat(stackTrace.length).isGreaterThan(0);
    }

    @Test
    void testExceptionSerialization() {
        // Test that exceptions can be used in exception handling
        try {
            throw new EmailAlreadyUsedException();
        } catch (EmailAlreadyUsedException e) {
            assertThat(e.getMessage()).isEqualTo("Email is already in use!");
        }

        try {
            throw new UsernameAlreadyUsedException();
        } catch (UsernameAlreadyUsedException e) {
            assertThat(e.getMessage()).isEqualTo("Login name already used!");
        }

        try {
            throw new InvalidPasswordException();
        } catch (InvalidPasswordException e) {
            assertThat(e.getMessage()).isEqualTo("Incorrect password");
        }
    }

    @Test
    void testExceptionEquality() {
        // Given
        EmailAlreadyUsedException exception1 = new EmailAlreadyUsedException();
        EmailAlreadyUsedException exception2 = new EmailAlreadyUsedException();

        // Then
        assertThat(exception1.getMessage()).isEqualTo(exception2.getMessage());
        assertThat(exception1.getClass()).isEqualTo(exception2.getClass());
    }

    @Test
    void testExceptionToString() {
        // Given
        EmailAlreadyUsedException exception = new EmailAlreadyUsedException();

        // When
        String toString = exception.toString();

        // Then
        assertThat(toString).contains("EmailAlreadyUsedException");
        assertThat(toString).contains("Email is already in use!");
    }
}
