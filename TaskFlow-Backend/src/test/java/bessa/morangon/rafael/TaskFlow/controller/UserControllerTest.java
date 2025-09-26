package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.GlobalExceptionHandler;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.UserAlreadyExistsException;
import bessa.morangon.rafael.TaskFlow.domain.dto.UserDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private User validUser;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Configurar MockMvc com GlobalExceptionHandler para tratar exceções corretamente
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        validUser = new User();
        validUser.setId(1L);
        validUser.setFullName("John Silva");
        validUser.setEmail("john@email.com");
        validUser.setAge(25);
        validUser.setPassword("Password123@");

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFullName("John Silva");
        userDTO.setEmail("john@email.com");
        userDTO.setAge(25);
    }

    // ================== GET BY ID TESTS ==================
    @Test
    @DisplayName("Should return user when valid ID is provided")
    void getUserById_ShouldReturnUser_WhenValidId() throws Exception {
        when(userService.getById(1L)).thenReturn(ResponseEntity.ok(userDTO));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("John Silva"))
                .andExpect(jsonPath("$.email").value("john@email.com"))
                .andExpect(jsonPath("$.age").value(25));

        verify(userService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void getUserById_ShouldReturn404_WhenUserNotFound() throws Exception {
        when(userService.getById(999L))
                .thenThrow(new ResourceNotFoundException("User", "id", 999L));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: '999'"))
                .andExpect(jsonPath("$.status").value(404));

        verify(userService, times(1)).getById(999L);
    }

    // ================== GET ALL USERS TESTS ==================
    @Test
    @DisplayName("Should return paginated users list")
    void getAllUsers_ShouldReturnPaginatedUsers() throws Exception {
        Page<UserDTO> userPage = new PageImpl<>(Arrays.asList(userDTO), PageRequest.of(0, 8), 1);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(ResponseEntity.ok(userPage));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].fullName").value("John Silva"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(8));

        verify(userService, times(1)).getAllUsers(any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle custom pagination parameters")
    void getAllUsers_ShouldHandleCustomPaginationParameters() throws Exception {
        // Cria a página mock
        Page<UserDTO> userPage = new PageImpl<>(
                Arrays.asList(userDTO),
                PageRequest.of(0, 5, Sort.by("fullName")),
                1
        );

        // Mocka o service para retornar ResponseEntity<Page<UserDTO>>
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(ResponseEntity.ok(userPage));

        mockMvc.perform(get("/users")
                        .param("size", "5")
                        .param("page", "0")
                        .param("sort", "fullName"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].fullName").value("John Silva"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(userService, times(1)).getAllUsers(any(Pageable.class));
    }


    // ================== CREATE USER TESTS ==================
    @Test
    @DisplayName("Should create user successfully with valid data")
    void createUser_ShouldCreateUser_WhenValidData() throws Exception {
        URI location = URI.create("/users/1");
        when(userService.createNewUser(any(User.class), any(UriComponentsBuilder.class)))
                .thenReturn(ResponseEntity.created(location).body(userDTO));

        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("John Silva"))
                .andExpect(jsonPath("$.email").value("john@email.com"));

        verify(userService, times(1)).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void createUser_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        when(userService.createNewUser(any(User.class), any(UriComponentsBuilder.class)))
                .thenThrow(new UserAlreadyExistsException("john@email.com"));

        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(409));

        verify(userService, times(1)).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when validation fails - empty fields")
    void createUser_ShouldReturn400_WhenValidationFails() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName(""); // Vazio - falha @NotBlank
        invalidUser.setEmail("invalid-email"); // Email inválido
        invalidUser.setAge(-5); // Idade negativa - falha @Min
        invalidUser.setPassword("123"); // Não atende os @Pattern

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").exists())
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when fullName has invalid characters")
    void createUser_ShouldReturn400_WhenInvalidFullName() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName("John123 Silva"); // Números não permitidos
        invalidUser.setEmail("john@email.com");
        invalidUser.setAge(25);
        invalidUser.setPassword("Password123@");

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.fullName")
                        .value("Full name must contain only letters and spaces"));

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when password missing requirements")
    void createUser_ShouldReturn400_WhenPasswordInvalid() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName("John Silva");
        invalidUser.setEmail("john@email.com");
        invalidUser.setAge(25);
        invalidUser.setPassword("password"); // Sem maiúscula, número e especial

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").exists());

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when JSON is malformed")
    void createUser_ShouldReturn400_WhenMalformedJson() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    // ================== UPDATE USER TESTS ==================
    @Test
    @DisplayName("Should update user successfully")
    void updateUser_ShouldUpdateUser_WhenValidData() throws Exception {
        when(userService.updateUser(any(User.class), eq(1L)))
                .thenReturn(ResponseEntity.ok(userDTO));

        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("John Silva"))
                .andExpect(jsonPath("$.email").value("john@email.com"));

        verify(userService, times(1)).updateUser(any(User.class), eq(1L));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent user")
    void updateUser_ShouldReturn404_WhenUserNotFound() throws Exception {
        when(userService.updateUser(any(User.class), eq(999L)))
                .thenThrow(new ResourceNotFoundException("User", "id", 999L));

        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(put("/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: '999'"));

        verify(userService, times(1)).updateUser(any(User.class), eq(999L));
    }

    @Test
    @DisplayName("Should return 409 when updating with existing email")
    void updateUser_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        when(userService.updateUser(any(User.class), eq(1L)))
                .thenThrow(new UserAlreadyExistsException("john@email.com"));

        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(userService, times(1)).updateUser(any(User.class), eq(1L));
    }

    @Test
    @DisplayName("Should return 400 when updating with invalid data")
    void updateUser_ShouldReturn400_WhenInvalidData() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName(""); // Nome vazio
        invalidUser.setEmail("invalid-email");
        invalidUser.setAge(150); // Acima do limite @Max(120)
        invalidUser.setPassword("weak");

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").exists());

        verify(userService, never()).updateUser(any(User.class), eq(1L));
    }

    // ================== DELETE USER TESTS ==================
    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_ShouldDeleteUser_WhenValidId() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent user")
    void deleteUser_ShouldReturn404_WhenUserNotFound() throws Exception {
        when(userService.deleteUser(999L))
                .thenThrow(new ResourceNotFoundException("User", "id", 999L));

        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: '999'"));

        verify(userService, times(1)).deleteUser(999L);
    }

    // ================== EDGE CASES ==================
    @Test
    @DisplayName("Should return 400 for invalid path variables")
    void shouldReturn400_WhenInvalidPathVariable() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getById(any());
    }

    @Test
    @DisplayName("Should return 415 when Content-Type is missing")
    void shouldReturn415_WhenContentTypeIsMissing() throws Exception {
        String userJson = objectMapper.writeValueAsString(validUser);

        mockMvc.perform(post("/users").content(userJson))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when age is negative")
    void createUser_ShouldReturn400_WhenAgeIsNegative() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName("John Silva");
        invalidUser.setEmail("john@email.com");
        invalidUser.setAge(-1); // Idade negativa
        invalidUser.setPassword("Password123@");

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.age")
                        .value("Age must be a positive number"));

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }

    @Test
    @DisplayName("Should return 400 when age exceeds maximum")
    void createUser_ShouldReturn400_WhenAgeExceedsMaximum() throws Exception {
        User invalidUser = new User();
        invalidUser.setFullName("John Silva");
        invalidUser.setEmail("john@email.com");
        invalidUser.setAge(130); // Acima de 120
        invalidUser.setPassword("Password123@");

        String userJson = objectMapper.writeValueAsString(invalidUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.age")
                        .value("Age must not exceed 120"));

        verify(userService, never()).createNewUser(any(User.class), any(UriComponentsBuilder.class));
    }
}