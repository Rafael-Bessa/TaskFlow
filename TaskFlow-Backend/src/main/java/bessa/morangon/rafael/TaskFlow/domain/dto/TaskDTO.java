package bessa.morangon.rafael.TaskFlow.domain.dto;

import bessa.morangon.rafael.TaskFlow.domain.model.Priority;
import bessa.morangon.rafael.TaskFlow.domain.model.Status;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Priority priority;
    private Status status;
    private User user;
}
