import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css'
})
export class Pedidos implements OnInit {
  pedidos: any[] = [];
  cargando = true;

  private urlPedidos = 'http://localhost:8081/api/pedidos';

  constructor(
    private http: HttpClient, 
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.http.get<any[]>(this.urlPedidos).subscribe({
      next: (data) => {
        this.pedidos = data;
        this.cargando = false;
        this.cdr.detectChanges(); // Fuerza renderizado de la lista
      },
      error: (err) => {
        console.error('Error al obtener pedidos:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  irAHome(): void {
    this.router.navigate(['/home']);
  }
}