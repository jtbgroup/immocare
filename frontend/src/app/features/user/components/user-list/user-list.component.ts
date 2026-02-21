import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../models/user.model';

type SortField = keyof Pick<User, 'username' | 'email' | 'role' | 'createdAt'>;
type SortDir   = 'asc' | 'desc';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss'],
})
export class UserListComponent implements OnInit {
  users:        User[]  = [];
  loading       = false;
  errorMessage  = '';

  // Sorting
  sortField: SortField = 'username';
  sortDir:   SortDir   = 'asc';

  // Pagination
  readonly pageSize = 20;
  currentPage       = 1;

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.userService.getAll().subscribe({
      next: (users) => {
        this.users   = users;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load users.';
        this.loading      = false;
      },
    });
  }

  sort(field: SortField): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir   = 'asc';
    }
    this.currentPage = 1;
  }

  get sortedUsers(): User[] {
    return [...this.users].sort((a, b) => {
      const aVal = String(a[this.sortField] ?? '').toLowerCase();
      const bVal = String(b[this.sortField] ?? '').toLowerCase();
      const cmp  = aVal.localeCompare(bVal);
      return this.sortDir === 'asc' ? cmp : -cmp;
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.users.length / this.pageSize));
  }

  get pagedUsers(): User[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedUsers.slice(start, start + this.pageSize);
  }

  get endIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.users.length);
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  navigateTo(user: User): void {
    this.router.navigate(['/users', user.id]);
  }

  create(): void {
    this.router.navigate(['/users/new']);
  }

  sortIcon(field: SortField): string {
    if (this.sortField !== field) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }
}
