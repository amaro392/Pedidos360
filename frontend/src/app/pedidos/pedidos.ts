import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pedidos.html'
})
export class PedidosComponent implements OnInit {
  pedidos = signal<any[]>([]);
  roles = signal<string[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  constructor(
    private http: HttpClient,
    private msal: MsalService
  ) {}

  ngOnInit(): void {
    this.leerRolesDesdeToken();
    this.cargarPedidos();
  }

  private leerRolesDesdeToken(): void {
    const account = this.msal.instance.getActiveAccount()
      ?? this.msal.instance.getAllAccounts()[0];
    const claims = account?.idTokenClaims as { roles?: string[] } | undefined;
    this.roles.set(claims?.roles ?? []);
  }

  private cargarPedidos(): void {
    // El MsalInterceptor adjunta el JWT automaticamente porque esta URL
    // matchea el protectedResourceMap definido en msal-interceptor-config.ts
    this.http.get<any[]>(`${environment.apiBaseUrl}/pedidos`).subscribe({
      next: (data) => {
        this.pedidos.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo conectar con el backend (' + err.status + ')');
        this.cargando.set(false);
      }
    });
  }
}
