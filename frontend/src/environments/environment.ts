export const environment = {
  production: false,
  azure: {
    clientId: '9078dcc3-9503-4237-a463-d5a4a96cb61f',
    tenantId: 'abf8edad-bd14-425d-9255-2e0e7e57dfa2',
    authority: 'https://login.microsoftonline.com/abf8edad-bd14-425d-9255-2e0e7e57dfa2',
    redirectUri: 'http://localhost:4200/',
    protectedResourceScopes: ['api://9078dcc3-9503-4237-a463-d5a4a96cb61f/access_as_user']
  },
  apiBaseUrl: 'https://uqzu7hn0h2.execute-api.us-east-1.amazonaws.com/prod',
  pedidosApiUrl: 'https://uqzu7hn0h2.execute-api.us-east-1.amazonaws.com/prod',
  notificacionesApiUrl: 'https://uqzu7hn0h2.execute-api.us-east-1.amazonaws.com/prod',
  clientesApiUrl: 'https://uqzu7hn0h2.execute-api.us-east-1.amazonaws.com/prod'
};