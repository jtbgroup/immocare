# Backlog Immocare

- [x] recherche de tenant : doit se faire sur le nom ou prénom. pour l'instant que sur prénom
- [x] menus header: revoir
- [x] revoir la doc par rapport au rent / lease
- [x] garanties locative type: ajouter dépot sur mon compte
- [ ] user: lier à une personne
- [x] vue qui regroupe tous les contrats
- [x] Person sans NISS ne va pas car NIS est une clé primaire
- [x] housing unit avec juste une terrasse donne une erreur si garden orientation n'est pas rempli
- [x] extincteurs
- [x] suivi des chaudières
- [ ] dépenses
- [x] view des alertes à revoir
- [ ] alertes: est-ce qu'on veut pouvoir les ignorer (de manière individuelle?)
- [ ] ajouter des documents : rapport PEB, entretien chaudière, ...
- [x] revoir les alertes pour les rendre transversales
- [ ] rôles et sécurité
- [x] bug avec les alertes qui ne s'affichent pas
- [ ] bug dans dans le format des dates et les settings des dates (pas visible)



1. dans le formulaire d'édition de la transaction, les buildings, units et lease doivent être des listes liées entre elles et je ne dois pas introduire des id mais sélectionner les valeurs en language compéhensible. 

revoit aussi le layout pour qu'il soit plus en ligne avec les aurtes formulaires

le formulaire et les listes des bank accounts est trop large. il doit prendre la CSS globale pour la largeur

Les Transaction Categories doivent venir dans les settings (menu supplémentaire). Il faut retirer le bouton de l'écran financial transactions. l'écran existant pour la gestion de catalogues est ok mais doit être intégré dans les settings dans un menu de gestion des catalogues
----DONE----


2. Rassemble les flyway 13-15

3. Vérifie la documentation des UC de transaction par rapport au code

4. dans les people, il faut ajouter des bank accounts. cela doit permettre de faire la réconciliation ensuite avec les transactions financières car c'est le seul lien que j'ai.

5. quand j'importe des transactions, je veux pouvoir revoir avant l'import. Je veux pouvoir ne sélectionner que certains imports éventuellement, je veux pouvoir valider directement, je veux directement faire le lien avec un unit ou building ou people. je veux voir les doublons potentiels et les écarter. Il faut aussi que sur base soit d'apprentissage, soit de donnéesconnues (ex bank account des personnes), le lien soit directement fait (ex: vers un lease et donc le unit et le building, un compteur d'eau)

 Si je veux faire des modifications plus tard, je dois avoir une possiblilité de le faire simplement. Par exemple avec des checkboxes dans la liste et changer le statut des sélectionnées. idem pour un delete.

6. une fois importées, j'ai le bouton Réviser les transactions importées, mais cela ne mène à rien. je n'ai pas non plus d'avertissement sur les doublons potentiel avec une décision à prendre.

7. les écrans d'import des transactions sont en FR et devraient être en anglais.

8. rend l'application multilingue avec une architecture simple pour les traductions

9. revois les formats des montants en pattern europe et pas US.

10. dans les app components "Financial" des loyers , je voudrais aussi le montant "expected" entre le début du contrat et maintenant et ce qui a été réellement payé. Dans les units ou buildings, les dépenses et recettes doivent couvrir tout ce qui est lié à chaque appartement. attention qu'il y a parfois des factures groupées. COmment gérer ça?

11. Le financial dashboard ne fonctionne pas
12.  

