import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JobOffer } from './job-offer.model';
import { PagedResult } from '../../core/models/paged-result.model'; // Créez cette interface générique

// Créez cette interface si besoin
// export interface PagedResult<T> {
//  content: T[];
//  totalElements: number;
//  totalPages: number;
//  size: number;
//  number: number; // page number
// }


@Injectable({
  providedIn: 'root' // Fourni globalement
})
export class JobOfferService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/offers`;

  // Récupérer les offres publiées (paginées)
  getPublishedOffers(page: number = 0, size: number = 10, sort: string = 'createdAt,desc', keyword?: string): Observable<PagedResult<JobOffer>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (keyword) {
      params = params.set('keyword', keyword);
    }
    // L'endpoint GET /api/offers du backend doit retourner un Page<JobOfferDTO>
    return this.http.get<PagedResult<JobOffer>>(this.apiUrl, { params });
  }

   // Récupérer toutes les offres pour admin/recruteur (paginées)
   getAllOffers(page: number = 0, size: number = 10, sort: string = 'createdAt,desc', status?: string): Observable<PagedResult<JobOffer>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (status) {
      params = params.set('status', status);
    }
    // L'endpoint GET /api/offers/all doit exister et être sécurisé
    return this.http.get<PagedResult<JobOffer>>(`${this.apiUrl}/all`, { params });
  }


  // Récupérer une offre par ID
  getOfferById(id: number): Observable<JobOffer> {
    return this.http.get<JobOffer>(`${this.apiUrl}/${id}`);
  }

  // Créer une offre
  createOffer(offer: Omit<JobOffer, 'id' | 'createdAt'>): Observable<JobOffer> {
    return this.http.post<JobOffer>(this.apiUrl, offer);
  }

  // Mettre à jour une offre
  updateOffer(id: number, offer: JobOffer): Observable<JobOffer> {
    return this.http.put<JobOffer>(`${this.apiUrl}/${id}`, offer);
  }

  // Mettre à jour le statut d'une offre
  updateOfferStatus(id: number, status: string): Observable<JobOffer> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<JobOffer>(`${this.apiUrl}/${id}/status`, null, { params }); // Pas de corps pour PATCH ici
  }

  // Supprimer une offre
  deleteOffer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}