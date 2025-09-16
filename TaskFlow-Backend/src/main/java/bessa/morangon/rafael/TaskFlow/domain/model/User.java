package bessa.morangon.rafael.TaskFlow.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "[a-zA-Z ]+", message = "Full name must contain only letters and spaces")
    private String fullName;
    @Min(value = 0, message = "Age must be a positive number")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;
    @NotBlank(message = "E-mail is required")
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    //@Size(min = 6, max = 50, message = "Password must be between 6 and 20 characters")
    @Pattern(regexp = ".*[A-Z].*", message = "Must contain at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Must contain at least one lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Must contain at least one number")
    @Pattern(regexp = ".*[@$!%*?&].*", message = "Must contain at least one special character (@$!%*?&)")
    private String password;

    //FetchType.LAZY significa: "não carregar as tasks automaticamente quando eu pegar um usuário".
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Task> tasks;

}
