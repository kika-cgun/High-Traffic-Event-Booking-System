import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'events', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'events',
    loadComponent: () =>
      import('./features/events/event-list/event-list.component').then(m => m.EventListComponent),
  },
  {
    path: 'events/:id',
    loadComponent: () =>
      import('./features/events/event-detail/event-detail.component').then(
        m => m.EventDetailComponent
      ),
  },
  {
    path: 'checkout/:ticketId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/checkout/checkout.component').then(m => m.CheckoutComponent),
  },
  {
    path: 'tickets',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/tickets/ticket-list/ticket-list.component').then(
        m => m.TicketListComponent
      ),
  },
  { path: '**', redirectTo: 'events' },
];
