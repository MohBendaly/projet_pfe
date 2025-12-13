import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Candidate, UpdateCandidateRequest } from './candidate.model';

@Injectable({
  providedIn: 'root'
})
export class CandidateService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/candidates`; // Base URL pour les candidats

  // Obtenir le profil du candidat connecté
  getMyProfile(): Observable<Candidate> {
    return this.http.get<Candidate>(`${this.apiUrl}/me`);
  }

  // Mettre à jour le profil du candidat connecté
  updateMyProfile(updateRequest: UpdateCandidateRequest): Observable<Candidate> {
    return this.http.put<Candidate>(`${this.apiUrl}/me`, updateRequest);
  }

  // Obtenir un profil par ID (pour admin/recruteur)
  getCandidateById(id: number): Observable<Candidate> {
      return this.http.get<Candidate>(`${this.apiUrl}/${id}`);
  }

  // Ajouter la méthode pour uploader le CV (via AttachmentService serait mieux)
  uploadResume(candidateId: number, file: File): Observable<any> {
      const formData = new FormData();
      formData.append('file', file, file.name);
      // Appeler l'endpoint d'upload spécifique pour le CV du candidat
      // Attention: L'endpoint et le service backend doivent être créés pour ça
      return this.http.post(`${this.apiUrl}/${candidateId}/resume`, formData); // Endpoint exemple
  }

  // Ajouter d'autres méthodes si nécessaire (ex: lister tous les candidats pour admin)
}