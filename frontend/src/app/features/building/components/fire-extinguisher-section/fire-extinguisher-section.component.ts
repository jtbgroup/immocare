import { CommonModule } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

import { FireExtinguisherService } from "../../../../core/services/fire-extinguisher.service";
import {
  AddRevisionRequest,
  FireExtinguisher,
  SaveFireExtinguisherRequest,
} from "../../../../models/fire-extinguisher.model";
import { AppDatePipe } from "../../../../shared/pipes/app-date.pipe";

@Component({
  selector: "app-fire-extinguisher-section",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, AppDatePipe],
  templateUrl: "./fire-extinguisher-section.component.html",
  styleUrls: ["./fire-extinguisher-section.component.scss"],
})
export class FireExtinguisherSectionComponent implements OnInit, OnDestroy {
  @Input() buildingId!: number;
  @Input() buildingUnits: { id: number; unitNumber: string }[] = [];

  extinguishers: FireExtinguisher[] = [];
  loading = false;
  error: string | null = null;

  // Add/Edit form
  showForm = false;
  editingId: number | null = null;
  saving = false;
  saveError: string | null = null;
  form!: FormGroup;

  // Delete extinguisher confirmation
  deleteConfirmId: number | null = null;

  // Revision panel (open/closed per extinguisher id)
  revisionPanelOpen: Record<number, boolean> = {};

  // Add revision form (open per extinguisher id)
  revisionFormOpenId: number | null = null;
  revisionForm!: FormGroup;
  savingRevision = false;
  revisionFormError: string | null = null;

  // Delete revision confirmation
  deleteRevisionConfirmId: number | null = null;
  deleteRevisionExtId: number | null = null;

  today = new Date().toISOString().split("T")[0];

  private destroy$ = new Subject<void>();
  extinguisherTransactionCounts: Record<number, number> = {};

  constructor(
    private service: FireExtinguisherService,
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      identificationNumber: [
        "",
        [Validators.required, Validators.maxLength(50)],
      ],
      unitId: [null],
      notes: ["", Validators.maxLength(2000)],
    });

    this.revisionForm = this.fb.group({
      revisionDate: ["", Validators.required],
      notes: ["", Validators.maxLength(2000)],
    });

    this.loadExtinguishers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadExtinguishers(): void {
    this.loading = true;
    this.error = null;
    this.service
      .getByBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.extinguishers = data;
          this.loading = false;
          data.forEach((e) => this.loadExtinguisherTransactionCount(e.id));
        },
        error: () => {
          this.error = "Failed to load fire extinguishers.";
          this.loading = false;
        },
      });
  }

  openAddForm(): void {
    this.form.reset({ identificationNumber: "", unitId: null, notes: "" });
    this.editingId = null;
    this.saveError = null;
    this.showForm = true;
  }

  openEditForm(ext: FireExtinguisher): void {
    this.form.patchValue({
      identificationNumber: ext.identificationNumber,
      unitId: ext.unitId,
      notes: ext.notes ?? "",
    });
    this.editingId = ext.id;
    this.saveError = null;
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.saveError = null;
  }

  saveForm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const req: SaveFireExtinguisherRequest = {
      identificationNumber: this.form.value.identificationNumber,
      unitId: this.form.value.unitId ?? null,
      notes: this.form.value.notes || null,
    };
    this.saving = true;
    this.saveError = null;

    const call$ =
      this.editingId !== null
        ? this.service.update(this.editingId, req)
        : this.service.create(this.buildingId, req);

    call$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.loadExtinguishers();
      },
      error: (err) => {
        this.saving = false;
        this.saveError =
          err.error?.message ?? "Failed to save. Please try again.";
      },
    });
  }

  confirmDelete(id: number): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  doDelete(): void {
    if (this.deleteConfirmId === null) return;
    this.service
      .delete(this.deleteConfirmId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleteConfirmId = null;
          this.loadExtinguishers();
        },
        error: () => {
          this.deleteConfirmId = null;
        },
      });
  }

  toggleRevisionPanel(extId: number): void {
    this.revisionPanelOpen[extId] = !this.revisionPanelOpen[extId];
  }

  openRevisionForm(extId: number): void {
    this.revisionForm.reset({ revisionDate: "", notes: "" });
    this.revisionFormError = null;
    this.revisionFormOpenId = extId;
  }

  cancelRevisionForm(): void {
    this.revisionFormOpenId = null;
    this.revisionFormError = null;
  }

  saveRevision(extId: number): void {
    if (this.revisionForm.invalid) {
      this.revisionForm.markAllAsTouched();
      return;
    }
    const req: AddRevisionRequest = {
      revisionDate: this.revisionForm.value.revisionDate,
      notes: this.revisionForm.value.notes || null,
    };
    this.savingRevision = true;
    this.revisionFormError = null;

    this.service
      .addRevision(extId, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.savingRevision = false;
          this.revisionFormOpenId = null;
          const idx = this.extinguishers.findIndex((e) => e.id === extId);
          if (idx !== -1) {
            this.extinguishers[idx] = updated;
          }
        },
        error: (err) => {
          this.savingRevision = false;
          this.revisionFormError =
            err.error?.message ?? "Failed to save revision.";
        },
      });
  }

  confirmDeleteRevision(extId: number, revId: number): void {
    this.deleteRevisionExtId = extId;
    this.deleteRevisionConfirmId = revId;
  }

  cancelDeleteRevision(): void {
    this.deleteRevisionExtId = null;
    this.deleteRevisionConfirmId = null;
  }

  doDeleteRevision(): void {
    if (
      this.deleteRevisionExtId === null ||
      this.deleteRevisionConfirmId === null
    )
      return;
    this.service
      .deleteRevision(this.deleteRevisionExtId, this.deleteRevisionConfirmId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleteRevisionExtId = null;
          this.deleteRevisionConfirmId = null;
          this.loadExtinguishers();
        },
        error: () => {
          this.deleteRevisionExtId = null;
          this.deleteRevisionConfirmId = null;
        },
      });
  }

  latestRevisionDate(ext: FireExtinguisher): string | null {
    return ext.revisions[0]?.revisionDate ?? null;
  }

  // ─── Transaction count ────────────────────────────────────────────────────

  loadExtinguisherTransactionCount(extinguisherId: number): void {
    this.http
      .get<number>(
        `/api/v1/fire-extinguishers/${extinguisherId}/transaction-count`,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (count: number) =>
          (this.extinguisherTransactionCounts[extinguisherId] = count),
      );
  }

  navigateToExtinguisherTransactions(extinguisherId: number): void {
    this.router.navigate(["/transactions"], {
      queryParams: {
        tab: "list",
        assetType: "FIRE_EXTINGUISHER",
        assetId: extinguisherId,
      },
    });
  }
}
