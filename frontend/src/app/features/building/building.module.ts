import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BuildingRoutingModule } from './building-routing.module';
import { BuildingListComponent } from './components/building-list/building-list.component';
import { BuildingFormComponent } from './components/building-form/building-form.component';
import { BuildingDetailsComponent } from './components/building-details/building-details.component';

/**
 * Feature module for Building management.
 * Contains all components related to UC001 - Manage Buildings.
 */
@NgModule({
  declarations: [
    BuildingListComponent,
    BuildingFormComponent,
    BuildingDetailsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    BuildingRoutingModule
  ]
})
export class BuildingModule {}
