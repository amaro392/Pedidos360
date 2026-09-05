import { Injectable } from '@angular/core';

export interface CartItem {
  producto: any;
  cantidad: number;
}

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private items: CartItem[] = [];

  getItems(): CartItem[] {
    return this.items;
  }

  agregar(producto: any): void {
    const itemExistente = this.items.find(i => i.producto.id === producto.id);
    if (itemExistente) {
      itemExistente.cantidad++;
    } else {
      this.items.push({ producto, cantidad: 1 });
    }
  }

  decrementar(id: number): void {
    const item = this.items.find(i => i.producto.id === id);
    if (item) {
      item.cantidad--;
      if (item.cantidad <= 0) {
        this.eliminar(id);
      }
    }
  }

  eliminar(id: number): void {
    this.items = this.items.filter(i => i.producto.id !== id);
  }

  vaciar(): void {
    this.items = [];
  }

  totalItems(): number {
    return this.items.reduce((acc, item) => acc + item.cantidad, 0);
  }

  totalPrecio(): number {
    return this.items.reduce((acc, item) => acc + (item.producto.precio * item.cantidad), 0);
  }
}