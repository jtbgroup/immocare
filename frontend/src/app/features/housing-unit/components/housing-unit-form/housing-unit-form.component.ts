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
import { HousingUnitService } from '../../../../core/services/housing-unit.service';
import { PersonService } from '../../../../core/services/person.service';
import { Building } from '../../../../models/building.model';
import {
  HousingUnit,
  ORIENTATIONS,
  Orientation,
} from '../../../../models/housing-unit.model';
import { PersonSummary } from '../../../../models/person.model';

/**
 * Create / Edit housing unit form.
 * UC004_ESTATE_PLACEHOLDER Phase 2: buildingId may come from query param; navigation includes estateId.
 */
@Component({
  selector: 'app-housing-unit-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './housing-unit-form.component.html',
  styleUrls: ['./housing-unit-form.component.scss'],
})
export class HousingUnitFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  unitId?: number;
  preselectedBuildingId?: number;
  loading = false;
  submitting = false;
  errorMessage = '';
  isDirty = false;

  buildings: Building[] = [];
  readonly orientations: Orientation[] = ORIENTATIONS;

  // Owner picker
  ownerSearchTerm = '';
  ownerResults: PersonSummary[] = [];
  selectedOwner: PersonSummary | null = null;
  ownerSearchLoading = false;

  constructor(
    private fb: FormBuilder,
    private housingUnitService: HousingUnitService,
    private buildingService: BuildingService,
    private personService: PersonService,
    private router: Router,
    private route: ActivatedRoute,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!id && id !== 'new';
    this.preselectedBuildingId = this.route.snapshot.queryParams['buildingId']
      ? Number(this.route.snapshot.queryParams['buildingId'])
      : undefined;

    this.loadBuildings();
    this.buildForm();

    if (this.isEdit) {
      this.unitId = Number(id);
      this.loading = true;
      this.housingUnitService.getUnitById(this.unitId).subscribe({
        next: (u) => {
          this.patchForm(u);
          this.loading = false;
          setTimeout(() => this.form.markAsPristine());
        },
        error: () => {
          this.errorMessage = 'Failed to load unit.';
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

  private loadBuildings(): void {
    this.buildingService.getAllBuildings(0, 200, 'name,asc').subscribe({
      next: (page) => (this.buildings = page.content),
      error: () => { /* non-blocking */ },
    });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      buildingId:          [this.preselectedBuildingId ?? null, Validators.required],
      unitNumber:          ['', [Validators.required, Validators.maxLength(20)]],
      floor:               [0, [Validators.required]],
      landingNumber:       [''],
      totalSurface:        [null],
      hasTerrace:          [false],
      terraceSurface:      [null],
      terraceOrientation:  [null],
      hasGarden:           [false],
      gardenSurface:       [null],
      gardenOrientation:   [null],
    });
  }

  private patchForm(u: HousingUnit): void {
    this.form.patchValue({
      buildingId:         u.buildingId,
      unitNumber:         u.unitNumber,
      floor:              u.floor,
      landingNumber:      u.landingNumber ?? '',
      totalSurface:       u.totalSurface ?? null,
      hasTerrace:         u.hasTerrace,
      terraceSurface:     u.terraceSurface ?? null,
      terraceOrientation: u.terraceOrientation ?? null,
      hasGarden:          u.hasGarden,
      gardenSurface:      u.gardenSurface ?? null,
      gardenOrientation:  u.gardenOrientation ?? null,
    });
    if (u.ownerId && u.ownerName) {
      this.selectedOwner = {
        id: u.ownerId,
        lastName: u.ownerName,
        firstName: '',
        isOwner: true,
        isTenant: false,
      };
    }
  }

  get hasTerrace(): boolean { return !!this.form.get('hasTerrace')?.value; }
  get hasGarden():  boolean { return !!this.form.get('hasGarden')?.value;  }

  fieldError(name: string): string {
    const ctrl = this.form.get(name);
    if (!ctrl || !ctrl.invalid || !ctrl.touched) return '';
    if (ctrl.errors?.['required'])  return 'Required field';
    if (ctrl.errors?.['maxlength']) return 'Value too long';
    return '';
  }

  // ── Owner picker ──────────────────────────────────────────────────────────

  searchOwner(): void {
    const q = this.ownerSearchTerm.trim();
    if (q.length < 2) { this.ownerResults = []; return; }
    this.ownerSearchLoading = true;
    this.personService.searchForPicker(q).subscribe({
      next: (r) => { this.ownerResults = r; this.ownerSearchLoading = false; },
      error: () => (this.ownerSearchLoading = false),
    });
  }

  selectOwner(p: PersonSummary): void {
    this.selectedOwner = p;
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
      buildingId:         Number(val['buildingId']),
      unitNumber:         val['unitNumber'],
      floor:              Number(val['floor']),
      landingNumber:      val['landingNumber'] || undefined,
      totalSurface:       val['totalSurface'] ? Number(val['totalSurface']) : undefined,
      hasTerrace:         !!val['hasTerrace'],
      terraceSurface:     val['hasTerrace'] && val['terraceSurface'] ? Number(val['terraceSurface']) : undefined,
      terraceOrientation: val['hasTerrace'] ? val['terraceOrientation'] : undefined,
      hasGarden:          !!val['hasGarden'],
      gardenSurface:      val['hasGarden'] && val['gardenSurface'] ? Number(val['gardenSurface']) : undefined,
      gardenOrientation:  val['hasGarden'] ? val['gardenOrientation'] : undefined,
      ownerId:            this.selectedOwner?.id ?? null,
    };

    const obs$ = this.isEdit && this.unitId != null
      ? this.housingUnitService.update(this.unitId, payload)
      : this.housingUnitService.create(payload);

    obs$.subscribe({
      next: (unit) => {
        this.submitting = false;
        this.form.markAsPristine();
        this.router.navigate(['/estates', this.estateId, 'units', unit.id]);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message ?? 'An error occurred.';
      },
    });
  }

  cancel(): void {
    if (this.isDirty && !confirm('Unsaved changes. Cancel anyway?')) return;
    if (this.isEdit && this.unitId != null) {
      this.router.navigate(['/estates', this.estateId, 'units', this.unitId]);
    } else if (this.preselectedBuildingId) {
      this.router.navigate(['/estates', this.estateId, 'buildings', this.preselectedBuildingId]);
    } else {
      this.router.navigate(['/estates', this.estateId, 'units']);
    }
  }
}
