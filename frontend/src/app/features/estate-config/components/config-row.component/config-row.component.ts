import { CommonModule } from "@angular/common";
import { Component, input } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms"; // <-- Ajout de FormControl

@Component({
  selector: "app-config-row",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: "./config-row.component.html",
  styleUrls: ["./config-row.component.scss"],
})
export class ConfigRowComponent {
  // required inputs
  id = input.required<string>();
  control = input.required<FormControl>();

  // optional inputs with default values
  label = input<string>("");
  valueType = input<"string" | "number" | "boolean">("string");

  // optional inputs without default values (thus undefined by default)
  unit = input<string | undefined>(undefined);
  tooltip = input<string | undefined>(undefined);
  min = input<number | undefined>(undefined);
  max = input<number | undefined>(undefined);
}
