// core/services/tag-subcategory.service.ts — UC016 Phase 4
// Tag subcategories are now estate-scoped (via tag_category.estate_id).
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from './active-estate.service';
import {
  SaveTagSubcategoryRequest,
  SubcategoryDirection,
  TagSubcategory,
} from '../../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TagSubcategoryService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get base(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return estateId
      ? `/api/v1/estates/${estateId}/tag-subcategories`
      : `/api/v1/tag-subcategories`;
  }

  getAll(
    categoryId?: number,
    direction?: SubcategoryDirection,
  ): Observable<TagSubcategory[]> {
    let params = new HttpParams();
    if (categoryId != null) params = params.set('categoryId', categoryId);
    if (direction) params = params.set('direction', direction);
    return this.http.get<TagSubcategory[]>(this.base, { params });
  }

  create(req: SaveTagSubcategoryRequest): Observable<TagSubcategory> {
    return this.http.post<TagSubcategory>(this.base, req);
  }

  update(id: number, req: SaveTagSubcategoryRequest): Observable<TagSubcategory> {
    return this.http.put<TagSubcategory>(`${this.base}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
