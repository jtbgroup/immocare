import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  BankAccount,
  SaveBankAccountRequest,
} from "../../models/transaction.model";

const BASE = "/api/v1/bank-accounts";

@Injectable({ providedIn: "root" })
export class BankAccountService {
  constructor(private http: HttpClient) {}

  getAll(activeOnly = false): Observable<BankAccount[]> {
    const params = new HttpParams().set("activeOnly", activeOnly);
    return this.http.get<BankAccount[]>(BASE, { params });
  }

  create(req: SaveBankAccountRequest): Observable<BankAccount> {
    return this.http.post<BankAccount>(BASE, req);
  }

  update(id: number, req: SaveBankAccountRequest): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${BASE}/${id}`, req);
  }
}
