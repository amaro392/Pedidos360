import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface NotificacionDTO {
  id?: number;
  destinatarioEmail: string;
  asunto: string;
  mensaje: string;
  tipo: string;
  estado?: string;
  pedidoId?: number;
  fechaEnvio?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {
  private apiUrl = environment.notificacionesApiUrl + '/notificaciones';

  constructor(private http: HttpClient) {}

  listar(): Observable<NotificacionDTO[]> {
    return this.http.get<NotificacionDTO[]>(this.apiUrl);
  }

  porDestinatario(email: string): Observable<NotificacionDTO[]> {
    return this.http.get<NotificacionDTO[]>(`${this.apiUrl}/destinatario/${email}`);
  }

  crear(notificacion: NotificacionDTO): Observable<NotificacionDTO> {
    return this.http.post<NotificacionDTO>(this.apiUrl, notificacion);
  }
}
