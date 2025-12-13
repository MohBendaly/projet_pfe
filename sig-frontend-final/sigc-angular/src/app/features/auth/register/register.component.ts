import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserDTO } from '../models/userDTO.model';
// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
//import { Message } from 'primeng/api';
import { MessageService } from 'primeng/api'; // Pour afficher un toast de succès

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MatIconModule,
    MessagesModule,
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  constructor(
     private snackBar: MatSnackBar
  ){}
  // Pour les toasts

  registerForm!: FormGroup;
  isLoading = false;
  errorMessages: any[] = [];
  error:string | null =null;


  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }

    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
      firstName: ['', Validators.maxLength(100)],
      lastName: ['', Validators.maxLength(100)]
    });
  }

  get f() { return this.registerForm.controls; }

 onSubmit(): void {
  this.isLoading = true;

  this.authService.register(this.registerForm.value).pipe(
    finalize(() => this.isLoading = false)
  ).subscribe({
    next: (res) => {
      console.log('Réponse complète du serveur:', res);
      this.router.navigate(['/login']);
    },
    error: (err) => {
      console.error('Détails complets de l\'erreur:', {
        status: err.status,
        url: err.url,
        serverError: err.error,  // Contient le message du serveur
        headers: err.headers
      });

      let errorMsg = 'Erreur lors de l\'inscription';
      if (err.error?.message) {
        errorMsg = err.error.message;
      } else if (err.status === 500) {
        errorMsg = 'Problème serveur - contactez l\'administrateur';
      }

      this.snackBar.open(errorMsg, 'OK', { duration: 5000 });
    }
  });
}
}