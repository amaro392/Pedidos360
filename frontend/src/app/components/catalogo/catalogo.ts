import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductoService, ProductoDTO } from '../../services/producto';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css'
})
export class Catalogo implements OnInit {
  productos: ProductoDTO[] = [];
  cargando = true;

  constructor(
    private productoService: ProductoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.productoService.listar().subscribe({
      next: (data: ProductoDTO[]) => {
        this.productos = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error('Error al cargar productos:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }
}