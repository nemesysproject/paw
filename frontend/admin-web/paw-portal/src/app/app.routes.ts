import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { PetListComponent } from './features/pets/pet-list/pet-list.component';
import { CreatePetComponent } from './features/pets/create-pet/create-pet.component';
import { EditPetComponent } from './features/pets/edit-pet/edit-pet.component';
import { MainLayoutComponent } from './shared/components/layout/main-layout/main-layout.component';
import { authGuard, publicGuard } from '@core/guards/auth.guard';

export const routes: Routes = [
  { path: 'auth/login', component: LoginComponent, canActivate: [publicGuard] },
  { path: 'auth/register', component: RegisterComponent, canActivate: [publicGuard] },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'pets', component: PetListComponent },
      { path: 'pets/create', component: CreatePetComponent },
      { path: 'pets/:id/edit', component: EditPetComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
