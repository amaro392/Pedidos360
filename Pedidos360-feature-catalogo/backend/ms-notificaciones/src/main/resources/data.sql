INSERT INTO notificaciones (destinatario_email, asunto, mensaje, tipo, estado, pedido_id, fecha_envio) VALUES
('cliente@duocuc.cl', 'Pedido recibido', 'Tu pedido #1 fue recibido y esta siendo procesado.', 'PEDIDO_CREADO', 'ENVIADA', 1, CURRENT_TIMESTAMP),
('cliente@duocuc.cl', 'Pedido enviado', 'Tu pedido #1 fue despachado.', 'PEDIDO_ENVIADO', 'ENVIADA', 1, CURRENT_TIMESTAMP);
