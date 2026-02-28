import { CommonModule } from "@angular/common";
import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  ViewChild,
} from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { AuthService } from "./core/auth/auth.service";
import { LeaseService } from "./core/services/lease.service";
import { VersionService } from "./core/services/version.service";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
})
export class AppComponent implements OnInit {
  version = "…";
  alertCount = 0;
  dropdownOpen = false;

  currentUser$ = this.authService.currentUser$;

  @ViewChild("avatarWrapper") avatarWrapper!: ElementRef;

  constructor(
    private authService: AuthService,
    private versionService: VersionService,
    private leaseService: LeaseService,
  ) {}

  ngOnInit(): void {
    this.versionService.getVersion().subscribe((v) => (this.version = v));

    // Initialise the currentUser$ stream on app load
    this.authService.getCurrentUser().subscribe();

    // Load alert count whenever the logged-in user changes
    this.currentUser$.subscribe((user) => {
      if (user) {
        this.leaseService.getAlerts().subscribe({
          next: (alerts) => (this.alertCount = alerts.length),
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

  @HostListener("document:click", ["$event"])
  onDocumentClick(event: MouseEvent): void {
    if (
      this.dropdownOpen &&
      this.avatarWrapper &&
      !this.avatarWrapper.nativeElement.contains(event.target)
    ) {
      this.dropdownOpen = false;
    }
  }

  logout(): void {
    this.dropdownOpen = false;
    this.authService.logout();
  }
}
