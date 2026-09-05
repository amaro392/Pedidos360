import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent implements OnInit {
  title = 'frontend';

  constructor(
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService
  ) {}

  ngOnInit(): void {
    // Procesa la redirección al volver del portal de Microsoft
    this.authService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result) {
          this.authService.instance.setActiveAccount(result.account);
        }
      },
      error: (err) => console.error('Error en redirección MSAL:', err)
    });

    this.msalBroadcastService.msalSubject$
      .pipe(filter((msg: any) => msg.interactionStatus === InteractionStatus.None))
      .subscribe(() => {
        this.checkAndSetActiveAccount();
      });
  }

  checkAndSetActiveAccount(): void {
    const activeAccount = this.authService.instance.getActiveAccount();
    if (!activeAccount && this.authService.instance.getAllAccounts().length > 0) {
      this.authService.instance.setActiveAccount(this.authService.instance.getAllAccounts()[0]);
    }
  }

  isLoggedIn(): boolean {
    return this.authService.instance.getActiveAccount() !== null;
  }

  login(): void {
    // Forzado directo del flujo de login redirigido
    this.authService.loginRedirect();
  }

  logout(): void {
    this.authService.logoutRedirect();
  }
}