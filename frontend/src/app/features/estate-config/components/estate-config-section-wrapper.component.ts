import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { TranslocoModule } from "@ngneat/transloco";
import { Observable } from "rxjs";
@Component({
  selector: "app-estate-config-section-wrapper",
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  template: `
    <div class="tab-wrapper-container">
      <!-- Injection dynamique du contenu de l'onglet (Champs, grille, etc.) -->
      <ng-content></ng-content>

      <!-- Bouton de sauvegarde unique pour cet onglet -->
      <div class="form-actions" style="margin-top: 2rem;">
        <button
          type="button"
          class="btn btn-primary"
          [disabled]="saving"
          (click)="onSubmit()"
        >
          {{
            saving
              ? ("GLOBAL.SAVING" | transloco) + "..."
              : ("GLOBAL.SAVE" | transloco)
          }}
        </button>
      </div>
    </div>
  `,
})
export class EstateConfigSectionWrapperComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) saveRequest!: () => Observable<any>;
  @Input() canEdit = false;

  saving = false;

  onSubmit(): void {
    if (!this.canEdit) return;

    this.saving = true;

    this.saveRequest().subscribe({
      next: () => {
        this.saving = false;
      },
      error: (err) => {
        this.saving = false;
        console.error("Erreur lors de la sauvegarde de l'onglet :", err);
      },
    });
  }
}
