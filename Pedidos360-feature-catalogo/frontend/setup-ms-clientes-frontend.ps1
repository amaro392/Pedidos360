# Script para agregar el modulo 'Mi Perfil' (ms-clientes) al frontend Angular
$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
Write-Host "Creando carpetas..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "src\app" | Out-Null
New-Item -ItemType Directory -Force -Path "src\app\components\home" | Out-Null
New-Item -ItemType Directory -Force -Path "src\app\components\perfil" | Out-Null
New-Item -ItemType Directory -Force -Path "src\app\services" | Out-Null
New-Item -ItemType Directory -Force -Path "src\environments" | Out-Null
Write-Host "Escribiendo archivos (sin BOM)..." -ForegroundColor Cyan
$content = @'
export const environment = {
  production: false,

  azure: {
    clientId: '9078dcc3-9503-4237-a463-d5a4a96cb61f',
    tenantId: 'abf8edad-bd14-425d-9255-2e0e7e57dfa2',
    authority: 'https://login.microsoftonline.com/abf8edad-bd14-425d-9255-2e0e7e57dfa2',
    redirectUri: 'http://localhost:4200',
    protectedResourceScopes: ['api://9078dcc3-9503-4237-a463-d5a4a96cb61f/access_as_user']
  },

  apiBaseUrl: 'http://localhost:8082/api',
  pedidosApiUrl: 'http://localhost:8081/api',
  notificacionesApiUrl: 'http://localhost:8083/api',
  clientesApiUrl: 'http://localhost:8084/api'
};

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\environments\environment.ts"), $content, $utf8NoBom)
Write-Host "  OK: src\environments\environment.ts"

