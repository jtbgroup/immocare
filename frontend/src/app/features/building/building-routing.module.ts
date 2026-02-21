import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BuildingListComponent } from './components/building-list/building-list.component';
import { BuildingFormComponent } from './components/building-form/building-form.component';
import { BuildingDetailsComponent } from './components/building-details/building-details.component';
import { AuthGuard } from '../../core/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: BuildingListComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'new',
    component: BuildingFormComponent,
    canActivate: [AuthGuard]
  },
  {
    path: ':id',
    component: BuildingDetailsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: ':id/edit',
    component: BuildingFormComponent,
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BuildingRoutingModule {}
