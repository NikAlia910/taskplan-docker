package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.web.rest.errors.EmailAlreadyUsedException;
import com.mycompany.myapp.web.rest.errors.InvalidPasswordException;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountResourceTest {

    private static final String TEST_LOGIN = "testlogin";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ACTIVATION_KEY = "activationKey123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    private AccountResource accountResource;

    @BeforeEach
    void setUp() {
        accountResource = new AccountResource(userRepository, userService, mailService);
    }

    @Test
    void testRegisterAccountWithValidUser() {
        // Given
        ManagedUserVM managedUserVM = createValidManagedUserVM();
        User registeredUser = createTestUser();
        when(userService.registerUser(any(ManagedUserVM.class), anyString())).thenReturn(registeredUser);
        when(userService.activateRegistration(ACTIVATION_KEY)).thenReturn(Optional.of(registeredUser));

        // When
        accountResource.registerAccount(managedUserVM);

        // Then
        verify(userService).registerUser(managedUserVM, TEST_PASSWORD);
        verify(userService).activateRegistration(ACTIVATION_KEY);
    }

    @Test
    void testRegisterAccountWithInvalidPassword() {
        // Given
        ManagedUserVM managedUserVM = createValidManagedUserVM();
        managedUserVM.setPassword("123"); // Too short

        // When & Then
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() -> accountResource.registerAccount(managedUserVM));

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testRegisterAccountWithNullPassword() {
        // Given
        ManagedUserVM managedUserVM = createValidManagedUserVM();
        managedUserVM.setPassword(null);

        // When & Then
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() -> accountResource.registerAccount(managedUserVM));

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testRegisterAccountWithEmptyPassword() {
        // Given
        ManagedUserVM managedUserVM = createValidManagedUserVM();
        managedUserVM.setPassword("");

        // When & Then
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() -> accountResource.registerAccount(managedUserVM));

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testActivateAccountWithValidKey() {
        // Given
        User user = createTestUser();
        when(userService.activateRegistration(ACTIVATION_KEY)).thenReturn(Optional.of(user));

        // When
        accountResource.activateAccount(ACTIVATION_KEY);

        // Then
        verify(userService).activateRegistration(ACTIVATION_KEY);
    }

    @Test
    void testActivateAccountWithInvalidKey() {
        // Given
        String invalidKey = "invalidKey";
        when(userService.activateRegistration(invalidKey)).thenReturn(Optional.empty());

        // When & Then
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> accountResource.activateAccount(invalidKey))
            .withMessage("No user was found for this activation key");
    }

    @Test
    void testGetAccountWithValidUser() {
        // Given
        User user = createTestUser();
        AdminUserDTO expectedDto = new AdminUserDTO(user);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));

            // When
            AdminUserDTO result = accountResource.getAccount();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLogin()).isEqualTo(user.getLogin());
            assertThat(result.getEmail()).isEqualTo(user.getEmail());
        }
    }

    @Test
    void testGetAccountWithNoUser() {
        // Given
        when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

        // When & Then
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> accountResource.getAccount())
            .withMessage("User could not be found");
    }

    @Test
    void testSaveAccountWithValidData() {
        // Given
        AdminUserDTO userDTO = createAdminUserDTO();
        User existingUser = createTestUser();

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(TEST_LOGIN));
            when(userRepository.findOneByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
            when(userRepository.findOneByLogin(TEST_LOGIN)).thenReturn(Optional.of(existingUser));
            doNothing().when(userService).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());

            // When
            accountResource.saveAccount(userDTO);

            // Then
            verify(userService).updateUser(
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                userDTO.getLangKey(),
                userDTO.getImageUrl()
            );
        }
    }

    @Test
    void testSaveAccountWithExistingEmail() {
        // Given
        AdminUserDTO userDTO = createAdminUserDTO();
        User existingUserWithEmail = createTestUser();
        existingUserWithEmail.setLogin("anotheruser"); // Different user with same email

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(TEST_LOGIN));
            when(userRepository.findOneByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(existingUserWithEmail));

            // When & Then
            assertThatExceptionOfType(EmailAlreadyUsedException.class).isThrownBy(() -> accountResource.saveAccount(userDTO));

            verify(userService, never()).updateUser(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void testSaveAccountWithNoCurrentUser() {
        // Given
        AdminUserDTO userDTO = createAdminUserDTO();

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            // When & Then
            assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> accountResource.saveAccount(userDTO))
                .withMessage("Current user login not found");
        }
    }

    @Test
    void testChangePasswordWithValidData() {
        // Given
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("oldpassword");
        passwordChangeDTO.setNewPassword("newpassword123");

        doNothing().when(userService).changePassword(anyString(), anyString());

        // When
        accountResource.changePassword(passwordChangeDTO);

        // Then
        verify(userService).changePassword("oldpassword", "newpassword123");
    }

    @Test
    void testChangePasswordWithInvalidNewPassword() {
        // Given
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword("oldpassword");
        passwordChangeDTO.setNewPassword("123"); // Too short

        // When & Then
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() -> accountResource.changePassword(passwordChangeDTO));

        verify(userService, never()).changePassword(anyString(), anyString());
    }

    @Test
    void testRequestPasswordResetWithExistingUser() {
        // Given
        User user = createTestUser();
        when(userService.requestPasswordReset(TEST_EMAIL)).thenReturn(Optional.of(user));
        doNothing().when(mailService).sendPasswordResetMail(user);

        // When
        accountResource.requestPasswordReset(TEST_EMAIL);

        // Then
        verify(userService).requestPasswordReset(TEST_EMAIL);
        verify(mailService).sendPasswordResetMail(user);
    }

    @Test
    void testRequestPasswordResetWithNonExistingUser() {
        // Given
        when(userService.requestPasswordReset(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        accountResource.requestPasswordReset(TEST_EMAIL);

        // Then
        verify(userService).requestPasswordReset(TEST_EMAIL);
        verify(mailService, never()).sendPasswordResetMail(any());
    }

    @Test
    void testFinishPasswordResetWithValidData() {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("resetKey");
        keyAndPassword.setNewPassword("newpassword123");

        User user = createTestUser();
        when(userService.completePasswordReset("newpassword123", "resetKey")).thenReturn(Optional.of(user));

        // When
        accountResource.finishPasswordReset(keyAndPassword);

        // Then
        verify(userService).completePasswordReset("newpassword123", "resetKey");
    }

    @Test
    void testFinishPasswordResetWithInvalidPassword() {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("resetKey");
        keyAndPassword.setNewPassword("123"); // Too short

        // When & Then
        assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() -> accountResource.finishPasswordReset(keyAndPassword));

        verify(userService, never()).completePasswordReset(anyString(), anyString());
    }

    @Test
    void testFinishPasswordResetWithInvalidKey() {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("invalidResetKey");
        keyAndPassword.setNewPassword("newpassword123");

        when(userService.completePasswordReset("newpassword123", "invalidResetKey")).thenReturn(Optional.empty());

        // When & Then
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> accountResource.finishPasswordReset(keyAndPassword))
            .withMessage("No user was found for this reset key");
    }

    @Test
    void testConstructor() {
        // When
        AccountResource resource = new AccountResource(userRepository, userService, mailService);

        // Then
        assertThat(resource).isNotNull();
    }

    private ManagedUserVM createValidManagedUserVM() {
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(TEST_LOGIN);
        managedUserVM.setPassword(TEST_PASSWORD);
        managedUserVM.setEmail(TEST_EMAIL);
        managedUserVM.setActivated(false);
        managedUserVM.setAuthorities(Set.of("ROLE_USER"));
        return managedUserVM;
    }

    private User createTestUser() {
        User user = new User();
        user.setLogin(TEST_LOGIN);
        user.setEmail(TEST_EMAIL);
        user.setPassword(TEST_PASSWORD);
        user.setActivated(true);
        user.setActivationKey(ACTIVATION_KEY);
        return user;
    }

    private AdminUserDTO createAdminUserDTO() {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(TEST_LOGIN);
        userDTO.setEmail(TEST_EMAIL);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setLangKey("en");
        userDTO.setImageUrl("http://example.com/avatar.jpg");
        return userDTO;
    }
}
