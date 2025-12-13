import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatMessage, PostChatMessageRequest } from './chat-message.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private http = inject(HttpClient);
  // Base URL spécifique pour le chat d'un entretien
  private getChatApiUrl(interviewId: number): string {
      return `${environment.apiUrl}/interviews/${interviewId}/chat`;
  }

  /**
   * Récupère l'historique complet des messages pour un entretien.
   * @param interviewId L'ID de l'entretien.
   */
  getChatHistory(interviewId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(this.getChatApiUrl(interviewId));
  }

  /**
   * Envoie un message du candidat au backend.
   * Le backend traitera ce message, appellera l'IA (Gemini),
   * et ce service renverra la réponse du bot (IA).
   * @param interviewId L'ID de l'entretien.
   * @param request Contient le message du candidat.
   * @returns La réponse (ChatMessage) générée par le bot/IA.
   */
  postMessage(interviewId: number, request: PostChatMessageRequest): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(this.getChatApiUrl(interviewId), request);
  }
}