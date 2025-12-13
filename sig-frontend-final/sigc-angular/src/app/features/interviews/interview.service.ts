import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Interview } from './interview.model';

// Interface pour la requête de complétion (si différente du DTO Interview)
export interface CompleteInterviewRequest {
    score?: number;
    feedback?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InterviewService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/interviews`; // Base URL pour les entretiens

  /**
   * Planifie un entretien pour une candidature donnée (Recruteur/Admin).
   * @param applicationId L'ID de la candidature pour laquelle planifier.
   * @param interviewData Données optionnelles (date/heure de début si applicable).
   */
  scheduleInterview(applicationId: number, interviewData: Partial<Interview>): Observable<Interview> {
      // L'endpoint est défini sur le contrôleur pour prendre l'ID de l'application dans le chemin
    return this.http.post<Interview>(`${this.apiUrl}/application/${applicationId}`, interviewData);
  }

  /**
   * Récupère les détails d'un entretien spécifique.
   * La sécurité backend vérifie l'accès.
   * @param interviewId L'ID de l'entretien.
   */
  getInterview(interviewId: number): Observable<Interview> {
    return this.http.get<Interview>(`${this.apiUrl}/${interviewId}`);
  }

   /**
   * Récupère tous les entretiens pour une candidature donnée.
   * La sécurité backend vérifie l'accès.
   * @param applicationId L'ID de la candidature.
   */
  getInterviewsForApplication(applicationId: number): Observable<Interview[]> {
      return this.http.get<Interview[]>(`${this.apiUrl}/application/${applicationId}`);
  }


  /**
   * Démarre un entretien (si un endpoint explicite existe).
   * @param interviewId L'ID de l'entretien.
   */
  startInterview(interviewId: number): Observable<Interview> {
    return this.http.post<Interview>(`${this.apiUrl}/${interviewId}/start`, {});
  }

  /**
   * Marque un entretien comme terminé et enregistre le score/feedback (Recruteur/Admin).
   * @param interviewId L'ID de l'entretien.
   * @param completionData Contient le score et le feedback.
   */
  completeInterview(interviewId: number, completionData: CompleteInterviewRequest): Observable<Interview> {
    return this.http.post<Interview>(`${this.apiUrl}/${interviewId}/complete`, completionData);
  }

  /**
   * Annule un entretien.
   * @param interviewId L'ID de l'entretien.
   */
  cancelInterview(interviewId: number): Observable<Interview> {
     return this.http.post<Interview>(`${this.apiUrl}/${interviewId}/cancel`, {});
  }


   /**
   * Déclenche la fin de l'entretien côté client et l'évaluation par l'IA.
   * @param interviewId L'ID de l'entretien.
   */
    finishAndEvaluateInterview(interviewId: number): Observable<Interview> {
        // Cet appel API correspond à la méthode du contrôleur qui appelle
        // ensuite chatService.evaluateInterviewWithGemini(...)
        return this.http.post<Interview>(`${this.apiUrl}/${interviewId}/finish`, {});
    }

}