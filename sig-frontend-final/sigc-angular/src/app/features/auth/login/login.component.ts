import { Component, inject, OnInit,OnDestroy} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router'; // Importer ActivatedRoute pour returnUrl
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'; // Pour les formulaires réactifs
import {MatInputModule} from '@angular/material/input';
import {MatFormFieldModule} from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Subject } from 'rxjs';
import { finalize,takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
// PrimeNG Modules
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages'; // Pour afficher les erreurs PrimeNG
//import { Message } from 'primeng/api'; // Interface pour les messages
import {FormsModule} from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'my-login-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    CardModule,
    MatCardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessagesModule,
    FormsModule,
    MatFormFieldModule, // For <mat-card>
    MatInputModule, // For <input matInput>
    MatButtonModule,
    MatInputModule,
  
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  // Injection de dépendances
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
// Pour obtenir l'URL de retour

  loginForm!: FormGroup; // Le ! indique qu'elle sera initialisée dans ngOnInit
  isLoading = false;
  errorMessages: any[] = []; // Pour les messages d'erreur PrimeNG
  returnUrl: string = '/'; 
  private destroy$ = new Subject<void>();
    constructor(

    private snackBar: MatSnackBar // Injection du service SnackBar
  ) {}// URL de redirection après succès

  ngOnInit(): void {
    // Rediriger si déjà connecté
    if (this.authService.isLoggedIn()) {
       this.router.navigate(['/']); // Ou vers le dashboard approprié
    }

    // Récupérer l'URL de retour depuis les query params
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';

    // Initialiser le formulaire
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get f() { return this.loginForm.controls; }
  
  // Accès facile aux contrôles dans le template

// login.component.ts
onSubmit(): void {
  this.errorMessages = [];
  
  if (this.loginForm.invalid) {
    this.markAllAsTouched();
    return;
  }

  this.isLoading = true;

  this.authService.login(this.loginForm.value)
    .pipe(
      finalize(() => this.isLoading = false),
      takeUntil(this.destroy$)
    )
    .subscribe({
      next: (success) => {
        if (success) {
          setTimeout(() => {
            this.router.navigate(['/candidate/profile']);
          }, 100);
        }
      },
      error: (err) => {
        this.errorMessages = [err.message]; // Affiche le message d'erreur
        console.error('Login failed:', err);
      }
    });
}
 

ngOnDestroy() {
  this.destroy$.next();
  this.destroy$.complete();
}
get formControls() {
    return this.loginForm.controls;
  }
  private markAllAsTouched(): void {
    Object.values(this.loginForm.controls).forEach(control => {
      control.markAsTouched();
    });
  }

 private handleLoginError(err: HttpErrorResponse): void {
  let detail: string;
  
  if (err.status === 0) {
    detail = 'Serveur injoignable - Vérifiez votre connexion';
  } else if (err.status === 401) {
    detail = 'Email/mot de passe incorrect';
  } else {
    detail = err.error?.message || 'Erreur technique';
  }

  this.errorMessages = [{
    severity: 'error',
    summary: 'Connexion échouée',
    detail: detail,
    life: 5000
  }];
  
  console.error('Erreur technique:', {
    status: err.status,
    url: err.url,
    error: err.error
  });
}
}