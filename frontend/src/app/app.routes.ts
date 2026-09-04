import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { Catalogo } from './components/catalogo/catalogo';

export const routes: Routes = [
  { path: 'catalogo', component: Catalogo, canActivate: [MsalGuard] },
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' }
];
