// Interface générique pour représenter les résultats paginés de Spring Data Page<>
export interface PagedResult<T> {
    content: T[];          // Les éléments de la page actuelle
    pageable: PageableInfo;
    last: boolean;         // Est-ce la dernière page ?
    totalElements: number; // Nombre total d'éléments sur toutes les pages
    totalPages: number;    // Nombre total de pages
    size: number;          // Taille de la page demandée
    number: number;        // Numéro de la page actuelle (commence à 0)
    sort: SortInfo;
    first: boolean;        // Est-ce la première page ?
    numberOfElements: number; // Nombre d'éléments sur la page actuelle
    empty: boolean;        // La page est-elle vide ?
  }
  
  export interface PageableInfo {
    pageNumber: number;
    pageSize: number;
    sort: SortInfo;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  }
  
  export interface SortInfo {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  }
  
  // Vous pouvez simplifier si votre backend ne renvoie pas toute la structure Page<>
  // export interface SimplePagedResult<T> {
  //   content: T[];
  //   totalElements: number;
  //   totalPages: number;
  //   number: number; // page number (0-based)
  //   size: number;
  // }