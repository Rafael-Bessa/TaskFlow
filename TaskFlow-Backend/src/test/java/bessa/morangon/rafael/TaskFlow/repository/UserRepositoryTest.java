package bessa.morangon.rafael.TaskFlow.repository;

import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * IMPORTANTE: Todos os dados de teste respeitam as validações da entidade:
 * - fullName: apenas letras e espaços (sem números)
 * - email: formato válido
 * - age: 0-120
 * - password: maiúscula + minúscula + número + caractere especial
 */
@DataJpaTest
@ActiveProfiles("dev")
@EntityScan(basePackages = "bessa.morangon.rafael.TaskFlow.domain.model")
@EnableJpaRepositories(basePackages = "bessa.morangon.rafael.TaskFlow.domain.repository")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    // Dados que PASSAM na validação Bean Validation
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_FULL_NAME = "John Doe";
    private static final Integer VALID_AGE = 25;
    private static final String VALID_PASSWORD = "MyPass123@";


    private static final String[] VALID_NAMES = {
            "Alice Silva", "Bob Santos", "Carol Lima", "David Costa", "Eva Rocha",
            "Frank Alves", "Grace Dias", "Henry Nunes", "Iris Moura", "Jack Pinto",
            "Kate Lopes", "Luis Gomes", "Mary Souza", "Nick Pereira", "Olga Ramos"
    };

    @BeforeEach
    void setUp() {
        entityManager.clear();
    }

    private User createValidUser() {
        User user = new User();
        user.setFullName(VALID_FULL_NAME);  // ✅ Só letras e espaços
        user.setAge(VALID_AGE);             // ✅ 0-120
        user.setEmail(VALID_EMAIL);         // ✅ Formato email válido
        user.setPassword(VALID_PASSWORD);   // ✅ Maiúscula+minúscula+número+especial
        return user;
    }

    private User createUserWithEmail(String email) {
        User user = createValidUser();
        user.setEmail(email);
        return user;
    }

    private User createUserWithName(String fullName) {
        User user = createValidUser();
        user.setFullName(fullName);
        return user;
    }

    // =====================================================
    // TESTE DE CONECTIVIDADE
    // =====================================================

    @Test
    @Order(1)
    @DisplayName("Database connection and schema creation should work")
    void testDatabaseConnection() {
        // Teste básico com dados que passam na validação
        User user = createValidUser();
        user.setEmail("connection.test@example.com");

        User saved = entityManager.persistAndFlush(user);
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("connection.test@example.com");
    }

    // =====================================================
    // TESTES - findByEmail()
    // =====================================================

    @Test
    @Order(2)
    @DisplayName("Should find user by existing email")
    void shouldFindUserByExistingEmail() {
        // Given - usuário válido
        User user = createValidUser();
        User saved = entityManager.persistAndFlush(user);
        entityManager.clear();

        // When
        Optional<User> result = userRepository.findByEmail(VALID_EMAIL);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getEmail()).isEqualTo(VALID_EMAIL);
        assertThat(result.get().getFullName()).isEqualTo(VALID_FULL_NAME);
    }

    @Test
    @Order(3)
    @DisplayName("Should return empty for non-existent email")
    void shouldReturnEmptyForNonExistentEmail() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("Should be case sensitive for email search")
    void shouldBeCaseSensitive() {
        // Given
        User user = createValidUser();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // When
        Optional<User> resultUpper = userRepository.findByEmail(VALID_EMAIL.toUpperCase());
        Optional<User> resultLower = userRepository.findByEmail(VALID_EMAIL);

        // Then
        assertThat(resultLower).isPresent();
        assertThat(resultUpper).isEmpty();
    }

    // =====================================================
    // TESTES - existsByEmail()
    // =====================================================

    @Test
    @Order(5)
    @DisplayName("Should return true for existing email")
    void shouldReturnTrueForExistingEmail() {
        // Given
        User user = createValidUser();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // When
        boolean exists = userRepository.existsByEmail(VALID_EMAIL);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Should return false for non-existent email")
    void shouldReturnFalseForNonExistentEmail() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    // =====================================================
    // TESTES DE CONSTRAINT
    // =====================================================

    @Test
    @Order(7)
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        User user1 = createValidUser();
        entityManager.persistAndFlush(user1);

        // When/Then - mesmo email deve falhar
        User user2 = createValidUser();
        user2.setFullName("Different Name"); // ✅ Nome válido (só letras e espaços)

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(user2);
        }).hasCauseInstanceOf(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);
    }

    // =====================================================
    // TESTES DE FUNCIONALIDADES ESPECIAIS
    // =====================================================

    @Test
    @Order(8)
    @DisplayName("Should handle special characters in email")
    void shouldHandleSpecialCharactersInEmail() {
        // Given - email válido com caracteres especiais
        String specialEmail = "test.email+tag@sub-domain.example.com";
        User user = createUserWithEmail(specialEmail);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // When
        Optional<User> result = userRepository.findByEmail(specialEmail);
        boolean exists = userRepository.existsByEmail(specialEmail);

        // Then
        assertThat(result).isPresent();
        assertThat(exists).isTrue();
        assertThat(result.get().getEmail()).isEqualTo(specialEmail);
    }

    @Test
    @Order(9)
    @DisplayName("Should handle null email gracefully")
    void shouldHandleNullEmailGracefully() {
        // When/Then - repository deve lidar com null sem quebrar
        Optional<User> result = userRepository.findByEmail(null);
        boolean exists = userRepository.existsByEmail(null);

        assertThat(result).isEmpty();
        assertThat(exists).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("Should work with multiple users (performance test)")
    void shouldWorkWithMultipleUsersPerformantly() {
        // Given - 15 usuários com nomes VÁLIDOS (sem números)
        for (int i = 0; i < VALID_NAMES.length; i++) {
            User user = createUserWithName(VALID_NAMES[i]);
            user.setEmail("user" + (i + 1) + "@example.com"); // Email único
            entityManager.persist(user);

            // Flush periodicamente para evitar memory issues
            if (i % 5 == 0) {
                entityManager.flush();
            }
        }
        entityManager.flush();
        entityManager.clear();

        // When - testa performance com múltiplas consultas
        long startTime = System.currentTimeMillis();

        // Verifica alguns usuários específicos
        boolean exists1 = userRepository.existsByEmail("user1@example.com");
        boolean exists8 = userRepository.existsByEmail("user8@example.com");
        boolean exists15 = userRepository.existsByEmail("user15@example.com");
        boolean existsNonExistent = userRepository.existsByEmail("user999@example.com");

        Optional<User> foundUser = userRepository.findByEmail("user5@example.com");

        long endTime = System.currentTimeMillis();

        // Then - deve ser performático e correto
        assertThat(exists1).isTrue();
        assertThat(exists8).isTrue();
        assertThat(exists15).isTrue();
        assertThat(existsNonExistent).isFalse();

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFullName()).isEqualTo(VALID_NAMES[4]); // user5 = índice 4

        // Performance check
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(1000) // menos de 1 segundo
                .as("Operations took %d ms, expected < 1000ms", duration);
    }

    @Test
    @Order(11)
    @DisplayName("Should maintain consistency between findByEmail and existsByEmail")
    void shouldMaintainConsistencyBetweenMethods() {
        // Given - alguns usuários válidos
        User user1 = createValidUser();
        entityManager.persistAndFlush(user1);

        User user2 = createUserWithEmail("another@example.com");
        user2.setFullName("Another Person"); // ✅ Nome válido
        entityManager.persistAndFlush(user2);
        entityManager.clear();

        // When/Then - testa consistência
        String[] testEmails = {
                VALID_EMAIL,              // existe
                "another@example.com",    // existe
                "missing@example.com",    // não existe
        };

        for (String email : testEmails) {
            boolean existsResult = userRepository.existsByEmail(email);
            Optional<User> findResult = userRepository.findByEmail(email);

            assertThat(existsResult).isEqualTo(findResult.isPresent())
                    .as("Inconsistency for email: %s", email);
        }
    }

    // =====================================================
    // TESTES DE VALIDAÇÃO (testam que Bean Validation funciona)
    // =====================================================

    @Test
    @Order(12)
    @DisplayName("Should reject invalid fullName with numbers")
    void shouldRejectInvalidFullNameWithNumbers() {
        // Given - nome com números (inválido pela regex)
        User userWithNumbers = createValidUser();
        userWithNumbers.setFullName("User123"); // ❌ Contém números
        userWithNumbers.setEmail("invalid.name@example.com"); // Email único

        // When/Then - deve falhar na validação Bean Validation
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(userWithNumbers);
        }).isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("Full name must contain only letters and spaces");
    }

    @Test
    @Order(13)
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        // Given - email inválido
        User userWithInvalidEmail = createValidUser();
        userWithInvalidEmail.setEmail("invalid-email"); // ❌ Formato inválido
        userWithInvalidEmail.setFullName("Valid Name"); // Nome válido

        // When/Then - deve falhar na validação Bean Validation
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(userWithInvalidEmail);
        }).isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("deve ser um endereço de e-mail bem formado");
    }

    @Test
    @Order(14)
    @DisplayName("Should accept valid user data")
    void shouldAcceptValidUserData() {
        // Given - usuário com TODOS os dados válidos
        User validUser = new User();
        validUser.setFullName("Maria Silva");     // ✅ Só letras e espaços
        validUser.setAge(30);                     // ✅ 0-120
        validUser.setEmail("maria.silva@test.com"); // ✅ Email válido
        validUser.setPassword("ValidPass123!");   // ✅ Maiúscula+minúscula+número+especial

        // When/Then - deve salvar sem problemas
        assertThatCode(() -> {
            User saved = entityManager.persistAndFlush(validUser);
            assertThat(saved.getId()).isNotNull();

            Optional<User> found = userRepository.findByEmail("maria.silva@test.com");
            assertThat(found).isPresent();
            assertThat(found.get().getFullName()).isEqualTo("Maria Silva");
        }).doesNotThrowAnyException();
    }
}