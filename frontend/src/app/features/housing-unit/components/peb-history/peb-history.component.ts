import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PebScoreService } from '../../../../core/services/peb-score.service';
import {
  PebImprovementDTO,
  PebScoreDTO,
  PEB_SCORE_DISPLAY,
} from '../../../../models/peb-score.model';

@Component({
  selector: 'app-peb-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './peb-history.component.html',
  styleUrls: ['./peb-history.component.css'],
})
export class PebHistoryComponent implements OnInit, OnDestroy {
  @Input() unitId!: number;
  @Output() close = new EventEmitter<void>();

  history: PebScoreDTO[] = [];
  improvement: PebImprovementDTO | null = null;
  loading = false;

  readonly display = PEB_SCORE_DISPLAY;

  private destroy$ = new Subject<void>();

  constructor(private pebScoreService: PebScoreService) {}

  ngOnInit(): void {
    this.loadHistory();
    this.loadImprovements();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadHistory(): void {
    this.loading = true;
    this.pebScoreService.getHistory(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.history = data; this.loading = false; },
        error: () => { this.loading = false; },
      });
  }

  private loadImprovements(): void {
    this.pebScoreService.getImprovements(this.unitId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.improvement = data; },
        error: () => { this.improvement = null; },
      });
  }

  improvementIcon(direction: string): string {
    if (direction === 'IMPROVED') return '↑';
    if (direction === 'DEGRADED') return '↓';
    return '−';
  }

  improvementClass(direction: string): string {
    if (direction === 'IMPROVED') return 'improved';
    if (direction === 'DEGRADED') return 'degraded';
    return 'unchanged';
  }

  statusClass(status: string): string {
    if (status === 'CURRENT') return 'status-current';
    if (status === 'EXPIRED') return 'status-expired';
    return 'status-historical';
  }

  rowDirection(item: PebScoreDTO): string {
    if (!this.improvement?.history) return '';
    const step = this.improvement.history.find(s => s.toScore === item.pebScore);
    return step ? step.direction : '';
  }
}
