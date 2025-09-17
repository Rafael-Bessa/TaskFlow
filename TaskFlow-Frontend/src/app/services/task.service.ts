import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export enum Status {
  PENDING = 'PENDING',
  DONE = 'DONE'
}

export interface Task {
  id?: number;
  title: string;
  description: string;
  dueDate: string;
  priority: Priority;
  status: Status;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = 'http://localhost:8080/tasks';
  
  private tasksSubject = new BehaviorSubject<Task[]>([]);
  public tasks$ = this.tasksSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private getToken(): string | null {
    if (this.isBrowser() && typeof Storage !== 'undefined') {
      try {
        return localStorage.getItem('token');
      } catch (error) {
        console.warn('Erro ao acessar localStorage para token:', error);
        return null;
      }
    }
    return null;
  }

  private getHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  // ✅ CORRIGIDO: Não faz subscribe interno, apenas retorna o Observable
  getAllTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.apiUrl, {
      headers: this.getHeaders()
    }).pipe(
      tap((tasks) => {
        console.log('Tasks carregadas do backend:', tasks.length);
        this.tasksSubject.next(tasks);
      })
    );
  }

  // ✅ NOVO: Método para recarregar tasks
  reloadTasks(): Observable<Task[]> {
    return this.getAllTasks();
  }

  clearTasks(): void {
    this.tasksSubject.next([]);
  }

  getCurrentTasks(): Task[] {
    return this.tasksSubject.value;
  }

  getTaskById(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.apiUrl}/${id}`, {
      headers: this.getHeaders()
    });
  }

  // ✅ CORRIGIDO: Recarrega tasks após criar
  createTask(task: Task): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, task, {
      headers: this.getHeaders()
    }).pipe(
      tap((newTask) => {
        console.log('Task criada:', newTask);
        // Recarrega todas as tasks para manter consistência
        this.reloadTasks().subscribe();
      })
    );
  }

  // ✅ CORRIGIDO: Recarrega tasks após atualizar
  updateTask(id: number, task: Task): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, task, {
      headers: this.getHeaders()
    }).pipe(
      tap((updatedTask) => {
        console.log('Task atualizada:', updatedTask);
        // Recarrega todas as tasks para manter consistência
        this.reloadTasks().subscribe();
      })
    );
  }

  // ✅ CORRIGIDO: Recarrega tasks após deletar
  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(() => {
        console.log('Task deletada:', id);
        // Recarrega todas as tasks para manter consistência
        this.reloadTasks().subscribe();
      })
    );
  }
}