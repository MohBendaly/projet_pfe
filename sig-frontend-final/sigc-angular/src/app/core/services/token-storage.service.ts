import { Injectable, Inject, PLATFORM_ID } from '@angular/core'; // Importer Inject, PLATFORM_ID
import { isPlatformBrowser } from '@angular/common'; // Importer isPlatformBrowser

const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private readonly TOKEN_KEY = 'authToken';
  private readonly REFRESH_TOKEN_KEY = 'refreshToken';
  private readonly USER_KEY = 'authUser';
   private memoryStorage: { [key: string]: string } = {};
  private isBrowser: boolean; // Stocker l'état de la plateforme

  // Injecter PLATFORM_ID dans le constructeur
  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    // Déterminer si on est dans le navigateur au moment de l'instanciation
    this.isBrowser = isPlatformBrowser(this.platformId);
    console.log(`TokenStorageService initialized. Is Browser? ${this.isBrowser}`); // Log pour vérifier
  }
    private get storage(): Storage {
    if (isPlatformBrowser(this.platformId)) {
      return window.localStorage; // ou sessionStorage
    }
    return {
      getItem: (key: string) => this.memoryStorage[key] || null,
      setItem: (key: string, value: string) => this.memoryStorage[key] = value,
      removeItem: (key: string) => delete this.memoryStorage[key],
      clear: () => this.memoryStorage = {},
      length: Object.keys(this.memoryStorage).length,
      key: (index: number) => Object.keys(this.memoryStorage)[index]
    } as Storage;
  }

    setToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('authToken', token); // Nom cohérent avec getToken()
    }
  }
   getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('authToken'); // Sécurité : ne s'exécute que côté navigateur
    }
    return null;
  }
  // token-storage.service.ts
 saveToken(token: string, rememberMe: boolean = false): void {
    // Stockage persistant ou session
    const storage = rememberMe ? localStorage : sessionStorage;
    storage.setItem(this.TOKEN_KEY, token);
    console.log('Token saved in:', rememberMe ? 'localStorage' : 'sessionStorage');
  }

  removeToken(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.TOKEN_KEY);
    }
  }

  signOut(): void {
    if (this.isBrowser) { // Vérifier si on est dans le navigateur
      window.sessionStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
      window.sessionStorage.removeItem(USER_KEY);
      window.sessionStorage.clear();
      console.log('Session storage cleared (browser).');
    } else {
        console.log('Skipping session storage clear (not browser).');
    }
  }

  clear(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_KEY);
  }

  public saveUser(user: any): void {
    if (this.isBrowser) { // Vérifier si on est dans le navigateur
       localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    }
  }
    saveRefreshToken(token: string): void {
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }
  
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }
 
  public getUser(): any | null {
    if (this.isBrowser) { // Vérifier si on est dans le navigateur
      const user = window.sessionStorage.getItem(USER_KEY);
      if (user) {
        try {
          return JSON.parse(user);
        } catch (e) {
          console.error("Error parsing user data from storage", e);
          // Appeler signOut ici pourrait causer une boucle si getUser est appelé dedans
          // Il vaut mieux juste nettoyer le storage corrompu
          window.sessionStorage.removeItem(USER_KEY);
          return null;
        }
      }
    }
    return null; // Retourner null si pas dans le navigateur ou pas d'utilisateur
  }
  
}