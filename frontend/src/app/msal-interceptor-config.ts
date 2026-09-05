import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>([
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes],
    [`${environment.pedidosApiUrl}/*`, environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
