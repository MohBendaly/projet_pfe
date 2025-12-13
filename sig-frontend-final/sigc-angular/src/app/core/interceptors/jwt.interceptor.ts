import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';

// Interceptor fonctionnel (nouveau style Angular)
export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<any>,
  next: HttpHandlerFn
): Observable<HttpEvent<any>> => {

  const tokenStorageService = inject(TokenStorageService);
  const token = tokenStorageService.getToken();
  const isApiUrl = req.url.startsWith(environment.apiUrl); // Vérifier si c'est une requête vers notre API

  // Ajouter le header seulement si on a un token et si c'est une requête API
  if (token && isApiUrl) {
    // Cloner la requête pour ajouter le nouveau header
    const clonedReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    // console.log('JWT Interceptor: Added Authorization header');
    return next(clonedReq); // Passer la requête clonée au handler suivant
  } else {
    // Sinon, passer la requête originale
    return next(req);
  }
};

// Importer environment pour l'URL API
import { environment } from '../../../environments/environment';