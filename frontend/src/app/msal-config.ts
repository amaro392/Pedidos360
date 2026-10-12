import { PublicClientApplication, IPublicClientApplication } from '@azure/msal-browser';
import { environment } from '../environments/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: environment.azure.authority,
      redirectUri: window.location.origin + '/'
    },
    cache: {
      cacheLocation: 'localStorage'
    }
  });
}
