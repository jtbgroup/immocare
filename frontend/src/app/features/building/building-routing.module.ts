import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BuildingListComponent } from './components/building-list/building-list.component';
import { BuildingFormComponent } from './components/building-form/building-form.component';
import { BuildingDetailsComponent } from './components/building-details/building-details.component';

const routes: Routes = [
  {
    path: '',
    component: BuildingListComponent
  },
  {
    path: 'new',
    component: BuildingFormComponent
  },
  {
    path: ':id',
    component: BuildingDetailsComponent
  },
  {
    path: ':id/edit',
    component: BuildingFormComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BuildingRoutingModule {}
