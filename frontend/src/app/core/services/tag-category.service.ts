// core/services/tag-category.service.ts — UC016 Phase 4
// Tag categories are now estate-scoped.
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from './active-estate.service';
import { SaveTagCategoryRequest, TagCategory } from '../../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TagCategoryService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get base(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return estateId
      ? `/api/v1/estates/${estateId}/tag-categories`
      : `/api/v1/tag-categories`;
  }

  getAll(): Observable<TagCategory[]> {
    return this.http.get<TagCategory[]>(this.base);
  }

  create(req: SaveTagCategoryRequest): Observable<TagCategory> {
    return this.http.post<TagCategory>(this.base, req);
  }

  update(id: number, req: SaveTagCategoryRequest): Observable<TagCategory> {
    return this.http.put<TagCategory>(`${this.base}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
