import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'nl2br',
  standalone: true // Rendre le pipe standalone
})
export class Nl2brPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }
    // Remplacer les nouvelles lignes par <br>, puis sanitizer
    const brValue = value.replace(/(\r\n|\r|\n)/g, '<br>');
    // Important: Faire confiance au HTML résultant
    return this.sanitizer.bypassSecurityTrustHtml(brValue);
  }
}