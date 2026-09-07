import { Component } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { RouterOutlet } from '@angular/router';
import { InteractionStatus } from '@azure/msal-browser';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  constructor(private msal: MsalService) {}

  isLoggedIn(): boolean {
    return this.msal.instance.getAllAccounts().length > 0;
  }

  getUserName(): string {
    const accounts = this.msal.instance.getAllAccounts();
    return accounts.length > 0 ? accounts[0].name || accounts[0].username : '';
  }

  login(): void {
    if (this.isLoggedIn()) {
      return;
    }
    this.msal.loginRedirect();
  }

  logout(): void {
    this.msal.logoutRedirect();
  }
}
