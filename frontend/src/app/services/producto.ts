import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ProductoDTO {
  id?: number;
  nombre: string;
  precio: number;
  stock: number;
  categoria: string;
  imagen?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private apiUrl = ${environment.apiBaseUrl}/productos;

  constructor(private http: HttpClient) {}

  listar(): Observable<ProductoDTO[]> {
    return this.http.get<ProductoDTO[]>(this.apiUrl);
  }

  crear(producto: ProductoDTO): Observable<ProductoDTO> {
    return this.http.post<ProductoDTO>(this.apiUrl, producto);
  }
}
