// features/person/person.module.ts
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PersonRoutingModule } from './person-routing.module';

// Standalone components are imported directly via routing.
// This module serves as the lazy-loading boundary.

@NgModule({
  imports: [
    CommonModule,
    PersonRoutingModule
  ]
})
export class PersonModule {}
