// core/services/bank-account.service.ts — UC016 Phase 4
// Bank accounts are now estate-scoped.
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from './active-estate.service';
import { BankAccount, SaveBankAccountRequest } from '../../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class BankAccountService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get base(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return estateId
      ? `/api/v1/estates/${estateId}/bank-accounts`
      : `/api/v1/bank-accounts`;
  }

  getAll(activeOnly = false): Observable<BankAccount[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<BankAccount[]>(this.base, { params });
  }

  create(req: SaveBankAccountRequest): Observable<BankAccount> {
    return this.http.post<BankAccount>(this.base, req);
  }

  update(id: number, req: SaveBankAccountRequest): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.base}/${id}`, req);
  }
}
