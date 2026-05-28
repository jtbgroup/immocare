import { Component, input, output } from "@angular/core"; // Notez les minuscules !
import { TranslocoModule } from "@ngneat/transloco";

@Component({
  selector: "app-pagination",
  standalone: true,
  imports: [TranslocoModule],
  templateUrl: "./pagination.component.html",
  //   styleUrls: ["./pagination.component.scss"],
})
export class PaginationComponent {
  // Syntaxe moderne pour les Inputs obligatoires
  currentPage = input.required<number>();
  totalPages = input.required<number>();
  totalElements = input.required<number>();

  // Syntaxe moderne pour l'Output (plus besoin de EventEmitter !)
  pageChange = output<number>();

  goToPage(newPage: number): void {
    // Avec les inputs "Signal", on accède à la valeur en appelant la fonction : ()
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.pageChange.emit(newPage); // L'émission reste identique
    }
  }
}
