export const environment = {
  production: false,

  azure: {
    clientId: '31a9a52e-4eed-46bf-9dcf-452616e8610a',
    tenantId: '7c165262-9efb-4aae-85ec-c2645f733de3',
    authority: 'https://login.microsoftonline.com/7c165262-9efb-4aae-85ec-c2645f733de3',
    redirectUri: 'http://localhost:4200',
    protectedResourceScopes: ['api://31a9a52e-4eed-46bf-9dcf-452616e8610a/access_as_user']
  },

  apiBaseUrl: 'http://localhost:8082/api',
  pedidosApiUrl: 'http://localhost:8081/api',
  notificacionesApiUrl: 'http://localhost:8083/api',
  clientesApiUrl: 'http://localhost:8084/api'
};