$content = @'
import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>([
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes],
    [`${environment.pedidosApiUrl}/*`, environment.azure.protectedResourceScopes],
    [`${environment.notificacionesApiUrl}/*`, environment.azure.protectedResourceScopes],
    [`${environment.clientesApiUrl}/*`, environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\msal-interceptor-config.ts"), $content, $utf8NoBom)
Write-Host "  OK: src\app\msal-interceptor-config.ts"

$content = @'
import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { Home } from './components/home/home';
import { Catalogo } from './components/catalogo/catalogo';
import { Pedidos } from './pedidos/pedidos';
import { Notificaciones } from './components/notificaciones/notificaciones';
import { Perfil } from './components/perfil/perfil';

export const routes: Routes = [
  { path: 'home', component: Home, canActivate: [MsalGuard] },
  { path: 'catalogo', component: Catalogo, canActivate: [MsalGuard] },
  { path: 'pedidos', component: Pedidos, canActivate: [MsalGuard] },
  { path: 'notificaciones', component: Notificaciones, canActivate: [MsalGuard] },
  { path: 'perfil', component: Perfil, canActivate: [MsalGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'home' }
];

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\app.routes.ts"), $content, $utf8NoBom)
Write-Host "  OK: src\app\app.routes.ts"

$content = @'
<div class="layout">
  <aside class="sidebar">
    <div class="sidebar-header">
      <span class="logo">🎮</span>
      <h2>GamerStore</h2>
    </div>

    <nav class="sidebar-nav">
      <a routerLink="/catalogo" routerLinkActive="active" class="nav-item">
        <span class="nav-icon">🛒</span> Ver Catálogo
      </a>
      <a routerLink="/pedidos" routerLinkActive="active" class="nav-item">
        <span class="nav-icon">📦</span> Mis Pedidos
      </a>
      <a routerLink="/notificaciones" routerLinkActive="active" class="nav-item">
        <span class="nav-icon">🔔</span> Notificaciones
      </a>
      <a routerLink="/perfil" routerLinkActive="active" class="nav-item">
        <span class="nav-icon">👤</span> Mi Perfil
      </a>
    </nav>

    <div class="sidebar-footer">
      <p>{{ getUserName() }}</p>
      <button class="logout-btn" (click)="logout()">Cerrar sesión</button>
    </div>
  </aside>

  <main class="content">
    <h1>Bienvenido, {{ getUserName() }}</h1>
    <p class="subtitle">Selecciona una opción del menú para comenzar.</p>
  </main>
</div>

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\components\home\home.html"), $content, $utf8NoBom)
Write-Host "  OK: src\app\components\home\home.html"

$content = @'
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ClienteDTO {
  id?: number;
  nombre: string;
  email: string;
  telefono: string;
  direccion: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private apiUrl = environment.clientesApiUrl + '/clientes';

  constructor(private http: HttpClient) {}

  listar(): Observable<ClienteDTO[]> {
    return this.http.get<ClienteDTO[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<ClienteDTO> {
    return this.http.get<ClienteDTO>(`${this.apiUrl}/${id}`);
  }

  buscarPorEmail(email: string): Observable<ClienteDTO> {
    return this.http.get<ClienteDTO>(`${this.apiUrl}/email/${email}`);
  }

  crear(cliente: ClienteDTO): Observable<ClienteDTO> {
    return this.http.post<ClienteDTO>(this.apiUrl, cliente);
  }

  actualizar(id: number, cliente: ClienteDTO): Observable<ClienteDTO> {
    return this.http.put<ClienteDTO>(`${this.apiUrl}/${id}`, cliente);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\services\cliente.ts"), $content, $utf8NoBom)
Write-Host "  OK: src\app\services\cliente.ts"

$content = @'
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { ClienteService, ClienteDTO } from '../../services/cliente';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css'
})
export class Perfil implements OnInit {
  cliente: ClienteDTO = { nombre: '', email: '', telefono: '', direccion: '' };
  clienteId: number | null = null;

  cargando = true;
  guardando = false;
  esNuevo = false;
  error = false;
  mensajeExito = '';

  constructor(
    private clienteService: ClienteService,
    private msal: MsalService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarPerfil();
  }

  cargarPerfil(): void {
    const cuenta = this.msal.instance.getActiveAccount();
    const email = cuenta?.username || '';
    const nombreCuenta = cuenta?.name || '';

    this.cargando = true;
    this.error = false;

    this.clienteService.buscarPorEmail(email).subscribe({
      next: (data) => {
        this.cliente = data;
        this.clienteId = data.id ?? null;
        this.esNuevo = false;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (err.status === 404) {
          // Aún no existe un perfil para este usuario: precargamos el formulario de creación
          this.cliente = { nombre: nombreCuenta, email: email, telefono: '', direccion: '' };
          this.esNuevo = true;
          this.cargando = false;
        } else {
          console.error('Error al cargar el perfil:', err);
          this.error = true;
          this.cargando = false;
        }
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    this.guardando = true;
    this.mensajeExito = '';

    const peticion = this.esNuevo
      ? this.clienteService.crear(this.cliente)
      : this.clienteService.actualizar(this.clienteId!, this.cliente);

    peticion.subscribe({
      next: (data) => {
        this.cliente = data;
        this.clienteId = data.id ?? null;
        this.esNuevo = false;
        this.guardando = false;
        this.mensajeExito = '¡Perfil guardado con éxito!';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al guardar el perfil:', err);
        this.guardando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\components\perfil\perfil.ts"), $content, $utf8NoBom)
Write-Host "  OK: src\app\components\perfil\perfil.ts"

$content = @'
<div class="perfil-container">
  <div class="perfil-header">
    <h1>👤 Mi Perfil</h1>
    <p class="subtitle" *ngIf="esNuevo && !cargando">
      Aún no tienes datos de perfil guardados. Complétalos para agilizar tus próximos pedidos.
    </p>
  </div>

  <div *ngIf="cargando" class="estado-carga">
    Cargando perfil...
  </div>

  <div *ngIf="!cargando" class="perfil-card">
    <form (ngSubmit)="guardar()" #perfilForm="ngForm">
      <div class="campo">
        <label for="nombre">Nombre completo</label>
        <input
          id="nombre"
          name="nombre"
          type="text"
          [(ngModel)]="cliente.nombre"
          required
          placeholder="Tu nombre completo" />
      </div>

      <div class="campo">
        <label for="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          [(ngModel)]="cliente.email"
          required
          [readonly]="!esNuevo"
          placeholder="tu@email.com" />
      </div>

      <div class="campo">
        <label for="telefono">Teléfono</label>
        <input
          id="telefono"
          name="telefono"
          type="tel"
          [(ngModel)]="cliente.telefono"
          required
          placeholder="+56 9 1234 5678" />
      </div>

      <div class="campo">
        <label for="direccion">Dirección</label>
        <input
          id="direccion"
          name="direccion"
          type="text"
          [(ngModel)]="cliente.direccion"
          required
          placeholder="Calle, número, comuna" />
      </div>

      <div class="mensaje-exito" *ngIf="mensajeExito">
        {{ mensajeExito }}
      </div>

      <div class="mensaje-error" *ngIf="error">
        Ocurrió un error al comunicarse con el servidor. Intenta nuevamente.
      </div>

      <button type="submit" class="btn-guardar" [disabled]="guardando || perfilForm.invalid">
        {{ guardando ? 'Guardando...' : (esNuevo ? 'Crear perfil' : 'Guardar cambios') }}
      </button>
    </form>
  </div>
</div>

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\components\perfil\perfil.html"), $content, $utf8NoBom)
Write-Host "  OK: src\app\components\perfil\perfil.html"

$content = @'
.perfil-container {
  padding: 3rem;
  color: white;
  max-width: 600px;
}

.perfil-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
}

.subtitle {
  color: #8892b0;
  margin-bottom: 1.5rem;
}

.estado-carga {
  color: #8892b0;
  padding: 2rem 0;
}

.perfil-card {
  background: #16213e;
  border-radius: 14px;
  padding: 2rem;
  border: 1px solid #1f2b4d;
}

.campo {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 1.2rem;
}

.campo label {
  font-size: 0.85rem;
  color: #8892b0;
  font-weight: 600;
}

.campo input {
  background: #0d1b2a;
  border: 1px solid #1f2b4d;
  border-radius: 8px;
  padding: 0.7rem 0.9rem;
  color: white;
  font-size: 1rem;
}

.campo input:focus {
  outline: none;
  border-color: #0fbcf9;
}

.campo input[readonly] {
  color: #8892b0;
  cursor: not-allowed;
}

.mensaje-exito {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  padding: 0.7rem 1rem;
  border-radius: 8px;
  margin-bottom: 1.2rem;
  font-size: 0.9rem;
}

.mensaje-error {
  background: rgba(233, 69, 96, 0.15);
  color: #e94560;
  padding: 0.7rem 1rem;
  border-radius: 8px;
  margin-bottom: 1.2rem;
  font-size: 0.9rem;
}

.btn-guardar {
  width: 100%;
  background: #0fbcf9;
  color: #0d1b2a;
  border: none;
  padding: 0.8rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-guardar:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-guardar:not(:disabled):hover {
  opacity: 0.9;
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\app\components\perfil\perfil.css"), $content, $utf8NoBom)
Write-Host "  OK: src\app\components\perfil\perfil.css"

Write-Host ""
Write-Host "Listo. Archivos creados/actualizados:" -ForegroundColor Green
Write-Host "  src\environments\environment.ts"
Write-Host "  src\app\msal-interceptor-config.ts"
Write-Host "  src\app\app.routes.ts"
Write-Host "  src\app\components\home\home.html"
Write-Host "  src\app\services\cliente.ts"
Write-Host "  src\app\components\perfil\perfil.ts"
Write-Host "  src\app\components\perfil\perfil.html"
Write-Host "  src\app\components\perfil\perfil.css"
Write-Host ""
Write-Host "Ahora corre: npm start  (o ng serve)" -ForegroundColor Yellow