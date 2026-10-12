import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();
  const scopes = environment.azure.protectedResourceScopes;

  // API Gateway (AWS)
  protectedResourceMap.set('https://uqzu7hn0h2.execute-api.us-east-1.amazonaws.com/prod/*', scopes);

  // Microservicios locales
  protectedResourceMap.set('http://localhost:8081/api/*', scopes);
  protectedResourceMap.set('http://localhost:8082/api/*', scopes);
  protectedResourceMap.set('http://localhost:8083/api/*', scopes);
  protectedResourceMap.set('http://localhost:8084/api/*', scopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}