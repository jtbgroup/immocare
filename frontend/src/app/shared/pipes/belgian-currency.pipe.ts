import { Pipe, PipeTransform } from "@angular/core";

/**
 * Formats a number in Belgian locale:
 *   - thousands separator: . (dot)
 *   - decimal separator:   , (comma)
 *   - symbol: €
 *
 * Usage:
 *   {{ amount | belgianCurrency }}           → "1.234,56 €"
 *   {{ amount | belgianCurrency : false }}   → "1.234,56"  (no symbol)
 *   {{ amount | belgianCurrency : true : '+' }} → "+1.234,56 €"
 */
@Pipe({
  name: "belgianCurrency",
  standalone: true,
  pure: true,
})
export class BelgianCurrencyPipe implements PipeTransform {
  private static formatter = new Intl.NumberFormat("fr-BE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  transform(
    value: number | null | undefined,
    showSymbol = true,
    prefix = "",
  ): string {
    if (value == null) return "—";
    const formatted = BelgianCurrencyPipe.formatter.format(Math.abs(value));
    const sign = prefix || (value < 0 ? "-" : "");
    return showSymbol ? `${sign}${formatted} €` : `${sign}${formatted}`;
  }
}
