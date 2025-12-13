import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JobApplication } from './job-application.model';
import { ApplicationStatus } from './application-status.enum';
import { PagedResult } from '../../core/models/paged-result.model'; // Interface de pagination

// Interface pour la requête de création
export interface CreateJobApplicationRequest {
    jobOfferId: number;
    coverLetter?: string;
}

@Injectable({
  providedIn: 'root'
})
export class JobApplicationService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/applications`; // Base URL pour les candidatures

  /**
   * Crée une nouvelle candidature pour l'utilisateur connecté.
   * Le backend déduit l'ID du candidat à partir du token JWT.
   * @param request Contient l'ID de l'offre et la lettre de motivation.
   */
  applyToOffer(request: CreateJobApplicationRequest): Observable<JobApplication> {
    return this.http.post<JobApplication>(this.apiUrl, request);
  }

  /**
   * Récupère les candidatures de l'utilisateur connecté.
   */
  getMyApplications(): Observable<JobApplication[]> {
    // Le backend filtre par l'utilisateur authentifié
    return this.http.get<JobApplication[]>(`${this.apiUrl}/my`);
  }

  /**
   * Récupère les candidatures pour une offre spécifique (vue Recruteur/Admin).
   * @param offerId L'ID de l'offre.
   * @param page Numéro de page (base 0).
   * @param size Taille de la page.
   */
  getApplicationsByOfferId(offerId: number, page: number = 0, size: number = 10): Observable<PagedResult<JobApplication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResult<JobApplication>>(`${this.apiUrl}/offer/${offerId}`, { params });
  }

  /**
   * Récupère une candidature spécifique par son ID.
   * La sécurité backend vérifie si l'utilisateur (candidat ou recruteur) a le droit de la voir.
   * @param applicationId L'ID de la candidature.
   */
  getApplicationById(applicationId: number): Observable<JobApplication> {
      return this.http.get<JobApplication>(`${this.apiUrl}/${applicationId}`);
  }


  /**
   * Met à jour le statut d'une candidature (Recruteur/Admin).
   * @param applicationId L'ID de la candidature.
   * @param status Le nouveau statut.
   */
  updateApplicationStatus(applicationId: number, status: ApplicationStatus): Observable<JobApplication> {
    const params = new HttpParams().set('status', status);
    // Utilise PATCH car on ne modifie qu'un seul champ (le statut)
    return this.http.patch<JobApplication>(`${this.apiUrl}/${applicationId}/status`, null, { params }); // Pas de corps nécessaire
  }

  /**
   * Permet à un candidat de retirer sa propre candidature.
   * @param applicationId L'ID de la candidature à retirer.
   */
  withdrawApplication(applicationId: number): Observable<void> {
     // Utilise PATCH ou POST selon votre API backend
    return this.http.patch<void>(`${this.apiUrl}/${applicationId}/withdraw`, null);
  }
}