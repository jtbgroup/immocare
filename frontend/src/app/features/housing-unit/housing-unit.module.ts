import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";

import { HousingUnitDetailsComponent } from "./components/housing-unit-details/housing-unit-details.component";
import { HousingUnitFormComponent } from "./components/housing-unit-form/housing-unit-form.component";
import { HousingUnitListComponent } from "./components/housing-unit-list/housing-unit-list.component";
import { HousingUnitRoutingModule } from "./housing-unit-routing.module";

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
