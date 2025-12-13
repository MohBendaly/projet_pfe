import { Inject, Injectable, inject,PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser} from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, map, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import { LoginRequest, JwtResponse, DecodedToken } from '../../core/models/user.model'; // Ajouter DecodedToken
import { UserDTO } from '../../features/auth/models/userDTO.model';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { jwtDecode } from "jwt-decode"; // Importer jwt-decode
const AUTH_API = `${environment.apiUrl}/auth/`;
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*'
   }),
   withCredentials: true 
};

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private currentUserSubject: BehaviorSubject<DecodedToken | null>;
  public currentUser: Observable<DecodedToken | null>;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  isAuthenticated =signal(false);
  public setAuthState(isAuthenticated: boolean): void {
    this.isAuthenticatedSubject.next(isAuthenticated);
  }

  constructor(private tokenStorage: TokenStorageService,
    @Inject(PLATFORM_ID) private platformId: Object) {
    this.checkInitialAuthState();
    this.currentUserSubject = new BehaviorSubject<DecodedToken | null>(this.getUserFromStorage());
    this.currentUser = this.currentUserSubject.asObservable();
    const token = this.tokenStorage.getToken();
    // Log initial
    console.log('AuthService Initialized - Has Token?', this.hasToken());
    this.isAuthenticatedSubject.next(!!token && !this.isTokenExpired(token));
    this.isAuthenticatedSubject.subscribe(value => {
      this.isAuthenticated.set(value);
    });
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
}
 private checkInitialAuthState(): void {
    // Ne pas exécuter côté serveur
    if (isPlatformBrowser(this.platformId)) {
      const token = this.tokenStorage.getToken();
      const isAuth = !!token && !this.isTokenExpired(token);
      this.isAuthenticatedSubject.next(isAuth);
    }
  }
  public get currentUserValue(): DecodedToken | null {
    return this.currentUserSubject.value;
  }
  
  isLoggedIn(): boolean {
    const loggedIn = this.hasToken();
    console.log('AuthService.isLoggedIn() check:', loggedIn); // <-- Log
    return loggedIn;
}
  public getToken(): string | null {
  const token = this.tokenStorage.getToken();
  console.log('AuthService - Token récupéré:', token ? 'présent' : 'absent');
  return this.tokenStorage.getToken();;
}
public isTokenValid(): boolean {
  const token = this.getToken();
  return !!token && !this.isTokenExpired(token);
}

  private hasToken(): boolean {
    try {
      const token = this.tokenStorage.getToken();
      return !!token && !this.isTokenExpired(token);
    } catch (e) {
      console.error('Error checking token:', e);
      return false;
    }
  }

private handleLoginResponse(response: any): void {
  if (!response?.jwttoken) {
    console.error('Token manquant dans la réponse', response);
    return;
  }

  // Stockage SSR-safe
  if (isPlatformBrowser(this.platformId)) {
    // 1. Stockage via TokenStorageService
    this.tokenStorage.setToken(response.jwttoken);
    
    // 2. Décodage forcé avec vérification
    try {
      const decoded: any = jwtDecode(response.jwttoken);
      
      this.currentUserSubject.next({
        sub: decoded.sub || '',
        roles: decoded.roles || [],
        exp: decoded.exp || 0,
        iat: decoded.iat || 0
      });
      
      // 3. Mise à jour de l'état APRÈS le décodage
      this.isAuthenticatedSubject.next(true);
      console.log('Auth state updated', this.isAuthenticatedSubject.value);
    } catch (e) {
      console.error('Erreur de décodage JWT', e);
    }
  }
}// In auth.service.ts
refreshToken(): Observable<JwtResponse> {
  return this.http.post<JwtResponse>(AUTH_API + 'refresh-token', {
    refreshToken: this.tokenStorage.getRefreshToken()
  }).pipe(
    tap(response => this.handleLoginResponse(response))
  );
}
   private isTokenExpired(token: string | null): boolean {
    if (!token) return true;
    try {
        const decoded: DecodedToken = jwtDecode<DecodedToken>(token);
        if (decoded.exp === undefined) return false; // Pas d'expiration
        const date = new Date(0);
        date.setUTCSeconds(decoded.exp);
        return date.valueOf() < new Date().valueOf();
    } catch (error) {
        console.error('Error decoding token for expiration check', error);
        return true; // Considérer comme expiré si erreur
    }
}
  private handleLoginError(error: HttpErrorResponse): Observable<never> {
  let errorMessage = 'An unknown error occurred';
  
  if (error.status === 0) {
    errorMessage = 'Network error - Please check your connection';
  } else if (error.status === 401) {
    errorMessage = 'Invalid email or password';
  } else if (error.error?.message) {
    errorMessage = error.error.message;
  }

  console.error('Login error:', error);
  return throwError(() => new Error(errorMessage));
}

login(credentials: LoginRequest): Observable<boolean> {
  return this.http.post<any>(`${environment.apiUrl}/auth/login`, credentials).pipe(
    tap(response => {
      console.log('Réponse brute:', response); // Debug
      this.handleLoginResponse(response);
    }),
    map(() => {
      // Vérification finale
      return !!this.tokenStorage.getToken();
    }),
    catchError(this.handleLoginError)
  );
}

private getLoginErrorMessage(error: any): string {
  switch (error?.status) {
    case 400:
      return 'Requête invalide. Vérifiez vos données.';
    case 401:
      return 'Email ou mot de passe incorrect.';
    case 403:
      return 'Accès refusé. Compte non autorisé.';
    case 500:
      return 'Erreur serveur. Réessayez plus tard.';
    default:
      return error.error?.message || 'Erreur de connexion inattendue.';
  }
}

  register(user: UserDTO): Observable<any> {
    return this.http.post(AUTH_API + 'register', user, httpOptions);
  }

  logout(): void {
    this.handleLogout();
    this.router.navigate(['/login']);
    this.tokenStorage.signOut();
    this.isAuthenticatedSubject.next(false); 
  }

  private handleLogout(): void {
      this.tokenStorage.signOut();
      this.currentUserSubject.next(null);
      this.isAuthenticatedSubject.next(false);
      console.log("User logged out and storage cleared.");
  }


  getUserRoles(): string[] {
    const user = this.currentUserValue; // Utiliser la valeur décodée
    return user?.roles || [];
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  // Récupère l'utilisateur depuis le storage (au démarrage)
  private getUserFromStorage(): DecodedToken | null {
       const token = this.tokenStorage.getToken();
       if (token && !this.isTokenExpired(token)) {
            return this.decodeToken(token);
       }
       // Si pas de token ou expiré, nettoyer
       this.tokenStorage.signOut();
       return null;
  }


  private decodeToken(token: string): DecodedToken | null {
      if (!token) return null;
      try {
          // Utiliser jwt-decode pour obtenir le payload
          const decoded = jwtDecode<DecodedToken>(token);
           // Retourner l'objet décodé (ajouter d'autres champs si nécessaire)
           // Assurez-vous que votre backend inclut bien les rôles dans le token !
           return {
               sub: decoded.sub, // username/email
               roles: decoded.roles || [], // rôles
               exp: decoded.exp, // expiration
               iat: decoded.iat // issued at
               // Ajoutez userId si votre backend le met dans le token
               // userId: decoded.userId
           };
      } catch (e) {
          console.error("Error decoding token", e);
           this.handleLogout(); // Nettoyer si token invalide
          return null;
      }
  }
}
