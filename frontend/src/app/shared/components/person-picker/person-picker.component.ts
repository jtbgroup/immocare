// ============================================================
// shared/components/person-picker/person-picker.component.ts
// ============================================================
import {
  Component, Input, Output, EventEmitter,
  OnInit, OnDestroy, forwardRef
} from '@angular/core';
import {
  ControlValueAccessor, NG_VALUE_ACCESSOR,
  FormControl, ReactiveFormsModule
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { of, Subject, Subscription } from 'rxjs';

import { PersonService } from '../../../core/services/person.service';
import { PersonSummary } from '../../../models/person.model';

@Component({
  selector: 'app-person-picker',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './person-picker.component.html',
  styleUrls: ['./person-picker.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PersonPickerComponent),
      multi: true
    }
  ]
})
export class PersonPickerComponent implements ControlValueAccessor, OnInit, OnDestroy {
  @Input() label = 'Person';
  @Input() required = false;

  @Output() personSelected = new EventEmitter<PersonSummary | null>();

  searchControl = new FormControl('');
  results: PersonSummary[] = [];
  isOpen = false;
  isLoading = false;
  noResults = false;
  showMinCharsHint = false;

  selectedPerson: PersonSummary | null = null;

  private onChange: (value: PersonSummary | null) => void = () => {};
  private onTouched: () => void = () => {};
  private sub = new Subscription();

  constructor(private personService: PersonService) {}

  ngOnInit(): void {
    this.sub.add(
      this.searchControl.valueChanges.pipe(
        debounceTime(300),
        distinctUntilChanged()
      ).subscribe(query => {
        if (!query || query.trim().length < 2) {
          this.results = [];
          this.noResults = false;
          this.isOpen = query !== null && query.trim().length > 0;
          this.showMinCharsHint = query !== null && query.trim().length === 1;
          return;
        }
        this.showMinCharsHint = false;
        this.isLoading = true;
        this.isOpen = true;
        this.personService.searchForPicker(query.trim()).pipe(
          catchError(() => of([]))
        ).subscribe(list => {
          this.results = list;
          this.noResults = list.length === 0;
          this.isLoading = false;
        });
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  openPicker(): void {
    if (!this.selectedPerson) {
      this.isOpen = true;
      this.searchControl.setValue('');
    }
  }

  selectPerson(person: PersonSummary): void {
    this.selectedPerson = person;
    this.isOpen = false;
    this.results = [];
    this.searchControl.setValue('', { emitEvent: false });
    this.onChange(person);
    this.onTouched();
    this.personSelected.emit(person);
  }

  clearSelection(): void {
    this.selectedPerson = null;
    this.isOpen = false;
    this.results = [];
    this.searchControl.setValue('', { emitEvent: false });
    this.onChange(null);
    this.onTouched();
    this.personSelected.emit(null);
  }

  closeDropdown(): void {
    setTimeout(() => {
      this.isOpen = false;
    }, 200); // allow click to register
  }

  getDisplayLabel(p: PersonSummary): string {
    let label = `${p.lastName} ${p.firstName}`;
    if (p.city) label += ` — ${p.city}`;
    if (p.nationalId) label += ` (${p.nationalId})`;
    return label;
  }

  // ControlValueAccessor
  writeValue(value: PersonSummary | null): void {
    this.selectedPerson = value;
  }
  registerOnChange(fn: (value: PersonSummary | null) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.searchControl.disable() : this.searchControl.enable();
  }
}
