import { CommonModule } from "@angular/common";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { PebScoreService } from "../../../../core/services/peb-score.service";
import {
  CreatePebScoreRequest,
  ExpiryWarning,
  PEB_SCORE_DISPLAY,
  PEB_SCORE_ORDER,
  PebScoreDTO,
} from "../../../../models/peb-score.model";
import { PebHistoryComponent } from "../peb-history/peb-history.component";

@Component({
  selector: "app-peb-section",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PebHistoryComponent],
  templateUrl: "./peb-section.component.html",
  styleUrls: ["./peb-section.component.css"],
})
export class PebSectionComponent implements OnInit, OnDestroy {
  @Input() unitId!: number;

  current: PebScoreDTO | null = null;
  loading = false;
  showForm = false;
  showHistory = false;
  saving = false;
  error: string | null = null;
  saveError: string | null = null;

  /** ID du score en cours d'édition (null = ajout) */
  editingId: number | null = null;

  form!: FormGroup;
  readonly scores = PEB_SCORE_ORDER;
  readonly display = PEB_SCORE_DISPLAY;
  readonly today = new Date().toISOString().split("T")[0];

  private destroy$ = new Subject<void>();

  constructor(
    private pebScoreService: PebScoreService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadCurrentScore();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm(): void {
    this.form = this.fb.group(
      {
        pebScore: [null, Validators.required],
        scoreDate: [null, Validators.required],
        certificateNumber: [null],
        validUntil: [null],
      },
      { validators: this.validUntilAfterScoreDate },
    );
  }

  /** Validateur cross-field : validUntil doit être > scoreDate */
  private validUntilAfterScoreDate(
    group: AbstractControl,
  ): ValidationErrors | null {
    const scoreDate = group.get("scoreDate")?.value;
    const validUntil = group.get("validUntil")?.value;
    if (scoreDate && validUntil && validUntil <= scoreDate) {
      group.get("validUntil")?.setErrors({ invalidRange: true });
      return { invalidRange: true };
    }
    return null;
  }

  loadCurrentScore(): void {
    this.loading = true;
    this.pebScoreService
      .getCurrentScore(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dto) => {
          this.current = dto;
          this.loading = false;
        },
        error: (err) => {
          if (err.status === 204 || err.status === 404) {
            this.current = null;
          }
          this.loading = false;
        },
      });
  }

  // FIX #2 : toggle correct pour View/Hide History
  toggleHistory(): void {
    this.showHistory = !this.showHistory;
  }

  openForm(): void {
    this.editingId = null;
    this.form.reset();
    this.saveError = null;
    this.showForm = true;
    this.showHistory = false;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
  }

  // FIX #3 : callback edit depuis peb-history
  onEditPeb(score: PebScoreDTO): void {
    this.editingId = score.id;
    this.form.patchValue({
      pebScore: score.pebScore,
      scoreDate: score.scoreDate,
      certificateNumber: score.certificateNumber ?? null,
      validUntil: score.validUntil ?? null,
    });
    this.saveError = null;
    this.showForm = true;
    this.showHistory = false;
  }

  // FIX #3 : callback delete depuis peb-history
  onDeletePeb(score: PebScoreDTO): void {
    if (
      !confirm(
        `Delete PEB score ${this.display[score.pebScore].label} from ${score.scoreDate}?`,
      )
    )
      return;
    this.pebScoreService
      .deleteScore(this.unitId, score.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadCurrentScore();
          // Recharger l'historique si ouvert
          if (this.showHistory) {
            this.showHistory = false;
            setTimeout(() => (this.showHistory = true), 0);
          }
        },
        error: (err) => {
          alert(err.error?.message || "Failed to delete PEB score.");
        },
      });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.saveError = null;

    const req: CreatePebScoreRequest = {
      pebScore: this.form.value.pebScore,
      scoreDate: this.form.value.scoreDate,
      certificateNumber: this.form.value.certificateNumber || null,
      validUntil: this.form.value.validUntil || null,
    };

    // Si édition, on utilise updateScore, sinon addScore
    const op$ = this.editingId
      ? this.pebScoreService.updateScore(this.unitId, this.editingId, req)
      : this.pebScoreService.addScore(this.unitId, req);

    op$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingId = null;
        this.loadCurrentScore();
      },
      error: (err) => {
        this.saving = false;
        this.saveError =
          err.error?.message || "An error occurred while saving.";
      },
    });
  }

  get expiryClass(): string {
    const w = this.current?.expiryWarning as ExpiryWarning;
    if (w === "EXPIRED") return "badge-expired";
    if (w === "EXPIRING_SOON") return "badge-warning";
    if (w === "VALID") return "badge-valid";
    return "badge-nodate";
  }

  get expiryLabel(): string {
    const w = this.current?.expiryWarning as ExpiryWarning;
    if (w === "EXPIRED") return "Expired";
    if (w === "EXPIRING_SOON") return "Expires soon";
    if (w === "VALID") return "Valid";
    return "No expiry date";
  }
}
