import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { Home } from './components/home/home';
import { Catalogo } from './components/catalogo/catalogo';
import { Pedidos } from './pedidos/pedidos';

export const routes: Routes = [
  { path: 'home', component: Home, canActivate: [MsalGuard] },
  { path: 'catalogo', component: Catalogo, canActivate: [MsalGuard] },
  { path: 'pedidos', component: Pedidos, canActivate: [MsalGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'home' }
];