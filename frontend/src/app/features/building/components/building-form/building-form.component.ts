import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';
import { BuildingService } from '../../../../core/services/building.service';
import { PersonService } from '../../../../core/services/person.service';
import { Building } from '../../../../models/building.model';
import { PersonSummary } from '../../../../models/person.model';

/**
 * Create / Edit building form.
 * UC016 Phase 2: breadcrumb and cancel navigation include estateId.
 */
@Component({
  selector: 'app-building-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './building-form.component.html',
  styleUrls: ['./building-form.component.scss'],
})
export class BuildingFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  buildingId?: number;
  loading = false;
  submitting = false;
  errorMessage = '';
  isDirty = false;

  // Owner picker
  ownerSearchTerm = '';
  ownerResults: PersonSummary[] = [];
  selectedOwner: PersonSummary | null = null;
  ownerSearchLoading = false;

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private personService: PersonService,
    private router: Router,
    private route: ActivatedRoute,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!id && id !== 'new';
    this.buildForm();

    if (this.isEdit) {
      this.buildingId = Number(id);
      this.loading = true;
      this.buildingService.getBuildingById(this.buildingId).subscribe({
        next: (b) => {
          this.patchForm(b);
          this.loading = false;
          setTimeout(() => this.form.markAsPristine());
        },
        error: () => {
          this.errorMessage = 'Failed to load building.';
          this.loading = false;
        },
      });
    }

    this.form.valueChanges.subscribe(() => {
      this.isDirty = this.form.dirty;
    });
  }

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name:          ['', [Validators.required, Validators.maxLength(100)]],
      streetAddress: ['', [Validators.required, Validators.maxLength(200)]],
      postalCode:    ['', [Validators.required, Validators.maxLength(20)]],
      city:          ['', [Validators.required, Validators.maxLength(100)]],
      country:       ['Belgium', [Validators.required, Validators.maxLength(100)]],
    });
  }

  private patchForm(b: Building): void {
    this.form.patchValue({
      name:          b.name,
      streetAddress: b.streetAddress,
      postalCode:    b.postalCode,
      city:          b.city,
      country:       b.country,
    });
    if (b.ownerId && b.ownerName) {
      // Reconstruct a minimal PersonSummary for display
      this.selectedOwner = {
        id: b.ownerId,
        lastName: b.ownerName,
        firstName: '',
        isOwner: true,
        isTenant: false,
      };
    }
  }

  fieldError(name: string): string {
    const ctrl = this.form.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return '';
    if (ctrl.errors?.['required'])   return 'Required field';
    if (ctrl.errors?.['maxlength'])  return 'Value too long';
    return '';
  }

  // ── Owner picker ──────────────────────────────────────────────────────────

  searchOwner(): void {
    const q = this.ownerSearchTerm.trim();
    if (q.length < 2) {
      this.ownerResults = [];
      return;
    }
    this.ownerSearchLoading = true;
    this.personService.searchForPicker(q).subscribe({
      next: (results) => {
        this.ownerResults = results;
        this.ownerSearchLoading = false;
      },
      error: () => (this.ownerSearchLoading = false),
    });
  }

  selectOwner(person: PersonSummary): void {
    this.selectedOwner = person;
    this.ownerSearchTerm = '';
    this.ownerResults = [];
    this.form.markAsDirty();
  }

  clearOwner(): void {
    this.selectedOwner = null;
    this.form.markAsDirty();
  }

  // ── Submit / Cancel ───────────────────────────────────────────────────────

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;
    this.errorMessage = '';

    const val = this.form.value;
    const payload = {
      name:          val['name'],
      streetAddress: val['streetAddress'],
      postalCode:    val['postalCode'],
      city:          val['city'],
      country:       val['country'],
      ownerId:       this.selectedOwner?.id ?? null,
    };

    const obs$ = this.isEdit && this.buildingId != null
      ? this.buildingService.updateBuilding(this.buildingId, payload)
      : this.buildingService.createBuilding(payload);

    obs$.subscribe({
      next: (building) => {
        this.submitting = false;
        this.form.markAsPristine();
        this.router.navigate(['/estates', this.estateId, 'buildings', building.id]);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message ?? 'An error occurred.';
      },
    });
  }

  cancel(): void {
    if (
      this.isDirty &&
      !confirm('You have unsaved changes. Are you sure you want to cancel?')
    ) return;
    if (this.isEdit && this.buildingId != null) {
      this.router.navigate(['/estates', this.estateId, 'buildings', this.buildingId]);
    } else {
      this.router.navigate(['/estates', this.estateId, 'buildings']);
    }
  }
}
