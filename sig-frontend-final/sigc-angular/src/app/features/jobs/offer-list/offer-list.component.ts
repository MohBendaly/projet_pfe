import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  Subject,
  Subscription,
  switchMap,
  catchError,
  of,
  tap,
  debounceTime,
  distinctUntilChanged,
  takeUntil
} from 'rxjs';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

// Services et Modèles
import { JobOfferService } from '../job-offer.service';
import { JobOffer } from '../job-offer.model';
import { PagedResult } from '../../../core/models/paged-result.model';
import { AuthService } from '../../../core/services/auth.service';

// PrimeNG
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';

type PrimeNGSeverity = "success" | "secondary" | "info" | "warn" | "danger" | "contrast";

@Component({
  selector: 'app-offer-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NgbPaginationModule,
    ToolbarModule,
    ButtonModule,
    TagModule,
    InputTextModule,
    PaginatorModule,
    TooltipModule,
    ProgressSpinnerModule,
    SkeletonModule
  ],
  templateUrl: './offer-list.component.html',
  styleUrls: ['./offer-list.component.scss']
})
export class OfferListComponent implements OnInit, OnDestroy {
  // --- Injections ---
  private jobOfferService = inject(JobOfferService);
  public authService = inject(AuthService);
  private messageService = inject(MessageService);

  // --- State ---
  offers: JobOffer[] = [];
  isLoading = true;
  errorLoading = false;
  currentPage = 1;
  pageSize = 6;

  // --- Filtre ---
  searchKeyword: string = '';
  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription | undefined;

  // --- Pagination ---
  first: number = 0;
  rows: number = 6;
  totalRecords: number = 0;

  // --- Rôle ---
  isRecruiterOrAdmin = false;

  // --- Subscriptions ---
  private offersSubscription: Subscription | undefined;
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.isRecruiterOrAdmin = this.authService.hasRole('ROLE_RECRUITER') || this.authService.hasRole('ROLE_ADMIN');
    this.setupSearchDebounce();
    this.loadOffers(0, this.rows);
  }

  ngOnDestroy(): void {
    this.offersSubscription?.unsubscribe();
    this.searchSubscription?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOffers(page: number, size: number, sort: string = 'publicationDate,desc', keyword?: string): void {
    this.isLoading = true;
    this.errorLoading = false;
    this.offers = [];

    this.offersSubscription?.unsubscribe();

    const observable$ = this.isRecruiterOrAdmin
      ? this.jobOfferService.getAllOffers(page, size, sort, keyword)
      : this.jobOfferService.getPublishedOffers(page, size, sort, keyword);

    this.offersSubscription = observable$.subscribe({
      next: (result: PagedResult<JobOffer>) => {
        this.offers = result?.content ?? [];
        this.totalRecords = result?.totalElements ?? 0;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading job offers:', error);
        this.isLoading = false;
        this.errorLoading = true;
        this.offers = [];
        this.totalRecords = 0;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur de chargement',
          detail: 'Impossible de récupérer les offres. Vérifiez la connexion au backend.',
          life: 7000
        });
      }
    });
  }

  setupSearchDebounce(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(keyword => {
      this.first = 0;
      this.loadOffers(0, this.rows, undefined, keyword);
    });
  }

  onSearchInput(event: Event): void {
    const keyword = (event.target as HTMLInputElement).value;
    this.searchSubject.next(keyword);
  }

  onPageChange(event: PaginatorState): void {
    if (event.first !== undefined && event.rows !== undefined && event.page !== undefined) {
      this.first = event.first;
      this.rows = event.rows;
      this.loadOffers(event.page, this.rows, undefined, this.searchKeyword);
    }
  }

  getSeverity(status: string | undefined): PrimeNGSeverity | undefined {
    if (!status) return 'secondary';
    switch (status) {
      case 'PUBLISHED': return 'success';
      case 'DRAFT': return 'secondary';
      case 'ARCHIVED': return 'danger';
      case 'CLOSED': return 'warn';
      default: return 'secondary';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PUBLISHED': return 'Publiée';
      case 'DRAFT': return 'Brouillon';
      case 'ARCHIVED': return 'Archivée';
      case 'CLOSED': return 'Clôturée';
      default: return status;
    }
  }

  trackByOfferId(index: number, offer: JobOffer): number {
    return offer.id;
  }

  refreshList(): void {
    const currentPage = this.first / this.rows;
    this.loadOffers(currentPage, this.rows, undefined, this.searchKeyword);
  }
}
