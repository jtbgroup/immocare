// features/lease/lease-routing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LeaseFormComponent }    from './lease-form/lease-form.component';
import { LeaseDetailsComponent } from './lease-details/lease-details.component';
import { AlertsComponent }       from './alerts/alerts.component';

const routes: Routes = [
  // Create lease for a specific unit
  { path: '',           redirectTo: '/housing-units', pathMatch: 'full' },
  { path: ':id',        component: LeaseDetailsComponent },
  { path: ':id/edit',   component: LeaseFormComponent },
  { path: 'alerts',     component: AlertsComponent }  // must be before :id to avoid conflict
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LeaseRoutingModule {}
