// shared/pipes/app-date.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe }            from '@angular/common';
import { DateFormatService }   from '../../core/services/date-format.service';

/**
 * Formats a date value using the application-wide date format stored in
 * platform_config (key: app.date_format).
 *
 * Usage:
 *   {{ value | appDate }}                 → uses the configured format (e.g. 'dd/MM/yyyy')
 *   {{ value | appDate : 'HH:mm' }}       → appends a time portion: 'dd/MM/yyyy HH:mm'
 *   {{ value | appDate : '' : 'MM/yyyy' }}→ full override with the third argument
 *
 * Parameters:
 *   timeSuffix  (optional) — appended to the date format with a space separator.
 *               Pass an empty string to use only the date format.
 *   fullOverride (optional) — replaces the format entirely (ignores the setting).
 */
@Pipe({
  name: 'appDate',
  standalone: true,
  // Not pure: format may change when the admin updates the setting.
  pure: false,
})
export class AppDatePipe implements PipeTransform {

  private datePipe: DatePipe;

  constructor(private dateFormatService: DateFormatService) {
    // 'en-US' locale is fine — we control the format string ourselves.
    this.datePipe = new DatePipe('en-US');
  }

  transform(
    value: string | Date | null | undefined,
    timeSuffix?: string,
    fullOverride?: string,
  ): string | null {
    if (value == null || value === '') {
      return null;
    }

    let format: string;
    if (fullOverride) {
      format = fullOverride;
    } else {
      format = this.dateFormatService.getFormat();
      if (timeSuffix) {
        format = `${format} ${timeSuffix}`;
      }
    }

    return this.datePipe.transform(value, format);
  }
}
