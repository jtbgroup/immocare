// features/person/person-form/person-form.component.ts — UC004_ESTATE_PLACEHOLDER Phase 3
import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { debounceTime, distinctUntilChanged } from "rxjs/operators";

import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { PersonService } from "../../../../core/services/person.service";
import { Person } from "../../../../models/person.model";

@Component({
  selector: "app-person-form",
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: "./person-form.component.html",
  styleUrls: ["./person-form.component.scss"],
})
export class PersonFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  personId?: number;
  isLoading = false;
  isSaving = false;
  errorMessage = "";
  nationalIdConflictId?: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private personService: PersonService,
    private activeEstateService: ActiveEstateService,
  ) {}

  get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  ngOnInit(): void {
    this.buildForm();

    this.route.paramMap.subscribe((params) => {
      const id = params.get("id");
      if (id) {
        this.isEditMode = true;
        this.personId = +id;
        this.loadPerson(+id);
      }
    });

    // Real-time nationalId uniqueness check
    this.form
      .get("nationalId")!
      .valueChanges.pipe(debounceTime(500), distinctUntilChanged())
      .subscribe((value) => {
        if (value && value.trim().length >= 2) {
          this.checkNationalIdUniqueness(value.trim());
        } else {
          this.nationalIdConflictId = undefined;
        }
      });
  }

  buildForm(): void {
    this.form = this.fb.group({
      // Identity
      lastName: ["", [Validators.required, Validators.maxLength(100)]],
      firstName: ["", [Validators.required, Validators.maxLength(100)]],
      birthDate: [null],
      birthPlace: ["", Validators.maxLength(100)],
      nationalId: ["", Validators.maxLength(20)],
      // Contact
      gsm: ["", Validators.maxLength(20)],
      email: ["", [Validators.email, Validators.maxLength(100)]],
      // Address
      streetAddress: ["", Validators.maxLength(200)],
      postalCode: ["", Validators.maxLength(20)],
      city: ["", Validators.maxLength(100)],
      country: ["Belgium", Validators.maxLength(100)],
    });
  }

  loadPerson(id: number): void {
    this.isLoading = true;
    this.personService.getPersonById(id).subscribe({
      next: (person: Person) => {
        this.form.patchValue({
          lastName: person.lastName,
          firstName: person.firstName,
          birthDate: person.birthDate,
          birthPlace: person.birthPlace,
          nationalId: person.nationalId,
          gsm: person.gsm,
          email: person.email,
          streetAddress: person.streetAddress,
          postalCode: person.postalCode,
          city: person.city,
          country: person.country || "Belgium",
        });
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Person not found.";
        this.isLoading = false;
      },
    });
  }

  checkNationalIdUniqueness(nationalId: string): void {
    this.personService.searchForPicker(nationalId).subscribe((results) => {
      const conflict = results.find(
        (p) =>
          p.nationalId?.toLowerCase() === nationalId.toLowerCase() &&
          p.id !== this.personId,
      );
      this.nationalIdConflictId = conflict?.id;
    });
  }

  submit(): void {
    if (this.form.invalid || this.nationalIdConflictId) return;

    this.isSaving = true;
    this.errorMessage = "";

    const request = { ...this.form.value };
    if (!request.country) request.country = "Belgium";

    const operation = this.isEditMode
      ? this.personService.update(this.personId!, request)
      : this.personService.create(request);

    operation.subscribe({
      next: (person) => {
        this.router.navigate(["/estates", this.estateId, "persons", person.id]);
      },
      error: (err) => {
        this.errorMessage =
          err.error?.message || "An error occurred. Please try again.";
        this.isSaving = false;
      },
    });
  }

  cancel(): void {
    if (this.isEditMode) {
      this.router.navigate([
        "/estates",
        this.estateId,
        "persons",
        this.personId,
      ]);
    } else {
      this.router.navigate(["/estates", this.estateId, "persons"]);
    }
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }
}
