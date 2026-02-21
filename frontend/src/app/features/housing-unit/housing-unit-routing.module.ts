// housing-unit-routing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HousingUnitFormComponent } from './components/housing-unit-form/housing-unit-form.component';
import { HousingUnitDetailsComponent } from './components/housing-unit-details/housing-unit-details.component';

const routes: Routes = [
  { path: 'new',       component: HousingUnitFormComponent },
  { path: ':id',       component: HousingUnitDetailsComponent },
  { path: ':id/edit',  component: HousingUnitFormComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class HousingUnitRoutingModule {}
