import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Skill, SkillRequest } from './skill.model';
import { PagedResult } from '../../core/models/paged-result.model';

@Injectable({
  providedIn: 'root'
})
export class SkillService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/skills`; // Base URL pour les compétences

  /**
   * Récupère une liste paginée de toutes les compétences (pour Admin).
   */
  findAllPaginated(page: number = 0, size: number = 10, sort: string = 'name,asc'): Observable<PagedResult<Skill>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<PagedResult<Skill>>(this.apiUrl, { params });
  }

   /**
   * Récupère la liste complète de toutes les compétences (pour les selects, attention performance).
   * Vous DEVEZ créer un endpoint backend pour cela (ex: GET /api/skills/all).
   */
  findAllSkillsList(): Observable<Skill[]> {
    // Supposons un endpoint /all qui retourne la liste complète
    // À adapter selon votre API backend !
    return this.http.get<Skill[]>(`${this.apiUrl}/all`);
    // Alternative si pas d'endpoint /all (NON RECOMMANDÉ pour beaucoup de données):
    // return this.findAllPaginated(0, 1000).pipe(map(result => result.content));
  }


  /**
   * Recherche des compétences par nom (pour Admin/Recruteur).
   * @param name Le terme de recherche.
   */
  searchSkills(name: string): Observable<Skill[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<Skill[]>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Récupère une compétence par son ID.
   */
  findById(id: number): Observable<Skill> {
    return this.http.get<Skill>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crée une nouvelle compétence (Admin).
   */
  createSkill(skillRequest: SkillRequest): Observable<Skill> {
    return this.http.post<Skill>(this.apiUrl, skillRequest);
  }

  /**
   * Met à jour une compétence existante (Admin).
   */
  updateSkill(id: number, skillRequest: SkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.apiUrl}/${id}`, skillRequest);
  }

  /**
   * Supprime une compétence (Admin).
   */
  deleteSkill(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}