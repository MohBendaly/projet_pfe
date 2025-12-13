import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpRequest, HttpEvent, HttpHeaders, HttpEventType } from '@angular/common/http'; // Importer les types nécessaires
import { Observable, map } from 'rxjs'; // Importer map si besoin
import { environment } from '../../../environments/environment'; // Pour l'URL de l'API
import { Attachment } from './attachment.model'; // Importer le modèle de données

@Injectable({
  providedIn: 'root' // Fourni globalement
})
export class AttachmentService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl; // URL de base de l'API backend

  /**
   * Uploade un fichier pour un candidat spécifique.
   * @param candidateId L'ID du candidat.
   * @param file Le fichier à uploader.
   * @returns Un Observable contenant les métadonnées de la pièce jointe créée (AttachmentDTO).
   */
  uploadForCandidate(candidateId: number, file: File): Observable<Attachment> {
    const formData: FormData = new FormData();
    // La clé 'file' doit correspondre au @RequestParam("file") du backend
    formData.append('file', file, file.name);

    const url = `${this.apiUrl}/candidates/${candidateId}/attachments`;
    console.log(`Uploading file to ${url} for candidate ${candidateId}`);

    // Utiliser post<Attachment> car le backend renvoie l'AttachmentDTO créé
    return this.http.post<Attachment>(url, formData, {
      // Pas besoin de définir Content-Type ici, le navigateur le fera
      // pour multipart/form-data avec la bonne boundary.
      // reportProgress: true, // Optionnel: pour suivre la progression de l'upload
      // observe: 'events' // Optionnel: pour obtenir tous les événements d'upload
    });
    // Si vous utilisez reportProgress et observe: 'events', le type de retour
    // serait Observable<HttpEvent<Attachment>> et vous devriez le filtrer.
    // Pour simplifier, on retourne juste l'Observable<Attachment> final.
  }

  /**
   * Uploade un fichier pour une candidature spécifique.
   * @param applicationId L'ID de la candidature.
   * @param file Le fichier à uploader.
   * @returns Un Observable contenant les métadonnées de la pièce jointe créée.
   */
  uploadForApplication(applicationId: number, file: File): Observable<Attachment> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);

    const url = `${this.apiUrl}/applications/${applicationId}/attachments`;
    console.log(`Uploading file to ${url} for application ${applicationId}`);

    return this.http.post<Attachment>(url, formData);
  }

  /**
   * Récupère les métadonnées d'une pièce jointe par son ID.
   * @param attachmentId L'ID de la pièce jointe.
   */
  getAttachmentMetadata(attachmentId: number): Observable<Attachment> {
    const url = `${this.apiUrl}/attachments/${attachmentId}/metadata`;
    console.log(`Fetching metadata from ${url}`);
    return this.http.get<Attachment>(url);
  }

   /**
   * Récupère le contenu binaire d'un fichier pour le téléchargement manuel côté client.
   * Cette méthode est MOINS courante car on utilise souvent l'URL de téléchargement direct.
   * @param attachmentId L'ID de la pièce jointe.
   * @returns Un Observable de Blob (contenu binaire).
   */
  downloadFileBlob(attachmentId: number): Observable<Blob> {
    const url = `${this.apiUrl}/attachments/${attachmentId}/download`;
    console.log(`Requesting file blob from ${url}`);
    return this.http.get(url, {
      responseType: 'blob' // Demander explicitement le Blob
    });
    // Utilisation typique dans le composant:
    // this.attachmentService.downloadFileBlob(id).subscribe(blob => {
    //   const url = window.URL.createObjectURL(blob);
    //   const a = document.createElement('a');
    //   a.href = url;
    //   a.download = 'nom_fichier.pdf'; // Récupérer le vrai nom depuis metadata
    //   a.click();
    //   window.URL.revokeObjectURL(url);
    // });
  }


  /**
   * Récupère la liste des métadonnées des pièces jointes pour un candidat.
   * @param candidateId L'ID du candidat.
   */
  getAttachmentsForCandidate(candidateId: number): Observable<Attachment[]> {
    const url = `${this.apiUrl}/candidates/${candidateId}/attachments`;
    console.log(`Fetching attachments from ${url}`);
    return this.http.get<Attachment[]>(url);
  }

  /**
   * Récupère la liste des métadonnées des pièces jointes pour une candidature.
   * @param applicationId L'ID de la candidature.
   */
  getAttachmentsForApplication(applicationId: number): Observable<Attachment[]> {
    const url = `${this.apiUrl}/applications/${applicationId}/attachments`;
     console.log(`Fetching attachments from ${url}`);
    return this.http.get<Attachment[]>(url);
  }

  /**
   * Supprime une pièce jointe.
   * @param attachmentId L'ID de la pièce jointe.
   * @returns Un Observable<void> car la réponse attendue est 204 No Content.
   */
  deleteAttachment(attachmentId: number): Observable<void> {
    const url = `${this.apiUrl}/attachments/${attachmentId}`;
    console.log(`Deleting attachment at ${url}`);
    return this.http.delete<void>(url);
  }

  /**
   * (Optionnel) Construit l'URL de téléchargement complète côté client.
   * Utile si l'API ne renvoie que le chemin relatif dans AttachmentDTO.
   * @param relativePath Le chemin retourné par le backend (ex: "candidates/1/cv.pdf")
   * @returns L'URL absolue pour le téléchargement.
   */
   buildDownloadUrl(relativePath: string | null | undefined): string | null {
       if (!relativePath) {
           return null;
       }
       // Important: L'URL de base pour les fichiers statiques peut être différente de l'apiUrl
       // Supposons que les fichiers sont servis depuis la racine ou un préfixe /uploads
       // par le ResourceHandler configuré dans Spring Boot (FileStorageConfig)

       // Obtenir l'origine (http://localhost:8080)
       const apiOrigin = environment.apiUrl.substring(0, environment.apiUrl.indexOf('/api'));
       // ou définir une base URL spécifique pour les uploads si nécessaire
       // const uploadsBaseUrl = 'http://localhost:8080'; // Ou juste '/' si servi à la racine

       // Construire l'URL
       const downloadUrl = `${apiOrigin}/uploads/${relativePath.replace(/\\/g, '/')}`; // Assurer les slashes corrects
       console.log(`Built download URL: ${downloadUrl}`);
       return downloadUrl;
   }

}