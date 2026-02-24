// features/person/person-routing.module.ts
import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";

import { PersonDetailsComponent } from "./person-details/person-details.component";
import { PersonFormComponent } from "./person-form/person-form.component";
import { PersonListComponent } from "./person-list/person-list.component";

const routes: Routes = [
  { path: "", component: PersonListComponent },
  { path: "new", component: PersonFormComponent },
  { path: ":id", component: PersonDetailsComponent },
  { path: ":id/edit", component: PersonFormComponent },
  {
    path: "persons",
    loadChildren: () =>
      import("../person/person.module").then((m) => m.PersonModule),
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PersonRoutingModule {}
