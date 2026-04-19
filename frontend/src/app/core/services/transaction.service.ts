// core/services/transaction.service.ts — UC016 Phase 4
// All transaction endpoints are now estate-scoped via ActiveEstateService.
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from './active-estate.service';
import {
  ConfirmTransactionRequest,
  CreateTransactionRequest,
  FinancialTransaction,
  ImportBatchResult,
  ImportPreviewRow,
  ImportRowEnrichment,
  PagedTransactionResponse,
  StatisticsFilter,
  TransactionFilter,
  TransactionStatistics,
} from '../../models/transaction.model';

const GLOBAL_BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get base(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return estateId
      ? `${GLOBAL_BASE}/estates/${estateId}/transactions`
      : `${GLOBAL_BASE}/transactions`;
  }

  getTransactions(filter: TransactionFilter): Observable<PagedTransactionResponse> {
    return this.http.get<PagedTransactionResponse>(this.base, {
      params: this.buildParams(filter),
    });
  }

  getById(id: number): Observable<FinancialTransaction> {
    return this.http.get<FinancialTransaction>(`${this.base}/${id}`);
  }

  create(req: CreateTransactionRequest): Observable<FinancialTransaction> {
    return this.http.post<FinancialTransaction>(this.base, req);
  }

  update(id: number, req: CreateTransactionRequest): Observable<FinancialTransaction> {
    return this.http.put<FinancialTransaction>(`${this.base}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  confirm(id: number, req: ConfirmTransactionRequest): Observable<FinancialTransaction> {
    return this.http.patch<FinancialTransaction>(`${this.base}/${id}/confirm`, req);
  }

  confirmBatch(batchId: number): Observable<{ confirmedCount: number }> {
    return this.http.post<{ confirmedCount: number }>(`${this.base}/confirm-batch`, { batchId });
  }

  bulkPatch(req: {
    ids: number[];
    status?: string;
    subcategoryId?: number;
  }): Observable<{ updatedCount: number; skippedCount: number }> {
    return this.http.patch<{ updatedCount: number; skippedCount: number }>(
      `${this.base}/bulk`,
      req,
    );
  }

  getStatistics(filter: StatisticsFilter): Observable<TransactionStatistics> {
    return this.http.get<TransactionStatistics>(`${this.base}/statistics`, {
      params: this.buildParams(filter as any),
    });
  }

  exportCsv(filter: TransactionFilter): Observable<Blob> {
    return this.http.get(`${this.base}/export`, {
      params: this.buildParams(filter),
      responseType: 'blob',
    });
  }

  /**
   * Step 1 — Preview: parse without persisting.
   * Preview uses the global endpoint (no estate scope needed for parsing).
   */
  previewFile(file: File, parserCode: string): Observable<ImportPreviewRow[]> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('parserCode', parserCode);
    return this.http.post<ImportPreviewRow[]>(`${this.base}/preview`, formData);
  }

  /**
   * Step 2 — Import: parse + apply enrichments + persist.
   */
  importFile(
    file: File,
    parserCode: string,
    bankAccountId: number | null,
    enrichments: ImportRowEnrichment[] = [],
    selectedFingerprints: string[] = [],
  ): Observable<ImportBatchResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('parserCode', parserCode);
    if (bankAccountId != null) {
      formData.append('bankAccountId', String(bankAccountId));
    }
    if (enrichments.length > 0) {
      formData.append('enrichments', JSON.stringify(enrichments));
    }
    if (selectedFingerprints.length > 0) {
      formData.append('selectedFingerprints', JSON.stringify(selectedFingerprints));
    }
    return this.http.post<ImportBatchResult>(`${this.base}/import`, formData);
  }

  getBatch(
    batchId: number,
    page = 0,
    size = 200,
    sort = 'transactionDate,asc',
  ): Observable<PagedTransactionResponse> {
    return this.http.get<PagedTransactionResponse>(this.base, {
      params: new HttpParams()
        .set('importBatchId', batchId)
        .set('page', page)
        .set('size', size)
        .set('sort', sort),
    });
  }

  private buildParams(filter: Record<string, any>): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
