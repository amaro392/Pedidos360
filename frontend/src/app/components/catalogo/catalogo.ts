import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CarritoService } from '../../services/carrito';

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

  private productosUrl = 'http://localhost:8082/api/productos';
  private pedidosUrl = 'http://localhost:8081/api/pedidos';
  private notificacionesUrl = 'http://localhost:8083/api/notificaciones';

  constructor(
    public carritoService: CarritoService,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.http.get<any[]>(this.productosUrl).subscribe({
      next: (data) => {
        this.productos = data;
        this.cargando = false;
        this.cdr.detectChanges(); // Forzar el renderizado inmediato en pantalla
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Obtiene el stock real restando la cantidad agregada al carrito
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

    const nuevoPedido = {
      clienteEmail: 'cliente@duocuc.cl',
      estado: 'COMPLETADO',
      total: this.carritoService.totalPrecio(),
      items: this.itemsCarrito.map((item: any) => ({
        nombreProducto: item.producto.nombre,
        imagenUrl: item.producto.imagenUrl || item.producto.imagen,
        precioUnitario: item.producto.precio,
        cantidad: item.cantidad
      }))
    };

    this.http.post<any>(this.pedidosUrl, nuevoPedido).subscribe({
      next: (pedidoCreado) => {
        this.notificarPedidoCreado(pedidoCreado, nuevoPedido.clienteEmail);
        alert('¡Pedido realizado con éxito!');
        this.carritoService.vaciar();
        this.mostrarCarrito = false;
        this.router.navigate(['/pedidos']);
      },
      error: (err) => console.error('Error al guardar pedido:', err)
    });
  }

  // Deja constancia en ms-notificaciones. Si falla, no bloquea la compra:
  // el pedido ya quedo guardado, esto es solo el aviso.
  private notificarPedidoCreado(pedido: any, clienteEmail: string): void {
    const notificacion = {
      destinatarioEmail: clienteEmail,
      asunto: 'Pedido recibido',
      mensaje: `Tu pedido #${pedido?.id ?? ''} fue recibido y esta siendo procesado.`,
      tipo: 'PEDIDO_CREADO',
      pedidoId: pedido?.id
    };

    this.http.post(this.notificacionesUrl, notificacion).subscribe({
      error: (err) => console.error('No se pudo registrar la notificacion:', err)
    });
  }
}