import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { CarritoService } from '../../services/carrito';
import { ProductoService } from '../../services/producto';
import { PedidoService, PedidoDTO } from '../../services/pedido';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css'
})
export class Catalogo implements OnInit {
  productos: any[] = [];
  cargando = true;
  mostrarCarrito = false;

  constructor(
    public carritoService: CarritoService,
    private productoService: ProductoService,
    private pedidoService: PedidoService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private msal: MsalService
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.productoService.listar().subscribe({
      next: (data: any[]) => {
        this.productos = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Error al cargar productos:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  getStockDisponible(producto: any): number {
    const itemEnCarrito = this.itemsCarrito.find((i: any) => i.producto.id === producto.id);
    const cantidadEnCarrito = itemEnCarrito ? itemEnCarrito.cantidad : 0;
    return producto.stock - cantidadEnCarrito;
  }

  toggleCarrito(): void {
    this.mostrarCarrito = !this.mostrarCarrito;
    this.cdr.detectChanges();
  }

  get itemsCarrito() {
    return this.carritoService.getItems();
  }

  irAHome(): void {
    this.router.navigate(['/home']);
  }

  procederAlPago(): void {
    if (this.itemsCarrito.length === 0) return;

    const cuenta = this.msal.instance.getActiveAccount();
    const clienteEmail = cuenta?.username || 'cliente@duocuc.cl';

    const nuevoPedido: PedidoDTO = {
      clienteEmail,
      estado: 'COMPLETADO',
      total: this.carritoService.totalPrecio(),
      items: this.itemsCarrito.map((item: any) => ({
        nombreProducto: item.producto.nombre,
        imagenUrl: item.producto.imagenUrl || item.producto.imagen,
        precioUnitario: item.producto.precio,
        cantidad: item.cantidad
      }))
    };

    this.pedidoService.crear(nuevoPedido).subscribe({
      next: (pedidoCreado: any) => {
        // La notificacion y el ticket los genera ms-notificaciones al consumir el
        // evento pedido.creado publicado por ms-pedidos en RabbitMQ.
        alert('¡Pedido realizado con éxito!');
        this.carritoService.vaciar();
        this.mostrarCarrito = false;
        this.router.navigate(['/pedidos']);
      },
      error: (err: any) => console.error('Error al guardar pedido:', err)
    });
  }
}
