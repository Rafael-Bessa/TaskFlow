import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  standalone: true,
  imports: [FormsModule, CommonModule]
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
  ageOptions: number[] = [];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    // Gera opções de idade de 13 a 100 anos
    for (let i = 13; i <= 100; i++) {
      this.ageOptions.push(i);
    }
  }

  // Funções de validação de senha
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
    if (this.errors[fieldName]) {
      delete this.errors[fieldName];
    }
  }

  private validateForm(): boolean {
    this.errors = {};
    
    if (!this.user.fullName.trim()) {
      this.errors.fullName = 'Nome completo é obrigatório';
    }
    
    if (!this.user.age || this.user.age === 0) {
      this.errors.age = 'Por favor, selecione sua idade';
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

  onSubmit(): void {
    console.log('onSubmit iniciado');
    this.showValidations = true;
    
    if (!this.validateForm()) {
      console.log('Validação falhou');
      return;
    }

    console.log('Validação passou, enviando para backend...');
    this.isLoading = true;

    const registerData = {
      fullName: this.user.fullName,
      age: this.user.age,
      email: this.user.email,
      password: this.user.password
    };

    console.log('Dados enviados:', { ...registerData, password: '[OCULTA]' });

    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('SUCCESS - Registro realizado:', response);
        this.showSuccessPopup();
        this.isLoading = false;
      },
      error: (error) => {
        console.log('ERROR - Erro capturado:', error);
        console.log('Status do erro:', error.status);
        console.log('Body do erro:', error.error);
        
        this.isLoading = false;
        this.showErrorPopup(error);
      }
    });
  }

  // MÉTODO PARA MOSTRAR POPUP DE SUCESSO
  private showSuccessPopup(): void {
    this.createPopup(
      'Sucesso!',
      'Usuário criado com sucesso! Agora faça login.',
      'success',
      () => this.router.navigate(['/login'])
    );
  }

  // MÉTODO PARA MOSTRAR POPUP DE ERRO
  private showErrorPopup(error: any): void {
    let message = '';
    let title = 'Ops! Algo deu errado';

    if (error.status === 409) {
      title = 'Email já cadastrado';
      message = error.error?.message || 'Este email já está em uso. Tente outro email.';
    } else if (error.status === 400) {
      title = 'Dados inválidos';
      if (error.error?.validationErrors) {
        const errors = Object.values(error.error.validationErrors);
        message = errors.join('\n');
      } else {
        message = error.error?.message || 'Verifique os dados e tente novamente.';
      }
    } else if (error.status === 0) {
      title = 'Erro de conexão';
      message = 'Não foi possível conectar ao servidor. Verifique sua internet.';
    } else {
      message = 'Erro interno do servidor. Tente novamente mais tarde.';
    }

    this.createPopup(title, message, 'error');
  }

  // MÉTODO PARA CRIAR POPUP DINÂMICO
  private createPopup(title: string, message: string, type: 'success' | 'error', callback?: () => void): void {
    // Remove popup anterior se existir
    const existingPopup = document.getElementById('error-popup');
    if (existingPopup) {
      existingPopup.remove();
    }

    // Cria o popup
    const popup = document.createElement('div');
    popup.id = 'error-popup';
    popup.innerHTML = `
      <div class="popup-overlay">
        <div class="popup-content ${type}">
          <div class="popup-icon">
            ${type === 'error' ? '⚠️' : '✅'}
          </div>
          <h3 class="popup-title">${title}</h3>
          <p class="popup-message">${message.replace(/\n/g, '<br>')}</p>
          <button class="popup-button" onclick="this.closePopup()">
            ${type === 'error' ? 'Entendi' : 'Continuar'}
          </button>
        </div>
      </div>
    `;

    // Adiciona estilos
    const style = document.createElement('style');
    style.textContent = `
      #error-popup .popup-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10000;
        animation: fadeIn 0.3s ease-out;
      }

      #error-popup .popup-content {
        background: white;
        padding: 2rem;
        border-radius: 16px;
        text-align: center;
        max-width: 400px;
        width: 90%;
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
        animation: slideUp 0.3s ease-out;
        position: relative;
      }

      #error-popup .popup-content.error {
        border-top: 4px solid #e74c3c;
      }

      #error-popup .popup-content.success {
        border-top: 4px solid #27ae60;
      }

      #error-popup .popup-icon {
        font-size: 3rem;
        margin-bottom: 1rem;
      }

      #error-popup .popup-title {
        font-size: 1.5rem;
        font-weight: 600;
        margin: 0 0 1rem 0;
        color: #333;
      }

      #error-popup .popup-message {
        font-size: 1rem;
        color: #666;
        line-height: 1.5;
        margin: 0 0 1.5rem 0;
      }

      #error-popup .popup-button {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        padding: 0.75rem 2rem;
        border-radius: 8px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: transform 0.2s ease;
      }

      #error-popup .popup-button:hover {
        transform: translateY(-2px);
      }

      @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
      }

      @keyframes slideUp {
        from { transform: translateY(50px); opacity: 0; }
        to { transform: translateY(0); opacity: 1; }
      }
    `;

    // Função para fechar popup
    (window as any).closePopup = () => {
      popup.remove();
      if (callback) callback();
    };

    // Adiciona ao DOM
    document.head.appendChild(style);
    document.body.appendChild(popup);

    // Auto-remove após 10 segundos
    setTimeout(() => {
      if (document.getElementById('error-popup')) {
        (window as any).closePopup();
      }
    }, 10000);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}