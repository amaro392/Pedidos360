import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  MsalService,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalGuardConfiguration
} from '@azure/msal-angular';
import { InteractionType } from '@azure/msal-browser';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { msalInstanceFactory } from './msal-config';
import { msalInterceptorConfigFactory } from './msal-interceptor-config';

function msalGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.azure.protectedResourceScopes
    }
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: MSAL_INSTANCE,
      useFactory: msalInstanceFactory
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: msalGuardConfigFactory
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: msalInterceptorConfigFactory
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },
    MsalService,
    MsalGuard,
    MsalBroadcastService
  ]
};