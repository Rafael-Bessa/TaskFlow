import { Routes } from '@angular/router';
import { Login } from './pages/login';
import { Register } from './pages/register';
import { Tasks } from './pages/tasks';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  // PROTEGIDO: Só acessa /tasks se estiver logado
  { path: 'tasks', component: Tasks, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];