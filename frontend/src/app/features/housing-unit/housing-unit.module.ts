import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { HousingUnitRoutingModule } from './housing-unit-routing.module';
import { HousingUnitListComponent } from './components/housing-unit-list/housing-unit-list.component';
import { HousingUnitFormComponent } from './components/housing-unit-form/housing-unit-form.component';
import { HousingUnitDetailsComponent } from './components/housing-unit-details/housing-unit-details.component';

@NgModule({
  declarations: [
    HousingUnitListComponent,
    HousingUnitFormComponent,
    HousingUnitDetailsComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    HousingUnitRoutingModule,
  ],
  exports: [
    HousingUnitListComponent, // exported so BuildingDetailsComponent can embed it
  ],
})
export class HousingUnitModule {}
