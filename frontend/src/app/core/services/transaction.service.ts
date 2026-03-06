import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  ConfirmTransactionRequest,
  CreateTransactionRequest,
  FinancialTransaction,
  ImportBatchResult,
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
    const params = this.buildParams(filter);
    return this.http.get<PagedTransactionResponse>(BASE, { params });
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
    const params = this.buildParams(filter as any);
    return this.http.get<TransactionStatistics>(`${BASE}/statistics`, {
      params,
    });
  }

  exportCsv(filter: TransactionFilter): Observable<Blob> {
    const params = this.buildParams(filter);
    return this.http.get(`${BASE}/export`, { params, responseType: "blob" });
  }

  importCsv(file: File): Observable<ImportBatchResult> {
    const formData = new FormData();
    formData.append("file", file);
    return this.http.post<ImportBatchResult>(`${BASE}/import`, formData);
  }

  getBatch(
    batchId: number,
    page = 0,
    size = 20,
  ): Observable<PagedTransactionResponse> {
    return this.http.get<PagedTransactionResponse>(
      `${BASE}/import/${batchId}`,
      {
        params: new HttpParams().set("page", page).set("size", size),
      },
    );
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
