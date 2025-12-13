
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages?: number;
  number?: number; // page actuelle
  size?: number;   // éléments par page
}

export interface Offer {
  id: number;
  title: string;
  description: string;
  // ajoutez d'autres champs selon votre API
  location?: string;
  salary?: number;
  createdAt?: Date;
}