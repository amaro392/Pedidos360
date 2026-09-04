import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProductoDTO {
  id?: number;
  nombre: string;
  precio: number;
  stock: number;
  categoria: string;
}

@Injectable({
  providedIn: 'root'
})
export class Producto {
  private apiUrl = 'http://localhost:8082/api/productos';

  constructor(private http: HttpClient) {}

  listar(): Observable<ProductoDTO[]> {
    return this.http.get<ProductoDTO[]>(this.apiUrl);
  }

  crear(producto: ProductoDTO): Observable<ProductoDTO> {
    return this.http.post<ProductoDTO>(this.apiUrl, producto);
  }
}