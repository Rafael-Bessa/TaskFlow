import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TaskService, Task, Priority, Status } from '../services/task.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tasks.html',
  styleUrl: './tasks.css'
})
export class Tasks implements OnInit, OnDestroy {
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  showModal = false;
  isEditing = false;
  currentTask: Task = this.getEmptyTask();
  isLoading = false;
  errorMessage = '';
  
  private subscriptions: Subscription[] = [];
  
  // Filtros
  statusFilter = '';
  priorityFilter = '';
  searchTerm = '';
  
  // Paginação (para uso futuro)
  currentPage = 0;
  totalPages = 0;
  pageSize = 8;

  // Enums para o template
  Priority = Priority;
  Status = Status;
  
  private router = inject(Router);
  private taskService = inject(TaskService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit() {
    this.subscribeToUserChanges();
    this.subscribeToTasks();
    
    if (this.authService.isAuthenticated()) {
      this.loadTasks();
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private subscribeToUserChanges(): void {
    const userSub = this.authService.onUserChange().subscribe(user => {
      if (user) {
        console.log('Usuário logado, carregando tasks:', user.email);
        this.loadTasks();
      } else {
        console.log('Usuário deslogado, limpando tasks');
        this.tasks = [];
        this.filteredTasks = [];
        this.taskService.clearTasks();
      }
      this.cdr.markForCheck();
    });
    
    this.subscriptions.push(userSub);
  }

  private subscribeToTasks(): void {
    const tasksSub = this.taskService.tasks$.subscribe(tasks => {
      this.tasks = tasks;
      this.applyFilters();
      this.cdr.markForCheck();
    });
    
    this.subscriptions.push(tasksSub);
  }

  getEmptyTask(): Task {
    return {
      title: '',
      description: '',
      dueDate: '',
      priority: Priority.MEDIUM,
      status: Status.PENDING
    };
  }

  loadTasks() {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    
    console.log('Carregando tasks para usuário:', this.authService.getCurrentUser()?.email);
    
    const loadSub = this.taskService.getAllTasks().subscribe({
      next: (tasks: Task[]) => {
        console.log('Tasks carregadas:', tasks.length, 'tasks');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Erro ao carregar tasks:', error);
        this.isLoading = false;
        
        if (error.status === 401) {
          this.authService.logout();
        } else if (error.status === 0) {
          this.errorMessage = 'Erro de conexão. Verifique se o servidor está rodando';
        } else {
          this.errorMessage = 'Erro ao carregar as tasks. Tente novamente';
        }
        this.cdr.markForCheck();
      }
    });

    this.subscriptions.push(loadSub);
  }

  applyFilters() {
    this.filteredTasks = this.tasks.filter(task => {
      const matchesStatus = !this.statusFilter || task.status === this.statusFilter;
      const matchesPriority = !this.priorityFilter || task.priority === this.priorityFilter;
      const matchesSearch = !this.searchTerm || 
        task.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        task.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      return matchesStatus && matchesPriority && matchesSearch;
    });
  }

  onFilterChange() {
    this.applyFilters();
    this.cdr.markForCheck();
  }

  openModal(task?: Task) {
    this.showModal = true;
    this.isEditing = !!task;
    
    if (task) {
      // Editando: converte a data do backend para o formato do input date
      this.currentTask = { 
        ...task,
        dueDate: this.convertToDateInputFormat(task.dueDate)
      };
    } else {
      // Criando: usa task vazia
      this.currentTask = this.getEmptyTask();
    }
    
    this.errorMessage = '';
    this.cdr.markForCheck();
  }

  closeModal() {
    this.showModal = false;
    this.currentTask = this.getEmptyTask();
    this.errorMessage = '';
    this.isEditing = false;
    this.cdr.markForCheck();
  }

  // Converte data do backend (ISO) para formato YYYY-MM-DD do input type="date"
  private convertToDateInputFormat(isoDate: string): string {
    if (!isoDate) return '';
    const date = new Date(isoDate);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Converte data do input (YYYY-MM-DD) para ISO com horário 23:59:59
  private convertToEndOfDay(dateString: string): string {
    if (!dateString) return '';
    // Adiciona o horário 23:59:59 no final do dia
    return `${dateString}T23:59:59`;
  }

  saveTask() {
    if (!this.currentTask.title || !this.currentTask.title.trim()) {
      this.errorMessage = 'Título é obrigatório';
      this.cdr.markForCheck();
      return;
    }

    if (!this.currentTask.dueDate) {
      this.errorMessage = 'Data de vencimento é obrigatória';
      this.cdr.markForCheck();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    // Converte a data para o final do dia (23:59:59)
    const dueDateWithTime = this.convertToEndOfDay(this.currentTask.dueDate);

    if (this.isEditing && this.currentTask.id) {
      // EDITANDO task existente
      console.log('Editando task:', this.currentTask.id);
      
      const taskToUpdate = {
        ...this.currentTask,
        dueDate: dueDateWithTime
      };
      
      const editSub = this.taskService.updateTask(this.currentTask.id, taskToUpdate).subscribe({
        next: (updatedTask) => {
          console.log('Task atualizada com sucesso:', updatedTask);
          this.isLoading = false;
          this.closeModal();
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao atualizar task:', error);
          this.handleSaveError(error);
        }
      });

      this.subscriptions.push(editSub);

    } else {
      // CRIANDO nova task
      console.log('Criando nova task:', this.currentTask.title);
      
      const newTask: Task = {
        title: this.currentTask.title.trim(),
        description: this.currentTask.description?.trim() || '',
        dueDate: dueDateWithTime, // Data com horário 23:59:59
        priority: this.currentTask.priority || Priority.MEDIUM,
        status: Status.PENDING
      };

      console.log('Dados da nova task (com horário):', newTask);

      const createSub = this.taskService.createTask(newTask).subscribe({
        next: (createdTask) => {
          console.log('Task criada com sucesso:', createdTask);
          this.isLoading = false;
          this.closeModal();
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao criar task:', error);
          this.handleSaveError(error);
        }
      });

      this.subscriptions.push(createSub);
    }
  }

  private handleSaveError(error: any) {
    this.isLoading = false;
    
    if (error.status === 401) {
      this.errorMessage = 'Sessão expirada. Faça login novamente';
      this.authService.logout();
    } else if (error.status === 400) {
      this.errorMessage = error.error?.message || 'Dados inválidos. Verifique os campos';
    } else if (error.status === 0) {
      this.errorMessage = 'Erro de conexão. Verifique se o servidor está rodando';
    } else {
      this.errorMessage = error.error?.message || 'Erro ao salvar a task. Tente novamente';
    }
    
    this.cdr.markForCheck();
  }

  deleteTask(id: number) {
    if (confirm('Tem certeza que deseja excluir esta task?')) {
      console.log('Deletando task:', id);
      
      const deleteSub = this.taskService.deleteTask(id).subscribe({
        next: () => {
          console.log('Task deletada com sucesso:', id);
        },
        error: (error) => {
          console.error('Erro ao deletar task:', error);
          
          if (error.status === 401) {
            this.authService.logout();
          } else if (error.status === 0) {
            alert('Erro de conexão. Verifique se o servidor está rodando');
          } else {
            alert('Erro ao deletar a task. Tente novamente');
          }
        }
      });

      this.subscriptions.push(deleteSub);
    }
  }

  toggleTaskStatus(task: Task) {
    if (!task.id) return;
    
    const updatedTask: Task = {
      ...task,
      status: task.status === Status.DONE ? Status.PENDING : Status.DONE
    };

    const toggleSub = this.taskService.updateTask(task.id, updatedTask).subscribe({
      next: (updated) => {
        console.log('Status da task alterado:', updated);
      },
      error: (error) => {
        console.error('Erro ao alterar status da task:', error);
        if (error.status === 401) {
          this.authService.logout();
        }
      }
    });

    this.subscriptions.push(toggleSub);
  }

  getStatusClass(status: Status): string {
    switch(status) {
      case Status.DONE: return 'status-completed';
      default: return 'status-pending';
    }
  }

  getPriorityClass(priority: Priority): string {
    switch(priority) {
      case Priority.HIGH: return 'priority-high';
      case Priority.MEDIUM: return 'priority-medium';
      default: return 'priority-low';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('pt-BR');
  }

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date();
  }

  logout() {
    this.authService.logout();
  }
}