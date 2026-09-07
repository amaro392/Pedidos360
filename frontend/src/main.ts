import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalInterceptor,
  MsalGuard,
  MsalService,
  MsalBroadcastService
} from '@azure/msal-angular';
import { InteractionType, PublicClientApplication } from '@azure/msal-browser';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { msalInstanceFactory } from './app/msal-config';
import { msalInterceptorConfigFactory } from './app/msal-interceptor-config';

const msalInstance = msalInstanceFactory() as PublicClientApplication;

msalInstance.initialize().then(() => {
  bootstrapApplication(AppComponent, {
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptorsFromDi()),

      { provide: MSAL_INSTANCE, useValue: msalInstance },
      { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
      {
        provide: MSAL_GUARD_CONFIG,
        useValue: { interactionType: InteractionType.Redirect }
      },
      { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },

      MsalGuard,
      MsalService,
      MsalBroadcastService
    ]
  }).catch(err => console.error(err));
}).catch(err => console.error('Error al inicializar MSAL:', err));