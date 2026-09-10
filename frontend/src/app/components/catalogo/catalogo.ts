import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { CarritoService } from '../../services/carrito';
import { ProductoService } from '../../services/producto';
import { PedidoService, PedidoDTO } from '../../services/pedido';
import { NotificacionService, NotificacionDTO } from '../../services/notificacion';

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
    private notificacionService: NotificacionService,
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
        this.notificarPedidoCreado(pedidoCreado, clienteEmail);
        alert('¡Pedido realizado con éxito!');
        this.carritoService.vaciar();
        this.mostrarCarrito = false;
        this.router.navigate(['/pedidos']);
      },
      error: (err: any) => console.error('Error al guardar pedido:', err)
    });
  }

  private notificarPedidoCreado(pedido: any, clienteEmail: string): void {
    const hora = new Date().toLocaleString('es-CL', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });

    const detalleProductos = (pedido?.items ?? []).length
      ? pedido.items.map((i: any) => `${i.cantidad}x ${i.nombreProducto}`).join(', ')
      : 'sin detalle de productos';

    const totalFormateado = new Intl.NumberFormat('es-CL', {
      style: 'currency', currency: 'CLP'
    }).format(pedido?.total ?? 0);

    const notificacion: NotificacionDTO = {
      destinatarioEmail: clienteEmail,
      asunto: `Pedido #${pedido?.id ?? ''} recibido`,
      mensaje: `Tu pedido #${pedido?.id ?? ''} fue recibido el ${hora}. `
        + `Total: ${totalFormateado}. Productos: ${detalleProductos}.`,
      tipo: 'PEDIDO_CREADO',
      pedidoId: pedido?.id
    };

    this.notificacionService.crear(notificacion).subscribe({
      error: (err: any) => console.error('No se pudo registrar la notificacion:', err)
    });
  }
}
