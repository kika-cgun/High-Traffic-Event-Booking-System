import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    name: [''],
    surname: [''],
    address: [''],
    phone: [''],
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);

    const value = this.form.getRawValue();
    const request = Object.fromEntries(
      Object.entries(value).filter(([, v]) => v !== '')
    ) as any;

    this.authService.register(request).subscribe({
      next: () => this.router.navigate(['/events']),
      error: (err) => {
        this.errorMessage.set(
          err.error?.message ?? 'Registration failed. Try a different username or email.'
        );
        this.loading.set(false);
      },
    });
  }
}
