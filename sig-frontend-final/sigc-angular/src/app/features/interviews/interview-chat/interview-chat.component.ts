// interview-chat.component.ts
import { Component, inject, OnInit, OnDestroy, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms'; // Pour ngModel (message input)
import { Observable, Subscription, catchError, of, finalize, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ChatService } from '../chat.service';
import { InterviewService } from '../interview.service'; // Pour terminer l'entretien
import { ChatMessage } from '../chat-message.model';
import { Interview } from '../interview.model';
import { InterviewStatus } from '../interview-status.enum'; // Pour vérifier le statut
import { AfterViewInit } from '@angular/core';
// ... autres imports ...
import { ScrollPanel } from 'primeng/scrollpanel'; // Importer le TYPE ScrollPanel pour ViewChild
// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextarea } from 'primeng/inputtextarea'; // <-- Import corrigé
import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel'; // Pour faire défiler le chat
import { AvatarModule } from 'primeng/avatar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog'; // Pour confirmer la fin
import { ConfirmationService } from 'primeng/api'; // Service pour ConfirmDialog

// Import Bootstrap JS types
declare var bootstrap: any;

@Component({
  selector: 'app-interview-chat',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    CardModule,
    InputTextarea,
    ButtonModule,
    ScrollPanelModule,
    AvatarModule,
    ProgressSpinnerModule,
    TagModule,
    ConfirmDialogModule // Importer ConfirmDialogModule
    ],
   providers: [ConfirmationService], // Fournir ConfirmationService
  templateUrl: './interview-chat.component.html',
  styleUrls: ['./interview-chat.component.scss']
})
export class InterviewChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() id!: string; // Interview ID from route
 // @ViewChild('scrollPanel') private scrollPanel!: ElementRef; // Pour scroller en bas
  @ViewChild('scrollPanel') private scrollPanelRef: ScrollPanel | undefined; // Renommer en scrollPanelRef pour clarté

  private chatService = inject(ChatService);
  private interviewService = inject(InterviewService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService); // Injecter ConfirmationService
  private router = inject(Router);

  interviewId!: number;
  messages: ChatMessage[] = [];
  newMessage: string = '';
  isLoadingHistory = false;
  isSendingMessage = false;
  isFinishingInterview = false;
  interviewStatus: InterviewStatus | null = null;
  interviewEnded = false;

  private messageSubscription: Subscription | undefined;
  private intervalSubscription: Subscription | undefined; // Pour recharger si besoin

  // Exposer l'enum au template
  InterviewStatus = InterviewStatus;
  // Utiliser ngAfterViewInit pour le premier scroll après le chargement initial
  ngAfterViewInit(): void {
    // Le scrollPanelRef devrait être défini ici
    console.log('ngAfterViewInit: scrollPanelRef is', this.scrollPanelRef);
    // On peut tenter un premier scroll ici si l'historique est déjà chargé
    // mais il est plus fiable de le faire à la fin de loadHistory
}
  ngOnInit(): void {
    if (this.id) {
      this.interviewId = parseInt(this.id, 10);
      if (!isNaN(this.interviewId)) {
        this.loadInitialData();
        // Optionnel : recharger périodiquement si le statut peut changer par le recruteur
        // this.startPolling();
      } else {
        console.error("Invalid interview ID");
        this.router.navigate(['/']); // Ou page d'erreur
      }
    }
  }

  ngOnDestroy(): void {
    this.messageSubscription?.unsubscribe();
    this.intervalSubscription?.unsubscribe();
  }

   ngAfterViewChecked(): void {
        this.scrollToBottom(); // Faire défiler vers le bas après chaque mise à jour de la vue
    }

  loadInitialData(): void {
    this.isLoadingHistory = true;
    // Charger les détails de l'entretien ET l'historique
    // On pourrait utiliser forkJoin ici pour les appels parallèles
    this.interviewService.getInterview(this.interviewId).subscribe({
       next: (interview) => {
            this.interviewStatus = interview.status;
            this.interviewEnded = this.interviewStatus === InterviewStatus.COMPLETED || this.interviewStatus === InterviewStatus.CANCELLED;
            // Charger l'historique seulement si l'entretien existe
            this.loadHistory();
       },
       error: (err) => {
            this.isLoadingHistory = false;
            console.error("Error loading interview details", err);
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les détails de l\'entretien.' });
            this.router.navigate(['/']); // Ou page d'erreur
       }
    });
  }

  loadHistory(): void {
     this.isLoadingHistory = true; // Peut être déjà true
    this.chatService.getChatHistory(this.interviewId)
      .pipe(finalize(() => this.isLoadingHistory = false))
      .subscribe({
        next: (history) => {
          this.messages = history;
          this.scrollToBottom(); // Défiler après chargement initial
        },
        error: (err) => {
          console.error("Error loading chat history", err);
           this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger l\'historique du chat.' });
        }
      });
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || this.isSendingMessage || this.interviewEnded) {
      return;
    }

    const messageToSend = this.newMessage;
    this.newMessage = ''; // Vider le champ immédiatement
    this.isSendingMessage = true;

    // Ajouter le message utilisateur à la liste (pour affichage immédiat)
     const userMessage: ChatMessage = {
         id: Date.now(), // ID temporaire pour la clé *ngFor
         content: messageToSend,
         isFromBot: false,
         interviewId: this.interviewId,
         timestamp: new Date().toISOString()
     };
     this.messages = [...this.messages, userMessage]; // Nouvelle référence pour détection de changement
     this.scrollToBottom();


    this.chatService.postMessage(this.interviewId, { content: messageToSend })
      .pipe(finalize(() => this.isSendingMessage = false))
      .subscribe({
        next: (botResponse) => {
          console.log('Bot response received:', botResponse);
           // Ajouter la réponse du bot à la liste
           this.messages = [...this.messages, botResponse]; // Nouvelle référence
           this.scrollToBottom();
           // Mettre à jour le statut si le backend démarre l'entretien au premier message
            if(this.interviewStatus === InterviewStatus.SCHEDULED) {
                this.interviewStatus = InterviewStatus.IN_PROGRESS;
            }
        },
        error: (err) => {
          console.error('Error sending message:', err);
          // Supprimer le message utilisateur temporaire si l'envoi échoue ? Ou afficher une erreur à côté.
          this.messages = this.messages.filter(m => m.id !== userMessage.id); // Enlever le message optimiste
          this.newMessage = messageToSend; // Remettre le texte dans le champ
           let detail = 'Erreur lors de l\'envoi du message.';
           if (err.error?.message) { detail = err.error.message; }
           else if (typeof err.error === 'string') { detail = err.error; }
           this.messageService.add({ severity: 'error', summary: 'Erreur', detail: detail });
        }
      });
  }

  scrollToBottom(): void {
    if (this.isFinishingInterview || this.interviewEnded) return; // Ne pas scroller si l'entretien est terminé
    // Utiliser setTimeout pour s'assurer que le DOM est mis à jour
    setTimeout(() => {
        try {
            // Vérifier si scrollPanelRef est défini AVANT d'accéder à nativeElement
            if (this.scrollPanelRef && this.scrollPanelRef.contentViewChild?.nativeElement) {
               // Utiliser l'accès interne de ScrollPanel si possible (plus fiable)
                const panelContent = this.scrollPanelRef.contentViewChild.nativeElement;
                panelContent.scrollTop = panelContent.scrollHeight;
                console.log('Scrolled to bottom.');
            } else if (this.scrollPanelRef?.el?.nativeElement) {
                // Fallback si contentViewChild n'est pas dispo (plus ancien ou structure différente)
                const container = this.scrollPanelRef.el.nativeElement.querySelector('.p-scrollpanel-content');
                if (container) {
                   container.scrollTop = container.scrollHeight;
                   console.log('Scrolled to bottom (using querySelector fallback).');
                } else {
                    console.warn('Could not find scrollable content element (.p-scrollpanel-content).');
                }
            } else {
                console.warn('ScrollPanel reference not available yet for scrolling.');
            }
         } catch (err) {
             console.error("Could not scroll to bottom:", err); // Log l'erreur réelle
         }
     }, 1111111150); // Un léger délai (ex: 50ms) peut parfois aider encore plus
 }

    confirmFinishInterview(): void {
        if (this.isFinishingInterview || this.interviewEnded) return;

        this.confirmationService.confirm({
            message: 'Êtes-vous sûr de vouloir terminer cet entretien ? Cette action est irréversible et déclenchera l\'évaluation par l\'IA.',
            header: 'Confirmation de Fin',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Oui, terminer',
            rejectLabel: 'Non, annuler',
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-secondary',
            accept: () => {
                this.finishInterview();
            },
            reject: () => {
                 console.log('Finish interview cancelled');
            }
        });
    }

 @ViewChild('confirmModal') confirmModal!: ElementRef;

  openModal() {
    const modalEl = this.confirmModal.nativeElement;
    new bootstrap.Modal(modalEl).show();
  }

  closeModal() {
    const modalEl = this.confirmModal.nativeElement;
    bootstrap.Modal.getInstance(modalEl)?.hide();
  }

  
    finishInterview(): void {
        if (this.isFinishingInterview || this.interviewEnded) return;
    this.closeModal();

        this.isFinishingInterview = true;
        this.interviewService.finishAndEvaluateInterview(this.interviewId)
         .pipe(finalize(() => this.isFinishingInterview = false))
         .subscribe({
             next: (updatedInterview) => {
                  console.log("Interview finished and evaluated", updatedInterview);
                  this.interviewStatus = updatedInterview.status;
                   this.interviewEnded = true;
                  this.messageService.add({ severity: 'success', summary: 'Entretien Terminé', detail: 'L\'entretien est terminé. L\'évaluation est disponible.', life: 5000 });
                  // Optionnel : rediriger vers la page de détails après un délai
                   setTimeout(() => this.router.navigate(['/candidate/interviews', this.interviewId, 'details']), 2000);
             },
             error: (err) => {
                  console.error("Error finishing interview:", err);
                  this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de terminer l\'entretien.' });
             }
         });
    }

    // --- Optionnel : Polling pour vérifier le statut ---
    startPolling(): void {
      this.intervalSubscription = timer(0, 30000) // Vérifier toutes les 30 sec
        .pipe(
          switchMap(() => this.interviewService.getInterview(this.interviewId)),
          catchError(() => of(null)) // Gérer les erreurs de polling
        )
        .subscribe(interview => {
          if (interview && interview.status !== this.interviewStatus) {
            console.log("Interview status changed:", interview.status);
            this.interviewStatus = interview.status;
            this.interviewEnded = this.interviewStatus === InterviewStatus.COMPLETED || this.interviewStatus === InterviewStatus.CANCELLED;
            if (this.interviewEnded) {
                this.messageService.add({ severity: 'info', summary: 'Statut Entretien', detail: `L'entretien est maintenant ${this.interviewStatus === InterviewStatus.COMPLETED ? 'terminé' : 'annulé'}.` });
                this.intervalSubscription?.unsubscribe(); // Arrêter le polling
            }
          }
        });
    }
}