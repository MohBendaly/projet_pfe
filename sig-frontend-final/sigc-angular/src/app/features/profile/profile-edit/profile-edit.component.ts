// profile-edit.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { finalize, catchError, tap, of } from 'rxjs';
import { CandidateService } from '../candidate.service';
import { Candidate, UpdateCandidateRequest } from '../candidate.model'; // Importer les modèles
import { SkillService } from '../../admin/skill.service'; // Pour les compétences
import { Skill } from '../../admin/skill.model'; // Modèle Skill

// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { MessageModule } from 'primeng/message'; 
import { MessagesModule } from 'primeng/messages';
//import { Message } from 'primeng/api';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-profile-edit',
  standalone: true,
  imports: 
  [ CommonModule, RouterModule, ReactiveFormsModule, CardModule, InputTextModule, ButtonModule, MultiSelectModule, MessagesModule,MessageModule, ProgressSpinnerModule ],
  templateUrl: './profile-edit.component.html',
  styleUrls: ['./profile-edit.component.scss'],
  
})
export class ProfileEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private candidateService = inject(CandidateService);
  private skillService = inject(SkillService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  profileForm!: FormGroup;
  isLoading = false;
  isSubmitting = false;
  errorMessages: any[] = [];
  currentProfile: Candidate | null = null;
  availableSkills: Skill[] = []; // Pour le multiselect

  ngOnInit(): void {
    this.initForm();
    this.loadAvailableSkills();
    this.loadProfileData();
  }

   initForm(profile?: Candidate): void {
    this.profileForm = this.fb.group({
      firstName: [profile?.firstName || '', [Validators.required, Validators.maxLength(100)]],
      lastName: [profile?.lastName || '', [Validators.required, Validators.maxLength(100)]],
      email: [{ value: profile?.email || '', disabled: true }], // Email non modifiable ici
      phone: [profile?.phone || '', [Validators.pattern('^\\+?[0-9.\\-\\s()]+$'), Validators.maxLength(20)]],
      skillNames: [profile?.skillNames || []] // Compétences actuelles
    });
  }

  loadProfileData(): void {
    this.isLoading = true;
    this.candidateService.getMyProfile()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (profile) => {
          this.currentProfile = profile;
          this.initForm(profile); // Pré-remplir le formulaire
        },
        error: (err) => {
          console.error("Error loading profile for edit:", err);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger le profil pour modification.' });
          this.router.navigate(['/candidate/profile']); // Rediriger si erreur
        }
      });
  }

   loadAvailableSkills(): void {
     this.skillService.findAllSkillsList().subscribe({
          next: (skills) => this.availableSkills = skills,
          error: (err) => console.error("Error loading skills", err)
     });
  }

  get f() { return this.profileForm.controls; }

  onSubmit(): void {
     this.errorMessages = [];
    if (this.profileForm.invalid || !this.currentProfile) {
      Object.values(this.profileForm.controls).forEach(control => control.markAsTouched());
      return;
    }

    this.isSubmitting = true;
    const formValue = this.profileForm.value;

    const updateRequest: UpdateCandidateRequest = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        phone: formValue.phone || undefined, // Envoyer undefined si vide
        skillNames: formValue.skillNames || [] // Envoyer les noms des compétences
    };

    this.candidateService.updateMyProfile(updateRequest) // Appeler la méthode de mise à jour
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
          next: (updatedProfile) => {
               console.log('Profile updated successfully:', updatedProfile);
               this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Profil mis à jour avec succès.' });
               // Rediriger vers la vue du profil
               this.router.navigate(['/candidate/profile']);
          },
          error: (err) => {
               console.error('Error updating profile:', err);
               let detail = `Erreur lors de la mise à jour du profil.`;
               if (err.error?.message) { detail = err.error.message; }
               else if (typeof err.error === 'string') { detail = err.error; }
               this.errorMessages = [{ severity: 'error', summary: 'Échec', detail: detail }];
          }
      });
  }

  onCancel(): void {
      this.router.navigate(['/candidate/profile']); // Retour à la vue profil
  }
}

// Ajoutez la méthode updateMyProfile(request: UpdateCandidateRequest) à CandidateService
// Elle fera un appel PUT /api/candidates/me au backend