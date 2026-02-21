import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HousingUnitModule } from "../housing-unit/housing-unit.module";
import { BuildingRoutingModule } from "./building-routing.module";
import { BuildingDetailsComponent } from "./components/building-details/building-details.component";
import { BuildingFormComponent } from "./components/building-form/building-form.component";
import { BuildingListComponent } from "./components/building-list/building-list.component";

/**
 * Feature module for Building management.
 * Contains all components related to UC001 - Manage Buildings.
 */
@NgModule({
  declarations: [
    BuildingListComponent,
    BuildingFormComponent,
    BuildingDetailsComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    BuildingRoutingModule,
    HousingUnitModule, // provides <app-housing-unit-list>
  ],
})
export class BuildingModule {}
