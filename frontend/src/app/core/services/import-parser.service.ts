import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ImportParser } from "../../models/transaction.model";

@Injectable({ providedIn: "root" })
export class ImportParserService {
  private readonly base = "/api/v1/import-parsers";

  constructor(private http: HttpClient) {}

  getAll(): Observable<ImportParser[]> {
    return this.http.get<ImportParser[]>(this.base);
  }
}
