import { Component, inject, OnInit, Input } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Observable, catchError, of, tap } from 'rxjs';
import { InterviewService } from '../interview.service';
import { Interview } from '../interview.model';
import { InterviewStatus, getInterviewStatusLabel, getInterviewStatusSeverity } from '../interview-status.enum';
import { AuthService } from '../../../core/services/auth.service';
import { Nl2brPipe } from '../../../shared/pipes/nl2br.pipe';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { PanelModule } from 'primeng/panel';
import { RatingModule } from 'primeng/rating';
import { FormsModule } from '@angular/forms';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { AvatarModule } from 'primeng/avatar';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-interview-detail',
  standalone: true,
  imports: [
    TagModule, ProgressSpinnerModule, PanelModule, RatingModule, FormsModule, ScrollPanelModule, AvatarModule,
    TagModule, ProgressSpinnerModule, PanelModule, RatingModule, FormsModule, ScrollPanelModule, AvatarModule, CommonModule, CardModule,    ButtonModule, RouterModule, Nl2brPipe
  ],
  templateUrl: './interview-detail.component.html',
  styleUrls: ['./interview-detail.component.scss']
})
export class InterviewDetailComponent implements OnInit {
  @Input() id!: string;

  private interviewService = inject(InterviewService);
  private authService = inject(AuthService);
  private router = inject(Router);

  interview$: Observable<Interview | null> | undefined;
  isLoading = false;
  userRole: 'CANDIDATE' | 'RECRUITER' | 'ADMIN' | 'UNKNOWN' = 'UNKNOWN';

  getInterviewStatusLabel = getInterviewStatusLabel;
  getInterviewStatusSeverity = getInterviewStatusSeverity;
  InterviewStatus = InterviewStatus;

  ngOnInit(): void {
    this.determineUserRole();

    if (this.id) {
      const interviewId = parseInt(this.id, 10);
      if (!isNaN(interviewId)) {
        this.loadInterview(interviewId);
      } else {
        console.error("Invalid interview ID");
        this.goBack();
      }
    }
  }

  determineUserRole(): void {
    if (this.authService.hasRole('ROLE_ADMIN')) {
      this.userRole = 'ADMIN';
    } else if (this.authService.hasRole('ROLE_RECRUITER')) {
      this.userRole = 'RECRUITER';
    } else if (this.authService.hasRole('ROLE_CANDIDATE')) {
      this.userRole = 'CANDIDATE';
    }
  }
getRatingScore(score?: number): number {
  if (!score) return 0;
  if (score >= 90) return 5;
  if (score >= 75) return 4;
  if (score >= 60) return 3;
  if (score >= 40) return 2;
  return 1;
}

  loadInterview(interviewId: number): void {
    this.isLoading = true;
    this.interview$ = this.interviewService.getInterview(interviewId).pipe(
      tap(interview => console.log("📋 Données de l'entretien :", interview)),
      catchError(error => {
        this.isLoading = false;
        console.error("Error loading interview details", error);
        this.goBack();
        return of(null);
      })
    );
  }

  goBack(): void {
    if (this.userRole === 'CANDIDATE') {
      this.router.navigate(['/candidate/applications']);
    } else if (this.userRole === 'RECRUITER' || this.userRole === 'ADMIN') {
      this.router.navigate(['/offers']);
    } else {
      this.router.navigate(['/']);
    }
  }

/*   getRatingScore(score: number | undefined | null): number {
    if (score === null || score === undefined) return 0;
    return Math.round(Math.max(0, Math.min(100, score)) / 20);
  } */
}
