import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  SaveTagSubcategoryRequest,
  SubcategoryDirection,
  TagSubcategory,
} from "../../models/transaction.model";

const BASE = "/api/v1/tag-subcategories";

@Injectable({ providedIn: "root" })
export class TagSubcategoryService {
  constructor(private http: HttpClient) {}

  getAll(
    categoryId?: number,
    direction?: SubcategoryDirection,
  ): Observable<TagSubcategory[]> {
    let params = new HttpParams();
    if (categoryId != null) params = params.set("categoryId", categoryId);
    if (direction) params = params.set("direction", direction);
    return this.http.get<TagSubcategory[]>(BASE, { params });
  }

  create(req: SaveTagSubcategoryRequest): Observable<TagSubcategory> {
    return this.http.post<TagSubcategory>(BASE, req);
  }

  update(
    id: number,
    req: SaveTagSubcategoryRequest,
  ): Observable<TagSubcategory> {
    return this.http.put<TagSubcategory>(`${BASE}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
