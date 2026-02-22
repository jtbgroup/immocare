import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PebScoreService } from '../../../../core/services/peb-score.service';
import {
  CreatePebScoreRequest,
  ExpiryWarning,
  PebScoreDTO,
  PEB_SCORE_DISPLAY,
  PEB_SCORE_ORDER,
} from '../../../../models/peb-score.model';
import { PebHistoryComponent } from '../peb-history/peb-history.component';

@Component({
  selector: 'app-peb-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PebHistoryComponent],
  templateUrl: './peb-section.component.html',
  styleUrls: ['./peb-section.component.css'],
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

  form!: FormGroup;
  readonly scores = PEB_SCORE_ORDER;
  readonly display = PEB_SCORE_DISPLAY;
  readonly today = new Date().toISOString().split('T')[0];

  private destroy$ = new Subject<void>();

  constructor(private pebScoreService: PebScoreService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadCurrentScore();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      pebScore: [null, Validators.required],
      scoreDate: [null, Validators.required],
      certificateNumber: [null],
      validUntil: [null],
    });
  }

  loadCurrentScore(): void {
    this.loading = true;
    this.pebScoreService.getCurrentScore(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dto) => { this.current = dto; this.loading = false; },
        error: (err) => {
          if (err.status === 204 || err.status === 404) {
            this.current = null;
          }
          this.loading = false;
        },
      });
  }

  openForm(): void {
    this.form.reset();
    this.saveError = null;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.saveError = null;

    const req: CreatePebScoreRequest = {
      pebScore: this.form.value.pebScore,
      scoreDate: this.form.value.scoreDate,
      certificateNumber: this.form.value.certificateNumber || null,
      validUntil: this.form.value.validUntil || null,
    };

    this.pebScoreService.addScore(this.unitId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.showForm = false;
          this.loadCurrentScore();
        },
        error: (err) => {
          this.saving = false;
          this.saveError = err.error?.message || 'An error occurred while saving.';
        },
      });
  }

  get expiryClass(): string {
    const w = this.current?.expiryWarning as ExpiryWarning;
    if (w === 'EXPIRED') return 'badge-expired';
    if (w === 'EXPIRING_SOON') return 'badge-warning';
    if (w === 'VALID') return 'badge-valid';
    return 'badge-nodate';
  }

  get expiryLabel(): string {
    const w = this.current?.expiryWarning as ExpiryWarning;
    if (w === 'EXPIRED') return 'Expired';
    if (w === 'EXPIRING_SOON') return 'Expires soon';
    if (w === 'VALID') return 'Valid';
    return 'No expiry date';
  }
}
