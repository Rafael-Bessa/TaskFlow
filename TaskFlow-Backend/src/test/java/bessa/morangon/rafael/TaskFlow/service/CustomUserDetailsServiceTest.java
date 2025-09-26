package bessa.morangon.rafael.TaskFlow.service;

import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    // Dados de teste
    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setId(1L);
        validUser.setFullName("João Silva");
        validUser.setEmail("joao@email.com");
        validUser.setPassword("$2a$10$encodedPasswordHash"); // Senha já criptografada
        validUser.setAge(30);
    }

    @Nested
    @DisplayName("loadUserByUsername Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user successfully when email exists")
        void shouldLoadUserSuccessfullyWhenEmailExists() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("joao@email.com");

            // Then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("joao@email.com");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER");

            // Verificações adicionais de segurança
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();

            verify(userRepository).findByEmail("joao@email.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Given
            String nonExistentEmail = "naoexiste@email.com";
            when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(nonExistentEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + nonExistentEmail);

            verify(userRepository).findByEmail(nonExistentEmail);
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void shouldHandleNullEmailGracefully() {
            // Given
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: null");

            verify(userRepository).findByEmail(null);
        }

        @Test
        @DisplayName("Should handle empty email gracefully")
        void shouldHandleEmptyEmailGracefully() {
            // Given
            String emptyEmail = "";
            when(userRepository.findByEmail(emptyEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emptyEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: ");

            verify(userRepository).findByEmail(emptyEmail);
        }

        @Test
        @DisplayName("Should handle whitespace-only email gracefully")
        void shouldHandleWhitespaceOnlyEmailGracefully() {
            // Given
            String whitespaceEmail = "   ";
            when(userRepository.findByEmail(whitespaceEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(whitespaceEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + whitespaceEmail);

            verify(userRepository).findByEmail(whitespaceEmail);
        }

        @Test
        @DisplayName("Should load user with different email formats correctly")
        void shouldLoadUserWithDifferentEmailFormatsCorrectly() {
            // Given - Email com diferentes formatos válidos
            String[] validEmails = {
                    "test@example.com",
                    "user.name@domain.co.uk",
                    "user+tag@example.org",
                    "123@numbers.com"
            };

            for (String email : validEmails) {
                // Criar user específico para cada email
                User userForEmail = new User();
                userForEmail.setId(1L);
                userForEmail.setEmail(email);
                userForEmail.setPassword("encodedPassword123");

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(userForEmail));

                // When
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // Then
                assertThat(userDetails.getUsername()).isEqualTo(email);
                assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
                assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER");

                // Reset para próxima iteração
                reset(userRepository);
            }
        }

        @Test
        @DisplayName("Should preserve exact password hash from database")
        void shouldPreserveExactPasswordHashFromDatabase() {
            // Given - Diferentes tipos de hash de senha
            String[] passwordHashes = {
                    "$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa", // BCrypt
                    "$2b$12$someOtherBcryptHash", // BCrypt variant
                    "plainTextPassword", // Plain text (não recomendado, mas pode existir)
                    "md5HashExample123", // MD5 (legado)
            };

            for (String passwordHash : passwordHashes) {
                // Setup
                User userWithSpecificPassword = new User();
                userWithSpecificPassword.setEmail("test@example.com");
                userWithSpecificPassword.setPassword(passwordHash);

                when(userRepository.findByEmail("test@example.com"))
                        .thenReturn(Optional.of(userWithSpecificPassword));

                // When
                UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

                // Then
                assertThat(userDetails.getPassword()).isEqualTo(passwordHash);

                // Reset para próxima iteração
                reset(userRepository);
            }
        }

        @Test
        @DisplayName("Should create UserDetails with correct authorities structure")
        void shouldCreateUserDetailsWithCorrectAuthoritiesStructure() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("joao@email.com");

            // Then - Verificações detalhadas das authorities
            assertThat(userDetails.getAuthorities()).isNotNull();
            assertThat(userDetails.getAuthorities()).isNotEmpty();
            assertThat(userDetails.getAuthorities()).hasSize(1);

            // Verificar que a authority é exatamente "USER"
            String authority = userDetails.getAuthorities().iterator().next().getAuthority();
            assertThat(authority).isEqualTo("USER");
            assertThat(authority).isNotEqualTo("ROLE_USER"); // Não deve ter prefixo ROLE_
            assertThat(authority).isNotEqualTo("user"); // Case sensitive
        }

        @Test
        @DisplayName("Should handle users with minimal required data")
        void shouldHandleUsersWithMinimalRequiredData() {
            // Given - User com apenas dados mínimos necessários
            User minimalUser = new User();
            minimalUser.setEmail("minimal@test.com");
            minimalUser.setPassword("minimalPassword");
            // Outros campos podem ser null

            when(userRepository.findByEmail("minimal@test.com")).thenReturn(Optional.of(minimalUser));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("minimal@test.com");

            // Then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("minimal@test.com");
            assertThat(userDetails.getPassword()).isEqualTo("minimalPassword");
            assertThat(userDetails.getAuthorities()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security Tests")
    class EdgeCasesAndSecurityTests {

        @Test
        @DisplayName("Should not expose sensitive user information in UserDetails")
        void shouldNotExposeSensitiveUserInformationInUserDetails() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("joao@email.com");

            // Then - UserDetails deve conter apenas informações de autenticação
            assertThat(userDetails.getUsername()).isEqualTo("joao@email.com");
            assertThat(userDetails.getPassword()).isNotNull();

            // Não deve expor informações pessoais do User original
            // (como fullName, age, etc. não devem estar acessíveis via UserDetails)
            assertThat(userDetails.toString()).doesNotContain("João Silva");
            assertThat(userDetails.toString()).doesNotContain("30"); // age
        }

        @Test
        @DisplayName("Should call repository exactly once per authentication attempt")
        void shouldCallRepositoryExactlyOncePerAuthenticationAttempt() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When
            customUserDetailsService.loadUserByUsername("joao@email.com");

            // Then
            verify(userRepository, times(1)).findByEmail("joao@email.com");
            verify(userRepository, never()).findByEmail(argThat(email -> !email.equals("joao@email.com")));
        }

        @Test
        @DisplayName("Should handle concurrent authentication requests properly")
        void shouldHandleConcurrentAuthenticationRequestsProperly() {
            // Given
            User user1 = new User();
            user1.setEmail("user1@test.com");
            user1.setPassword("password1");

            User user2 = new User();
            user2.setEmail("user2@test.com");
            user2.setPassword("password2");

            when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user1));
            when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(user2));

            // When - Simular chamadas "simultâneas"
            UserDetails userDetails1 = customUserDetailsService.loadUserByUsername("user1@test.com");
            UserDetails userDetails2 = customUserDetailsService.loadUserByUsername("user2@test.com");

            // Then - Cada chamada deve retornar os dados corretos
            assertThat(userDetails1.getUsername()).isEqualTo("user1@test.com");
            assertThat(userDetails1.getPassword()).isEqualTo("password1");

            assertThat(userDetails2.getUsername()).isEqualTo("user2@test.com");
            assertThat(userDetails2.getPassword()).isEqualTo("password2");

            // Verificar que cada busca foi feita corretamente
            verify(userRepository).findByEmail("user1@test.com");
            verify(userRepository).findByEmail("user2@test.com");
        }
    }
}