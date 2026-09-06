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
