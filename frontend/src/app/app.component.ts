import { CommonModule } from "@angular/common";
import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  ViewChild,
} from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { AuthService } from "./core/auth/auth.service";
import { ActiveEstateService } from "./core/services/active-estate.service";
import { AlertService } from "./core/services/alert.service";
import { VersionService } from "./core/services/version.service";
import { EstateHeaderComponent } from "./features/estate/components/estate-header/estate-header.component";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    EstateHeaderComponent,
  ],
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
})
export class AppComponent implements OnInit {
  version = this.versionService.getVersion();
  alertCount = 0;
  dropdownOpen = false;
  managementOpen = false;

  currentUser$ = this.authService.currentUser$;

  @ViewChild("avatarWrapper") avatarWrapper!: ElementRef;
  @ViewChild("managementWrapper") managementWrapper!: ElementRef;

  constructor(
    private authService: AuthService,
    private versionService: VersionService,
    private alertService: AlertService,
    private router: Router,
    readonly activeEstateService: ActiveEstateService,
  ) {}

  ngOnInit(): void {
    this.authService.getCurrentUser().subscribe();

    this.currentUser$.subscribe((user) => {
      if (user) {
        this.alertService.getCount().subscribe({
          next: (count) => (this.alertCount = count),
          error: () => (this.alertCount = 0),
        });
      } else {
        this.alertCount = 0;
      }
    });
  }

  userInitials(username: string): string {
    if (!username) return "?";
    return username.slice(0, 2).toUpperCase();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  toggleManagement(): void {
    this.managementOpen = !this.managementOpen;
  }

  closeManagement(): void {
    this.managementOpen = false;
  }

  isManagementActive(): boolean {
    const url = this.router.url;
    return url.startsWith("/persons") || url.startsWith("/bank-accounts");
  }

  @HostListener("document:click", ["$event"])
  onDocumentClick(event: MouseEvent): void {
    if (
      this.dropdownOpen &&
      this.avatarWrapper &&
      !this.avatarWrapper.nativeElement.contains(event.target)
    ) {
      this.dropdownOpen = false;
    }
    if (
      this.managementOpen &&
      this.managementWrapper &&
      !this.managementWrapper.nativeElement.contains(event.target)
    ) {
      this.managementOpen = false;
    }
  }

  logout(): void {
    this.dropdownOpen = false;
    this.authService.logout();
  }
}
