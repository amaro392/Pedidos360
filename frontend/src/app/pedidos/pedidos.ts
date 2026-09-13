import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { PedidoService, PedidoDTO } from '../services/pedido';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css'
})
export class Pedidos implements OnInit {
  pedidos: PedidoDTO[] = [];
  cargando = true;

  constructor(
    private pedidoService: PedidoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.pedidoService.listar().subscribe({
      next: (data: PedidoDTO[]) => {
        this.pedidos = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
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
