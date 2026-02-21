import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { UserListComponent } from './components/user-list/user-list.component';
import { UserFormComponent } from './components/user-form/user-form.component';
import { UserDetailsComponent } from './components/user-details/user-details.component';

const routes: Routes = [
  { path: '',        component: UserListComponent },
  { path: 'new',     component: UserFormComponent },
  { path: ':id',     component: UserDetailsComponent },
  { path: ':id/edit', component: UserFormComponent },
];

@NgModule({
  declarations: [
    UserListComponent,
    UserFormComponent,
    UserDetailsComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
  ],
})
export class UserModule {}
