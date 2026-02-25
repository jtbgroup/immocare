import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { debounceTime, distinctUntilChanged, Subject } from "rxjs";
import { PersonService } from "../../../core/services/person.service";
import { PersonSummary } from "../../../models/person.model";
import { SortIconPipe } from "../../../shared/pipes/sort-icon.pipe";

@Component({
  selector: "app-person-list",
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SortIconPipe],
  templateUrl: "./person-list.component.html",
  styleUrls: ["./person-list.component.scss"],
})
export class PersonListComponent implements OnInit {
  persons: PersonSummary[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  sort = "lastName,asc";
  sortField = "lastName";
  sortDirection: "asc" | "desc" = "asc";
  searchTerm = "";
  isLoading = false;
  errorMessage = "";

  private searchSubject = new Subject<string>();

  constructor(private personService: PersonService) {}

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.currentPage = 0;
        this.loadPersons();
      });
    this.loadPersons();
  }

  loadPersons(): void {
    this.isLoading = true;
    this.errorMessage = "";
    this.personService
      .getAll(this.currentPage, this.pageSize, this.sort, this.searchTerm)
      .subscribe({
        next: (page) => {
          this.persons = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.isLoading = false;
        },
        error: () => {
          this.errorMessage = "Failed to load persons. Please try again.";
          this.isLoading = false;
        },
      });
  }

  onSearch(): void {
    this.searchSubject.next(this.searchTerm);
  }

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortField = field;
      this.sortDirection = "asc";
    }
    this.sort = `${this.sortField},${this.sortDirection}`;
    this.currentPage = 0;
    this.loadPersons();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadPersons();
    }
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }
}
