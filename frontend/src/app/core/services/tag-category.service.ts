import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  SaveTagCategoryRequest,
  TagCategory,
} from "../../models/transaction.model";

const BASE = "/api/v1/tag-categories";

@Injectable({ providedIn: "root" })
export class TagCategoryService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<TagCategory[]> {
    return this.http.get<TagCategory[]>(BASE);
  }

  create(req: SaveTagCategoryRequest): Observable<TagCategory> {
    return this.http.post<TagCategory>(BASE, req);
  }

  update(id: number, req: SaveTagCategoryRequest): Observable<TagCategory> {
    return this.http.put<TagCategory>(`${BASE}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
