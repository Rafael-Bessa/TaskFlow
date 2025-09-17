import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login {
  email: string = '';
  password: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';

  private router = inject(Router);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  onSubmit(): void {
    this.errorMessage = '';
    this.cdr.markForCheck();

    if (!this.email || !this.password) {
      this.errorMessage = 'Por favor, preencha todos os campos';
      this.cdr.markForCheck();
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    const loginData = { email: this.email, password: this.password };

    this.authService.login(loginData).subscribe({
      next: () => {
        console.log('Login realizado com sucesso');
        this.router.navigate(['/tasks']);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Erro no login:', error);

        if (error.status === 401) this.errorMessage = 'Email ou senha incorretos';
        else if (error.status === 0) this.errorMessage = 'Erro de conexão. Verifique se o servidor está rodando';
        else this.errorMessage = 'Erro interno do servidor. Tente novamente';

        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  goToRegister(): void {
    console.log('🔥 Navegando para registro...');
    this.router.navigate(['/register']);
  }
}