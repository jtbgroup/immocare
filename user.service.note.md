# NOTE — UserService : getAll() avec search param

Le composant AdminEstateFormComponent et EstateMemberListComponent utilisent
`UserService.getAll()` pour la recherche d'utilisateurs dans les pickers.

La méthode existante dans `user.service.ts` retourne `Observable<User[]>`.
Elle ne supporte pas encore de paramètre `search`.

**Option A (recommandée)** — Ajouter un paramètre optionnel `search` à `getAll()` :

```typescript
// Dans user.service.ts — modifier la méthode getAll()
getAll(search?: string): Observable<User[]> {
  let params = new HttpParams();
  if (search?.trim()) params = params.set('search', search.trim());
  return this.http.get<User[]>(API, { params });
}
```

Le backend `/api/v1/users` devra également supporter le paramètre `search`.
Si ce n'est pas encore le cas, le filtrage se fait côté frontend (solution B).

**Option B (fallback)** — Filtrage côté frontend (déjà implémenté dans les composants) :
Les composants récupèrent tous les users puis filtrent localement.
C'est acceptable pour de petites bases d'utilisateurs (< 100).
