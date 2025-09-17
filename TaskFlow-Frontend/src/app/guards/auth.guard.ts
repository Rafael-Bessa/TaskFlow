import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    console.log('AuthGuard: Verificando autenticação...');
    
    if (this.authService.isAuthenticated()) {
      console.log('AuthGuard: Usuário autenticado, liberando acesso');
      return true;
    }
    
    console.log('AuthGuard: Usuário não autenticado, redirecionando para login');
    this.router.navigate(['/login']);
    return false;
  }
}