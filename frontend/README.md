# Pedidos360 - Frontend

Aplicación Angular con login mediante Azure AD (MSAL). El JWT obtenido se adjunta automáticamente
a las llamadas hacia las APIs (ver `src/app/msal-interceptor-config.ts`).

## Ejecutar

```bash
npm install
npm start        # http://localhost:4200
```

## Configuración

Los datos de Azure AD y las URLs de las APIs están en `src/environments/environment.ts`.

## Vistas

Home, Catálogo, Pedidos, Notificaciones y Perfil (todas protegidas con `MsalGuard`).

Documentación general del sistema en el README de la raíz del repositorio.
