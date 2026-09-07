import { Component } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  constructor(private msal: MsalService) {}

  getUserName(): string {
    const accounts = this.msal.instance.getAllAccounts();
    return accounts.length > 0 ? accounts[0].name || accounts[0].username : '';
  }

  logout(): void {
    this.msal.logoutRedirect();
  }
}