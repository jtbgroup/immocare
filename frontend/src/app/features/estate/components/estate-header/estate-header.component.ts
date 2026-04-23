// shared/components/estate-header/estate-header.component.ts — UC004_ESTATE_PLACEHOLDER UC003.010 AC4
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ActiveEstateService } from '../../../../core/services/active-estate.service';

@Component({
  selector: 'app-estate-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estate-header.component.html',
  styleUrls: ['./estate-header.component.scss'],
})
export class EstateHeaderComponent {
  constructor(
    readonly activeEstateService: ActiveEstateService,
    private router: Router,
  ) {}

  switchEstate(): void {
    this.activeEstateService.clearActiveEstate();
    this.router.navigate(['/select-estate']);
  }
}
