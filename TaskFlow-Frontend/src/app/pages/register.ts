import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms'; // Para ngModel
import { CommonModule } from '@angular/common'; // Para *ngIf

@Component({
  selector: 'app-register',
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  standalone: true, // Adicione essa linha se não tiver
  imports: [FormsModule, CommonModule] // Adicione essa linha
})




export class Register {
  user = {
    fullName: '',
    age: 0,
    email: '',
    password: ''
  };
  
  confirmPassword: string = '';
  isLoading: boolean = false;
  showValidations: boolean = false;
  errors: any = {};

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  // Suas funções de validação de senha (mantenha as que você já tem)
  hasMinLength(): boolean {
    return this.user.password.length >= 6;
  }

  hasUppercase(): boolean {
    return /[A-Z]/.test(this.user.password);
  }

  hasLowercase(): boolean {
    return /[a-z]/.test(this.user.password);
  }

  hasNumber(): boolean {
    return /\d/.test(this.user.password);
  }

  hasSpecialChar(): boolean {
    return /[@$!%*?&]/.test(this.user.password);
  }

  getPasswordStrength(): string {
    const conditions = [
      this.hasMinLength(),
      this.hasUppercase(),
      this.hasLowercase(),
      this.hasNumber(),
      this.hasSpecialChar()
    ];
    
    const score = conditions.filter(c => c).length;
    
    if (score < 2) return 'Fraca';
    if (score < 4) return 'Média';
    return 'Forte';
  }

  getPasswordStrengthClass(): string {
    const strength = this.getPasswordStrength();
    return strength.toLowerCase();
  }

  onFieldChange(fieldName: string): void {
    // Limpa o erro do campo quando o usuário começa a digitar
    if (this.errors[fieldName]) {
      delete this.errors[fieldName];
    }
  }

  // Validações antes de enviar
  private validateForm(): boolean {
    this.errors = {};
    
    if (!this.user.fullName.trim()) {
      this.errors.fullName = 'Nome completo é obrigatório';
    }
    
    if (!this.user.age || this.user.age < 1 || this.user.age > 120) {
      this.errors.age = 'Idade deve ser entre 1 e 120 anos';
    }
    
    if (!this.user.email || !this.user.email.includes('@')) {
      this.errors.email = 'Email válido é obrigatório';
    }
    
    if (!this.hasMinLength() || !this.hasUppercase() || !this.hasLowercase() || !this.hasNumber() || !this.hasSpecialChar()) {
      this.errors.password = 'A senha deve atender todos os requisitos';
    }
    
    if (this.user.password !== this.confirmPassword) {
      this.errors.confirmPassword = 'As senhas não coincidem';
    }
    
    return Object.keys(this.errors).length === 0;
  }

  // Função que executa quando envia o formulário
  onSubmit(): void {
    this.showValidations = true;
    
    // Valida o formulário
    if (!this.validateForm()) {
      return;
    }

    // Mostra loading
    this.isLoading = true;

    // Dados para enviar ao Spring Boot
    const registerData = {
      fullName: this.user.fullName,
      age: this.user.age,
      email: this.user.email,
      password: this.user.password
    };

    // Faz a requisição para o Spring Boot
this.authService.register(registerData).subscribe({
  next: (response) => {
    // Registro deu certo!
    console.log('Registro realizado com sucesso:', response);
    
    // Não chamamos saveAuthData porque não há token
    // Mostramos mensagem e redirecionamos para login
    alert('Usuário criado com sucesso! Agora faça login.');
    this.router.navigate(['/login']);
    
    this.isLoading = false;
  },
  error: (error) => {
    // Registro deu erro
    console.error('Erro no registro:', error);
    
    // Mostra mensagem de erro
    if (error.status === 400) {
      this.errors.email = 'Este email já está em uso';
    } else if (error.status === 0) {
      this.errors.email = 'Erro de conexão. Verifique se o servidor está rodando';
    } else {
      this.errors.email = 'Erro interno do servidor. Tente novamente';
    }
    
    this.isLoading = false;
  }
});

  }

  // Função para voltar ao login
  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}