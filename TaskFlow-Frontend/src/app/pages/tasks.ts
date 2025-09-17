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
    
    // ✅ CORRIGIDO: Só faz subscribe uma vez
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
    this.currentTask = task ? { ...task } : this.getEmptyTask();
    this.errorMessage = '';
    this.cdr.markForCheck();
  }

  // ✅ CORRIGIDO: Força update da view
  closeModal() {
    this.showModal = false;
    this.currentTask = this.getEmptyTask();
    this.errorMessage = '';
    this.isEditing = false;
    this.cdr.markForCheck();
  }

  saveTask() {
    if (!this.currentTask.title.trim()) {
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

    if (this.isEditing && this.currentTask.id) {
      // ✅ EDITANDO task existente
      console.log('Editando task:', this.currentTask.id);
      
      const editSub = this.taskService.updateTask(this.currentTask.id, this.currentTask).subscribe({
        next: (updatedTask) => {
          console.log('Task atualizada com sucesso:', updatedTask);
          this.closeModal(); // ✅ FECHA O MODAL
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao atualizar task:', error);
          this.handleSaveError(error);
        }
      });

      this.subscriptions.push(editSub);

    } else {
      // ✅ CRIANDO nova task
      console.log('Criando nova task:', this.currentTask.title);
      
      const newTask: Task = {
        title: this.currentTask.title,
        description: this.currentTask.description,
        dueDate: this.currentTask.dueDate,
        priority: this.currentTask.priority,
        status: Status.PENDING
      };

      const createSub = this.taskService.createTask(newTask).subscribe({
        next: (createdTask) => {
          console.log('Task criada com sucesso:', createdTask);
          this.closeModal(); // ✅ FECHA O MODAL
          this.isLoading = false;
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
      this.authService.logout();
    } else if (error.status === 400) {
      this.errorMessage = 'Dados inválidos. Verifique os campos';
    } else if (error.status === 0) {
      this.errorMessage = 'Erro de conexão. Verifique se o servidor está rodando';
    } else {
      this.errorMessage = 'Erro ao salvar a task. Tente novamente';
    }
    this.cdr.markForCheck();
  }

  // ✅ CORRIGIDO: Confirmação e recarregamento
  deleteTask(id: number) {
    if (confirm('Tem certeza que deseja excluir esta task?')) {
      console.log('Deletando task:', id);
      
      const deleteSub = this.taskService.deleteTask(id).subscribe({
        next: () => {
          console.log('Task deletada com sucesso:', id);
          // TaskService já recarrega a lista automaticamente
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

  // ✅ CORRIGIDO: Toggle de status
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