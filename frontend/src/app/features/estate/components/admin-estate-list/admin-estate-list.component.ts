// features/estate/components/admin-estate-list/admin-estate-list.component.ts — UC016 US092-US095
import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { debounceTime, distinctUntilChanged, takeUntil } from "rxjs/operators";
import { ActiveEstateService } from "../../../../core/services/active-estate.service";
import { EstateService } from "../../../../core/services/estate.service";
import { Estate } from "../../../../models/estate.model";
import { Page } from "../../../../models/page.model";

@Component({
  selector: "app-admin-estate-list",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./admin-estate-list.component.html",
  styleUrls: ["./admin-estate-list.component.scss"],
})
export class AdminEstateListComponent implements OnInit, OnDestroy {
  estates: Estate[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  searchTerm = "";
  loading = false;
  error: string | null = null;

  deleteConfirmId: string | null = null;
  deleteError: string | null = null;
  deleting = false;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private estateService: EstateService,
    private activeEstateService: ActiveEstateService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        this.searchTerm = term;
        this.currentPage = 0;
        this.load();
      });

    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.estateService
      .getAllEstates(
        this.currentPage,
        this.pageSize,
        this.searchTerm || undefined,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: Page<Estate>) => {
          this.estates = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.number;
          this.loading = false;
        },
        error: () => {
          this.error = "Failed to load estates.";
          this.loading = false;
        },
      });
  }

  onSearchChange(term: string): void {
    this.searchSubject.next(term);
  }

  create(): void {
    this.router.navigate(["/admin/estates/new"]);
  }

  edit(estate: Estate): void {
    this.router.navigate(["/admin/estates", estate.id, "edit"]);
  }

  viewDashboard(estate: Estate): void {
    this.activeEstateService.setActiveEstate(estate as any);
    this.router.navigate(["/estates", estate.id, "dashboard"]);
  }

  requestDelete(estate: Estate): void {
    this.deleteConfirmId = estate.id;
    this.deleteError = null;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
    this.deleteError = null;
  }

  confirmDelete(): void {
    if (!this.deleteConfirmId) return;
    this.deleting = true;
    this.deleteError = null;
    this.estateService
      .deleteEstate(this.deleteConfirmId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.deleting = false;
          this.deleteConfirmId = null;
          this.load();
        },
        error: (err) => {
          this.deleting = false;
          const buildingCount = err.error?.buildingCount;
          this.deleteError = buildingCount
            ? `Cannot delete: this estate contains ${buildingCount} building(s).`
            : (err.error?.message ?? "Failed to delete estate.");
          this.deleteConfirmId = null;
        },
      });
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  estateToDelete(): Estate | undefined {
    return this.estates.find((e) => e.id === this.deleteConfirmId);
  }
}
