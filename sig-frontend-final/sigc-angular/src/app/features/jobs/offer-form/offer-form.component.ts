import { Component, inject, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormArray } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { switchMap, catchError, tap, finalize } from 'rxjs/operators';
import { JobOfferService } from '../job-offer.service';
import { JobOffer, JobOfferRequest } from '../job-offer.model'; // Importer les modèles
import { SkillService } from '../../admin/skill.service'; // Pour charger les compétences existantes
import { Skill } from '../../admin/skill.model';

// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { EditorModule } from 'primeng/editor'; // Editeur de texte riche
import { DropdownModule } from 'primeng/dropdown';
//import { ChipsModule } from 'primeng/chips'; // Importer le module Chips
import { MultiSelectModule } from 'primeng/multiselect'; // Pour sélectionner les compétences
import { CalendarModule } from 'primeng/calendar'; // Pour les dates
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
//import { Message } from 'primeng/api';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-offer-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    EditorModule,
    DropdownModule,
   // ChipsModule,
    MultiSelectModule,
    CalendarModule,
    ButtonModule,
    MessagesModule,
    ProgressSpinnerModule
  ],
  templateUrl: './offer-form.component.html',
  styleUrls: ['./offer-form.component.scss']
})
export class OfferFormComponent implements OnInit {
  @Input() id?: string; // Reçu de la route pour l'édition

  private fb = inject(FormBuilder);
  private jobOfferService = inject(JobOfferService);
  private skillService = inject(SkillService); // Pour la liste des compétences
  private router = inject(Router);
  private messageService = inject(MessageService);

  offerForm!: FormGroup;
  isEditMode = false;
  isLoading = false;
  isSubmitting = false;
  errorMessages: any[] = [];

  // Options pour les DTOs
  offerStatuses: any[] = [
    { label: 'Brouillon', value: 'DRAFT' },
    { label: 'Publiée', value: 'PUBLISHED' },
    { label: 'Fermée', value: 'CLOSED' },
    { label: 'Archivée', value: 'ARCHIVED' },
    { label: 'Pourvue', value: 'FILLED' }
  ];

  availableSkills: Skill[] = []; // Compétences chargeables pour le MultiSelect

  ngOnInit(): void {
    this.isEditMode = !!this.id;
    this.loadAvailableSkills(); // Charger les compétences disponibles
    this.initForm();

    if (this.isEditMode && this.id) {
      this.loadOfferData(parseInt(this.id, 10));
    }
  }

  initForm(offer?: JobOffer): void {
    this.offerForm = this.fb.group({
      title: [offer?.title || '', [Validators.required, Validators.maxLength(200)]],
      description: [offer?.description || '', Validators.required],
      status: [offer?.status || 'DRAFT', Validators.required],
      salaryRange: [offer?.salaryRange || '', Validators.maxLength(100)],
      // Utiliser Chips pour requirements (liste de strings)
      requirements: [offer?.requirements || []],
      // Utiliser MultiSelect pour requiredSkillNames (liste de strings)
      requiredSkillNames: [offer?.requiredSkillNames || []],
      // Utiliser Calendar pour les dates
    publicationDate: [offer?.publicationDate ? new Date(offer.publicationDate) : null],
    expirationDate: [offer?.expirationDate ? new Date(offer.expirationDate) : null]
    });
  }

  loadOfferData(offerId: number): void {
    this.isLoading = true;
    this.jobOfferService.getOfferById(offerId)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (offer) => {
          this.initForm(offer); // Pré-remplir le formulaire avec les données de l'offre
        },
        error: (err) => {
          console.error("Error loading offer for edit:", err);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger l\'offre pour modification.' });
          this.router.navigate(['/offers']); // Rediriger si erreur
        }
      });
  }

  loadAvailableSkills(): void {
     // Charger toutes les compétences (ou utiliser une recherche si la liste est trop longue)
     // Supposons une méthode dans SkillService qui retourne toutes les compétences
     this.skillService.findAllSkillsList().subscribe({ // Créez cette méthode dans SkillService
          next: (skills) => this.availableSkills = skills,
          error: (err) => console.error("Error loading skills", err)
     });
  }


  get f() { return this.offerForm.controls; }

  onSubmit(): void {
    this.errorMessages = [];
    if (this.offerForm.invalid) {
      Object.values(this.offerForm.controls).forEach(control => control.markAsTouched());
      return;
    }

    this.isSubmitting = true;
    const formValue = this.offerForm.value;

    // Préparer les données pour l'API (DTO Request)
    const offerRequest: JobOfferRequest = {
        title: formValue.title,
        description: formValue.description,
        status: formValue.status,
        salaryRange: formValue.salaryRange || null, // Envoyer null si vide
        requirements: formValue.requirements || [],
        requiredSkillNames: formValue.requiredSkillNames || [],
        // Formater les dates si elles sont présentes
        publicationDate: formValue.publicationDate ? new Date(formValue.publicationDate).toISOString() : undefined, // Remplacer null par undefined
        expirationDate: formValue.expirationDate ? new Date(formValue.expirationDate).toISOString() : undefined, // 
    };


    let saveObservable: Observable<JobOffer>;

    if (this.isEditMode && this.id) {
      // Mode édition : appeler updateOffer
      saveObservable = this.jobOfferService.updateOffer(parseInt(this.id, 10), { id: parseInt(this.id, 10), ...offerRequest, createdAt: '' /* Ajouté pour correspondre au type JobOffer si besoin, mais pas envoyé */ });
    } else {
      // Mode création : appeler createOffer
      saveObservable = this.jobOfferService.createOffer(offerRequest);
    }

    saveObservable
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (savedOffer) => {
          console.log('Offer saved successfully:', savedOffer);
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Offre ${this.isEditMode ? 'mise à jour' : 'créée'} avec succès.` });
          // Rediriger vers la liste des offres ou le détail de l'offre créée/modifiée
          this.router.navigate(['/offers', savedOffer.id]); // Ou '/offers'
        },
        error: (err) => {
          console.error('Error saving offer:', err);
          let detail = `Erreur lors de ${this.isEditMode ? 'la mise à jour' : 'la création'} de l'offre.`;
           if (err.error?.message) { detail = err.error.message; }
           else if (typeof err.error === 'string') { detail = err.error; }
           this.errorMessages = [{ severity: 'error', summary: 'Échec', detail: detail }];
        }
      });
  }

  onCancel(): void {
      // Rediriger vers la page précédente ou la liste des offres
      if (this.isEditMode && this.id) {
          this.router.navigate(['/offers', this.id]);
      } else {
           this.router.navigate(['/offers']);
      }
  }
}

// Ajoutez la méthode findAllSkillsList() à SkillService
// src/app/features/admin/skill.service.ts
// findAllSkillsList(): Observable<Skill[]> {
//     // Appeler le backend pour récupérer TOUTES les compétences
//     // Attention à la performance si beaucoup de compétences
//     // Alternative : endpoint de recherche avec pagination infinie ?
//     return this.http.get<Skill[]>(`${this.apiUrl}/all`); // Supposons un endpoint /api/skills/all
// }