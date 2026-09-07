import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { NotificacionService, NotificacionDTO } from '../../services/notificacion';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.css'
})
export class Notificaciones implements OnInit {
  notificaciones: NotificacionDTO[] = [];
  cargando = true;
  error = false;

  constructor(
    private notificacionService: NotificacionService,
    private msal: MsalService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarNotificaciones();
  }

  cargarNotificaciones(): void {
    const cuenta = this.msal.instance.getActiveAccount();
    const email = cuenta?.username;

    this.cargando = true;
    this.error = false;

    const peticion = email
      ? this.notificacionService.porDestinatario(email)
      : this.notificacionService.listar();

    peticion.subscribe({
      next: (data) => {
        this.notificaciones = data.sort((a, b) =>
          (b.fechaEnvio || '').localeCompare(a.fechaEnvio || '')
        );
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cargar notificaciones:', err);
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  iconoPorTipo(tipo: string): string {
    switch (tipo) {
      case 'PEDIDO_CREADO': return '🛒';
      case 'PEDIDO_ENVIADO': return '📦';
      case 'PEDIDO_CANCELADO': return '❌';
      default: return '🔔';
    }
  }
}
