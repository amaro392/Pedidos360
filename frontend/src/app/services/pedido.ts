import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PedidoDTO {
  id?: number;
  clienteEmail: string;
  estado: string;
  total: number;
  items: any[];
  fechaCreacion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private apiUrl = environment.pedidosApiUrl + '/pedidos';

  constructor(private http: HttpClient) {}

  listar(): Observable<PedidoDTO[]> {
    return this.http.get<PedidoDTO[]>(this.apiUrl);
  }

  crear(pedido: PedidoDTO): Observable<PedidoDTO> {
    return this.http.post<PedidoDTO>(this.apiUrl, pedido);
  }
}
