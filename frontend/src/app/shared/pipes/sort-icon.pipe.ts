import { Pipe, PipeTransform } from "@angular/core";

/**
 * Displays a sort direction icon for a given column.
 *
 * Usage: {{ activeField | sortIcon: sortDirection : 'columnField' }}
 *
 * Returns:
 *   ↑  when column is active and direction is 'asc'
 *   ↓  when column is active and direction is 'desc'
 *   ↕  when column is not the active sort column
 */
@Pipe({
  name: "sortIcon",
  standalone: true,
  pure: true,
})
export class SortIconPipe implements PipeTransform {
  transform(
    activeField: string,
    direction: "asc" | "desc",
    column: string,
  ): string {
    if (activeField !== column) return "↕";
    return direction === "asc" ? "↑" : "↓";
  }
}
