// features/person/person-details/person-details.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { PersonService } from '../../../core/services/person.service';
import { Person } from '../../../models/person.model';

@Component({
  selector: 'app-person-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './person-details.component.html',
  styleUrls: ['./person-details.component.scss']
})
export class PersonDetailsComponent implements OnInit {
  person?: Person;
  isLoading = false;
  errorMessage = '';
  showDeleteConfirm = false;
  isDeleting = false;
  deleteError = '';
  deleteErrorDetails?: {
    ownedBuildings: string[];
    ownedUnits: string[];
    activeLeases: string[];
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private personService: PersonService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadPerson(+id);
  }

  loadPerson(id: number): void {
    this.isLoading = true;
    this.personService.getById(id).subscribe({
      next: p => {
        this.person = p;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Person not found.';
        this.isLoading = false;
      }
    });
  }

  confirmDelete(): void {
    this.showDeleteConfirm = true;
    this.deleteError = '';
    this.deleteErrorDetails = undefined;
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  doDelete(): void {
    if (!this.person) return;
    this.isDeleting = true;
    this.personService.delete(this.person.id).subscribe({
      next: () => this.router.navigate(['/persons']),
      error: (err) => {
        this.isDeleting = false;
        if (err.status === 409) {
          this.deleteError = err.error?.message || 'This person cannot be deleted.';
          this.deleteErrorDetails = {
            ownedBuildings: err.error?.ownedBuildings || [],
            ownedUnits:     err.error?.ownedUnits || [],
            activeLeases:   err.error?.activeLeases || []
          };
        } else {
          this.deleteError = 'An error occurred. Please try again.';
        }
        this.showDeleteConfirm = false;
      }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'bg-success',
      DRAFT: 'bg-secondary',
      FINISHED: 'bg-info',
      CANCELLED: 'bg-danger'
    };
    return map[status] || 'bg-secondary';
  }
}
