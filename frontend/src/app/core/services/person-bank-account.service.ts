import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PersonBankAccount, SavePersonBankAccountRequest } from '../../models/person.model';

@Injectable({ providedIn: 'root' })
export class PersonBankAccountService {

  constructor(private http: HttpClient) {}

  private base(personId: number): string {
    return `/api/v1/persons/${personId}/bank-accounts`;
  }

  getAll(personId: number): Observable<PersonBankAccount[]> {
    return this.http.get<PersonBankAccount[]>(this.base(personId));
  }

  create(personId: number, req: SavePersonBankAccountRequest): Observable<PersonBankAccount> {
    return this.http.post<PersonBankAccount>(this.base(personId), req);
  }

  update(personId: number, id: number, req: SavePersonBankAccountRequest): Observable<PersonBankAccount> {
    return this.http.put<PersonBankAccount>(`${this.base(personId)}/${id}`, req);
  }

  delete(personId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base(personId)}/${id}`);
  }
}
