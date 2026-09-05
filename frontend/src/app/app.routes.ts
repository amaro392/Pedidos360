import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { Catalogo } from './components/catalogo/catalogo';
import { PedidosComponent } from './pedidos/pedidos';

export const routes: Routes = [
  { path: 'catalogo', component: Catalogo, canActivate: [MsalGuard] },
  { path: 'pedidos', component: PedidosComponent, canActivate: [MsalGuard] },
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' }
];
