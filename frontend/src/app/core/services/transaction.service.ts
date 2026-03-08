import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
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
} from "../../models/transaction.model";

const BASE = "/api/v1/transactions";

@Injectable({ providedIn: "root" })
export class TransactionService {
  constructor(private http: HttpClient) {}

  getTransactions(
    filter: TransactionFilter,
  ): Observable<PagedTransactionResponse> {
    return this.http.get<PagedTransactionResponse>(BASE, {
      params: this.buildParams(filter),
    });
  }

  getById(id: number): Observable<FinancialTransaction> {
    return this.http.get<FinancialTransaction>(`${BASE}/${id}`);
  }

  create(req: CreateTransactionRequest): Observable<FinancialTransaction> {
    return this.http.post<FinancialTransaction>(BASE, req);
  }

  update(
    id: number,
    req: CreateTransactionRequest,
  ): Observable<FinancialTransaction> {
    return this.http.put<FinancialTransaction>(`${BASE}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  confirm(
    id: number,
    req: ConfirmTransactionRequest,
  ): Observable<FinancialTransaction> {
    return this.http.patch<FinancialTransaction>(`${BASE}/${id}/confirm`, req);
  }

  confirmBatch(batchId: number): Observable<{ confirmedCount: number }> {
    return this.http.post<{ confirmedCount: number }>(`${BASE}/confirm-batch`, {
      batchId,
    });
  }

  getStatistics(filter: StatisticsFilter): Observable<TransactionStatistics> {
    return this.http.get<TransactionStatistics>(`${BASE}/statistics`, {
      params: this.buildParams(filter as any),
    });
  }

  exportCsv(filter: TransactionFilter): Observable<Blob> {
    return this.http.get(`${BASE}/export`, {
      params: this.buildParams(filter),
      responseType: "blob",
    });
  }

  /**
   * Step 1 — Preview: parse without persisting.
   * Returns rows enriched with duplicate flag, subcategory and lease suggestions.
   */
  previewFile(file: File, parserCode: string): Observable<ImportPreviewRow[]> {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("parserCode", parserCode);
    return this.http.post<ImportPreviewRow[]>(`${BASE}/preview`, formData);
  }

  /**
   * Step 2 — Import: parse + apply enrichments + persist as DRAFT or CONFIRMED.
   */
  importFile(
    file: File,
    parserCode: string,
    bankAccountId: number | null,
    enrichments: ImportRowEnrichment[] = [],
    selectedFingerprints: string[] = [],
  ): Observable<ImportBatchResult> {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("parserCode", parserCode);
    if (bankAccountId != null) {
      formData.append("bankAccountId", String(bankAccountId));
    }
    if (enrichments.length > 0) {
      formData.append("enrichments", JSON.stringify(enrichments));
    }
    if (selectedFingerprints.length > 0) {
      formData.append(
        "selectedFingerprints",
        JSON.stringify(selectedFingerprints),
      );
    }
    return this.http.post<ImportBatchResult>(`${BASE}/import`, formData);
  }

  getBatch(
    batchId: number,
    page = 0,
    size = 200,
  ): Observable<PagedTransactionResponse> {
    return this.http.get<PagedTransactionResponse>(BASE, {
      params: new HttpParams()
        .set("importBatchId", batchId)
        .set("page", page)
        .set("size", size)
        .set("sort", "transactionDate,asc"),
    });
  }

  private buildParams(filter: Record<string, any>): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== "") {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
