import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  age: number;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: number;
    fullName: string;
    email: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';
  
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient, 
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.checkStoredToken();
  }

  // ADICIONADO: Verifica se está executando no browser
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  // ADICIONADO: Método seguro para acessar localStorage
  private getFromStorage(key: string): string | null {
    if (this.isBrowser() && typeof Storage !== 'undefined') {
      try {
        return localStorage.getItem(key);
      } catch (error) {
        console.warn('Erro ao acessar localStorage:', error);
        return null;
      }
    }
    return null;
  }

  // ADICIONADO: Método seguro para salvar no localStorage
  private setInStorage(key: string, value: string): void {
    if (this.isBrowser() && typeof Storage !== 'undefined') {
      try {
        localStorage.setItem(key, value);
      } catch (error) {
        console.warn('Erro ao salvar no localStorage:', error);
      }
    }
  }

  // ADICIONADO: Método seguro para remover do localStorage
  private removeFromStorage(key: string): void {
    if (this.isBrowser() && typeof Storage !== 'undefined') {
      try {
        localStorage.removeItem(key);
      } catch (error) {
        console.warn('Erro ao remover do localStorage:', error);
      }
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth`, credentials).pipe(
      tap((res) => {
        this.saveAuthData(res);
        this.onLoginSuccess();
      })
    );
  }

  register(userData: RegisterRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/users`, userData);
  }

  // MODIFICADO: Usa métodos seguros para localStorage
  saveAuthData(authResponse: AuthResponse): void {
    this.setInStorage('token', authResponse.token);
    this.setInStorage('user', JSON.stringify(authResponse.user));
    this.currentUserSubject.next(authResponse.user);
  }

  private onLoginSuccess(): void {
    console.log('Login bem-sucedido para:', this.getCurrentUser()?.email);
  }

  onUserChange(): Observable<any> {
    return this.currentUser$;
  }

  // MODIFICADO: Verifica se está no browser antes de acessar localStorage
  private checkStoredToken(): void {
    if (this.isBrowser()) {
      try {
        const token = this.getFromStorage('token');
        const user = this.getFromStorage('user');
        
        if (token && user) {
          this.currentUserSubject.next(JSON.parse(user));
        }
      } catch (err) {
        console.warn('Erro ao ler dados do localStorage', err);
        this.currentUserSubject.next(null);
      }
    }
  }

  // MODIFICADO: Verifica se está no browser
  isAuthenticated(): boolean {
    if (!this.isBrowser()) {
      return false; // No servidor, sempre considera não autenticado
    }
    
    const token = this.getFromStorage('token');
    return !!token;
  }

  // MODIFICADO: Usa método seguro
  getToken(): string | null {
    return this.getFromStorage('token');
  }

  // MODIFICADO: Usa método seguro
  getCurrentUser(): any {
    const user = this.getFromStorage('user');
    return user ? JSON.parse(user) : null;
  }

  getCurrentUserId(): number | null {
    const user = this.getCurrentUser();
    return user ? user.id : null;
  }

  // MODIFICADO: Usa métodos seguros para localStorage
  logout(): void {
    this.removeFromStorage('token');
    this.removeFromStorage('user');
    this.currentUserSubject.next(null);
    
    this.onLogout();
    
    if (this.isBrowser()) {
      this.router.navigate(['/login']);
    }
  }

  private onLogout(): void {
    console.log('Logout realizado');
  }
}