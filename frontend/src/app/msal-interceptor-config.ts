import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();
  const scopes = environment.azure.protectedResourceScopes;

  // Protege la raíz del Stage /prod para inyectar el Bearer Token en todas las subrutas
  protectedResourceMap.set('https://csd8f2cegk.execute-api.us-east-1.amazonaws.com/prod/*', scopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}