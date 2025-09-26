package bessa.morangon.rafael.TaskFlow.service;

import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.UserAlreadyExistsException;
import bessa.morangon.rafael.TaskFlow.domain.dto.UserDTO;
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
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class
UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // Dados de teste reutilizáveis
    private User validUser;
    private UserDTO userDTO;
    private UriComponentsBuilder uriBuilder;

    @BeforeEach
    void setUp() {
        // Arrange - Preparar dados de teste
        validUser = new User();
        validUser.setId(1L);
        validUser.setFullName("João Silva");
        validUser.setAge(30);
        validUser.setEmail("joao@email.com");
        validUser.setPassword("MinhaSenh@123");

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFullName("João Silva");
        userDTO.setAge(30);
        userDTO.setEmail("joao@email.com");

        uriBuilder = UriComponentsBuilder.newInstance();
    }

    @Nested
    @DisplayName("getById Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return user when ID exists")
        void shouldReturnUserWhenIdExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(validUser));
            when(modelMapper.map(validUser, UserDTO.class)).thenReturn(userDTO);

            // When
            ResponseEntity<UserDTO> response = userService.getById(1L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(1L);
            assertThat(response.getBody().getEmail()).isEqualTo("joao@email.com");

            verify(userRepository).findById(1L);
            verify(modelMapper).map(validUser, UserDTO.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenIdNotExists() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("id")
                    .hasMessageContaining("999");

            verify(userRepository).findById(999L);
            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<User> users = List.of(validUser);
            Page<User> userPage = new PageImpl<>(users, pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(modelMapper.map(validUser, UserDTO.class)).thenReturn(userDTO);

            // When
            ResponseEntity<Page<UserDTO>> response = userService.getAllUsers(pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getTotalElements()).isEqualTo(1);

            verify(userRepository).findAll(pageable);
            verify(modelMapper).map(validUser, UserDTO.class);
        }

        @Test
        @DisplayName("Should return empty page when no users exist")
        void shouldReturnEmptyPageWhenNoUsersExist() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = Page.empty(pageable);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            // When
            ResponseEntity<Page<UserDTO>> response = userService.getAllUsers(pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).isEmpty();
            assertThat(response.getBody().getTotalElements()).isEqualTo(0);

            verify(userRepository).findAll(pageable);
            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("createNewUser Tests")
    class CreateNewUserTests {

        @Test
        @DisplayName("Should create new user successfully")
        void shouldCreateNewUserSuccessfully() {
            // Given
            User newUser = new User();
            newUser.setFullName("Maria Santos");
            newUser.setAge(25);
            newUser.setEmail("maria@email.com");
            newUser.setPassword("MinhaSenh@456");

            User savedUser = new User();
            savedUser.setId(2L);
            savedUser.setFullName("Maria Santos");
            savedUser.setAge(25);
            savedUser.setEmail("maria@email.com");
            savedUser.setPassword("encodedPassword");

            when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("MinhaSenh@456")).thenReturn("encodedPassword");
            when(userRepository.save(newUser)).thenReturn(savedUser);
            when(modelMapper.map(savedUser, UserDTO.class)).thenReturn(userDTO);

            // When
            ResponseEntity<UserDTO> response = userService.createNewUser(newUser, uriBuilder);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getHeaders().getLocation()).isNotNull();

            verify(userRepository).findByEmail("maria@email.com");
            verify(passwordEncoder).encode("MinhaSenh@456");
            verify(userRepository).save(newUser);
            verify(modelMapper).map(savedUser, UserDTO.class);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            User newUser = new User();
            newUser.setEmail("joao@email.com");

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When & Then
            assertThatThrownBy(() -> userService.createNewUser(newUser, uriBuilder))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("joao@email.com");

            verify(userRepository).findByEmail("joao@email.com");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException on DataIntegrityViolationException")
        void shouldHandleDataIntegrityViolationException() {
            // Given
            User newUser = new User();
            newUser.setEmail("teste@email.com");
            newUser.setPassword("MinhaSenh@123");

            when(userRepository.findByEmail("teste@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("Constraint violation"));

            // When & Then
            assertThatThrownBy(() -> userService.createNewUser(newUser, uriBuilder))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("teste@email.com");

            verify(userRepository).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully without changing email")
        void shouldUpdateUserSuccessfullyWithoutChangingEmail() {
            // Given
            User updateData = new User();
            updateData.setFullName("João Silva Updated");
            updateData.setAge(31);
            updateData.setEmail("joao@email.com"); // mesmo email
            updateData.setPassword("NovaSenh@123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(validUser));
            when(passwordEncoder.encode("NovaSenh@123")).thenReturn("newEncodedPassword");
            when(modelMapper.map(validUser, UserDTO.class)).thenReturn(userDTO);

            // When
            ResponseEntity<UserDTO> response = userService.updateUser(updateData, 1L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verificar se os dados foram atualizados no objeto existente
            assertThat(validUser.getFullName()).isEqualTo("João Silva Updated");
            assertThat(validUser.getAge()).isEqualTo(31);
            assertThat(validUser.getPassword()).isEqualTo("newEncodedPassword");

            verify(userRepository).findById(1L);
            verify(passwordEncoder).encode("NovaSenh@123");
            verify(modelMapper).map(validUser, UserDTO.class);
        }

        @Test
        @DisplayName("Should update user successfully with different email")
        void shouldUpdateUserSuccessfullyWithDifferentEmail() {
            // Given
            User updateData = new User();
            updateData.setFullName("João Silva");
            updateData.setAge(30);
            updateData.setEmail("joao.novo@email.com"); // email diferente

            when(userRepository.findById(1L)).thenReturn(Optional.of(validUser));
            when(userRepository.findByEmail("joao.novo@email.com")).thenReturn(Optional.empty());
            when(modelMapper.map(validUser, UserDTO.class)).thenReturn(userDTO);

            // When
            ResponseEntity<UserDTO> response = userService.updateUser(updateData, 1L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(validUser.getEmail()).isEqualTo("joao.novo@email.com");

            verify(userRepository).findByEmail("joao.novo@email.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            User updateData = new User();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateData, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when trying to update to existing email")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            User updateData = new User();
            updateData.setEmail("outro@email.com");

            User anotherUser = new User();
            anotherUser.setId(2L);
            anotherUser.setEmail("outro@email.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(validUser));
            when(userRepository.findByEmail("outro@email.com")).thenReturn(Optional.of(anotherUser));

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(updateData, 1L))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("outro@email.com");
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(validUser));

            // When
            ResponseEntity<Void> response = userService.deleteUser(1L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(userRepository).findById(1L);
            verify(userRepository).delete(validUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).findById(999L);
            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("findByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should return user when email exists")
        void shouldReturnUserWhenEmailExists() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When
            User result = userService.findByEmail("joao@email.com");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("joao@email.com");
            assertThat(result.getId()).isEqualTo(1L);

            verify(userRepository).findByEmail("joao@email.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Given
            when(userRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findByEmail("inexistente@email.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("email")
                    .hasMessageContaining("inexistente@email.com");

            verify(userRepository).findByEmail("inexistente@email.com");
        }
    }
}