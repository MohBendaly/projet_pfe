export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  // Optionnel : totalPages, number, etc.
}

export interface Offer {
  id: number;
  title: string;
  // ... autres champs
}