import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { PedidosComponent } from './pedidos/pedidos';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'pedidos' },
  { path: 'pedidos', component: PedidosComponent, canActivate: [MsalGuard] }
];
