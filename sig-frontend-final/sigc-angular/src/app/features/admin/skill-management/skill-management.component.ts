// skill-management.component.ts
import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Pour pInputText dans le tableau
import { Observable, catchError, of, finalize, tap, Subject } from 'rxjs';
import { SkillService } from '../skill.service';
import { Skill, SkillRequest } from '../skill.model';

// PrimeNG
import { Table, TableModule } from 'primeng/table'; // Importer Table pour pouvoir le réinitialiser
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog'; // Pour le formulaire modal
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag'; // Si on affiche la catégorie



import { CardModule } from 'primeng/card'; // AJOUTER
import { ProgressSpinnerModule } from 'primeng/progressspinner'; // AJOUTER

@Component({
  selector: 'app-skill-management',
  standalone: true,
  imports: [ CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, DialogModule, ConfirmDialogModule, ToastModule, TagModule,CardModule, ProgressSpinnerModule ], // Importer les modules nécessaires
  providers: [ConfirmationService], // Fournir pour p-confirmDialog
  templateUrl: './skill-management.component.html',
  styleUrls: ['./skill-management.component.scss']
})
export class SkillManagementComponent implements OnInit {
  @ViewChild('skillsTable') skillsTable: Table | undefined; // Pour pouvoir reset le filtre

  private skillService = inject(SkillService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  skills: Skill[] = [];
  isLoading = false;

  // Pour le dialogue de création/modification
  skillDialogVisible = false;
  skillForm: SkillRequest = { name: '', category: '' };
  isEditMode = false;
  currentSkillId: number | null = null;
  submitted = false; // Pour la validation du dialogue

  constructor() { }

  ngOnInit(): void {
    this.loadSkills();
  }

  loadSkills(): void {
    this.isLoading = true;
    // Charger toutes les compétences (pas de pagination ici, adapter si besoin)
    this.skillService.findAllSkillsList() // Utilise la méthode créée pour OfferForm
        .pipe(finalize(() => this.isLoading = false))
        .subscribe({
            next: (data) => this.skills = data,
            error: (err) => {
                console.error("Error loading skills", err);
                this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les compétences.' });
            }
        });
  }

  openNewSkillDialog(): void {
    this.skillForm = { name: '', category: '' };
    this.isEditMode = false;
    this.currentSkillId = null;
    this.submitted = false;
    this.skillDialogVisible = true;
  }

  openEditSkillDialog(skill: Skill): void {
    this.skillForm = { name: skill.name, category: skill.category }; // Copier les données
    this.isEditMode = true;
    this.currentSkillId = skill.id;
    this.submitted = false;
    this.skillDialogVisible = true;
  }
selectedSkillId: number | null = null;

  hideSkillDialog(): void {
    this.skillDialogVisible = false;
    this.submitted = false;
  }









// Préparer une nouvelle compétence
prepareNewSkill() {
  this.skillForm = { name: '', category: '' };
  this.isEditMode = false;
  this.submitted = false;
}

// Préparer pour édition
prepareEditSkill(skill: any) {
  this.skillForm = { name: skill.name, category: skill.category };
  this.isEditMode = true;
  this.selectedSkillId = skill.id;
  this.submitted = false;
}






  saveSkill(): void {
    this.submitted = true;
    // Validation simple (nom requis)
    if (!this.skillForm.name?.trim()) {
        return;
    }

    let saveObservable: Observable<Skill>;
    if (this.isEditMode && this.currentSkillId) {
        saveObservable = this.skillService.updateSkill(this.currentSkillId, this.skillForm);
    } else {
        saveObservable = this.skillService.createSkill(this.skillForm);
    }

    saveObservable.subscribe({
        next: (savedSkill) => {
             this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Compétence ${this.isEditMode ? 'mise à jour' : 'créée'}.` });
             this.hideSkillDialog();
             this.loadSkills(); // Recharger la liste
        },
        error: (err) => {
             console.error("Error saving skill", err);
              let detail = `Erreur lors de ${this.isEditMode ? 'la mise à jour' : 'la création'}.`;
              if (err.status === 409) { // Duplicate
                  detail = err.error?.error || 'Ce nom de compétence existe déjà.';
              } else if (err.error?.message) { detail = err.error.message; }
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: detail });
             // Ne pas fermer le dialogue en cas d'erreur pour permettre la correction
        }
    });
  }

  confirmDeleteSkill(skillId: number): void {
      this.confirmationService.confirm({
            message: 'Êtes-vous sûr de vouloir supprimer cette compétence ? Cette action pourrait échouer si elle est utilisée.',
            header: 'Confirmation de Suppression',
            icon: 'pi pi-info-circle',
            acceptLabel: 'Oui, supprimer',
            rejectLabel: 'Non',
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-secondary',
            accept: () => {
                this.deleteSkill(skillId);
            }
        });
  }

  deleteSkill(skillId: number): void {
      this.skillService.deleteSkill(skillId).subscribe({
            next: () => {
                 this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Compétence supprimée.' });
                 this.loadSkills(); // Recharger
            },
            error: (err) => {
                console.error("Error deleting skill", err);
                 let detail = 'Impossible de supprimer la compétence.';
                 if (err.error?.message) { detail = err.error.message; }
                 this.messageService.add({ severity: 'error', summary: 'Erreur', detail: detail });
            }
      });
  }

  // Pour le filtre du tableau PrimeNG
  onGlobalFilter(table: Table, event: Event): void {
    table.filterGlobal((event.target as HTMLInputElement).value, 'contains');
  }

   clearFilter(table: Table): void {
       table.clear();
       const input = document.getElementById('globalFilterInput') as HTMLInputElement; // Assurez-vous que l'input a cet ID
       if (input) input.value = '';
   }

}

// Ajoutez la méthode findAllSkillsList() à SkillService si ce n'est pas déjà fait
// src/app/features/admin/skill.service.ts
// findAllSkillsList(): Observable<Skill[]> {
//     return this.http.get<Skill[]>(`${this.apiUrl}/all`); // Assurez-vous que l'endpoint existe et renvoie une liste
// }