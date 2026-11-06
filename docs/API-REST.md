# API REST

Tous les échanges sont en JSON (UTF-8), les dates au format ISO 8601.

**Authentification** : HTTP Basic sur les trois services (identifiants : variables `SOA_USER` et `SOA_PASSWORD`).

**Erreurs** : corps `{"code": "...", "message": "..."}`.

| Code | Signification |
|---|---|
| 200, 201, 204 | Succès, ressource créée (avec en-tête `Location`), succès sans contenu |
| 400 `VALIDATION` | Donnée absente ou invalide, JSON mal formé |
| 401 `NON_AUTHENTIFIE` | Authentification absente ou incorrecte |
| 404 `INTROUVABLE` | Ressource inexistante |
| 405 | Méthode non prise en charge sur cette adresse |
| 409 `CONFLIT` | Règle de gestion violée : doublon, transition interdite |
| 503 `INDISPONIBLE` | Base de données inaccessible |

## Service 4 : Candidatures (port 8084)

| Méthode et adresse | Corps et paramètres | Réponse |
|---|---|---|
| `POST /api/candidatures` | `idEtudiant`, `idOffre`, `lettreMotivation` (facultatif) | 201 ; 400, 404 (étudiant ou offre inconnus), 409 (doublon) |
| `GET /api/candidatures` | Filtres facultatifs : `idEtudiant`, `idOffre`, `idEntreprise`, `statut` | 200, liste |
| `GET /api/candidatures/{id}` | | 200 ; 404 |
| `PATCH /api/candidatures/{id}/statut` | `statut`, `motif` (facultatif) | 200 ; 400, 404, 409 (transition interdite) |
| `DELETE /api/candidatures/{id}` | | 204 (annulation, idempotente) ; 404, 409 (décision déjà rendue) |

Transitions autorisées :

| Depuis | Vers |
|---|---|
| `DEPOSEE` | `EN_COURS_EVALUATION`, `REFUSEE`, `ANNULEE` |
| `EN_COURS_EVALUATION` | `ACCEPTEE`, `REFUSEE`, `ANNULEE` |
| `ACCEPTEE`, `REFUSEE`, `ANNULEE` | Aucune. Seule une candidature `ANNULEE` peut être redéposée par `POST`. |

```json
{
  "idCandidature": 12,
  "idEtudiant": 1,
  "idOffre": 5,
  "lettreMotivation": "Motivé par ce stage",
  "dateDepot": "2026-11-06T16:52:10",
  "dateMaj": "2026-11-06T16:52:10",
  "statut": "DEPOSEE",
  "motif": null,
  "matricule": "INE-2026-001",
  "nomEtudiant": "Oussama Dahmani",
  "titreOffre": "Ingénieur Cloud Infrastructure",
  "idEntreprise": 3,
  "nomEntreprise": "Capgemini Engineering"
}
```

## Service 5 : Notifications (port 8085)

| Méthode et adresse | Corps et paramètres | Réponse |
|---|---|---|
| `POST /api/notifications` | `type`, `destinataireType` (`ETUDIANT`, `ENTREPRISE`), `destinataireEmail`, `sujet`, `message` ; facultatifs : `destinataireId`, `idCandidature`, `idOffre` | 201 ; 400 |
| `GET /api/notifications` | Filtres facultatifs : `destinataireType`, `destinataireId`, `idCandidature` | 200, liste |
| `GET /api/notifications/{id}` | | 200 ; 404 |
| `POST /api/notifications/rappels?joursAvant=7` | Rappel des offres actives expirant dans ce délai, à l'entreprise et aux étudiants actifs n'ayant pas candidaté, une fois par jour au plus | 200, `{"rappelsEnvoyes": 4}` ; 400 |

`type` : `CONFIRMATION_DEPOT`, `RECEPTION_ENTREPRISE`, `CANDIDATURE_ACCEPTEE`, `CANDIDATURE_REFUSEE`, `CANDIDATURE_REJETEE`, `DEMANDE_COMPLEMENT`, `CANDIDATURE_ANNULEE`, `RAPPEL_DATE_LIMITE`.

L'envoi est simulé : la notification est conservée et journalisée avec `statutEnvoi` à `SIMULEE`.

## Service 6 : Stages (port 8086)

| Méthode et adresse | Corps et paramètres | Réponse |
|---|---|---|
| `POST /api/stages` | `idCandidature`, `dateDebut`, `dateFin` | 201 ; 400 (dates), 404, 409 (candidature non acceptée ou stage déjà créé) |
| `GET /api/stages` | Filtres facultatifs : `idEtudiant` (historique d'un étudiant), `idCandidature`, `statut` | 200, liste |
| `GET /api/stages/{id}` | | 200 ; 404 |
| `PATCH /api/stages/{id}` | `statut`, `dateDebut`, `dateFin` (tous facultatifs) | 200 ; 400, 404, 409 (transition interdite) |
| `POST /api/stages/{id}/comptes-rendus` | `periode`, `contenu`, `auteur` | 201 ; 400, 404, 409 (stage pas en cours) |
| `GET /api/stages/{id}/comptes-rendus` | | 200, liste ; 404 |
| `POST /api/stages/{id}/evaluations` | `note` (0 à 20), `evaluateur`, `commentaire` (facultatif) | 201 ; 400, 404, 409 (stage ni en cours ni terminé) |
| `GET /api/stages/{id}/evaluations` | | 200, liste ; 404 |

Transitions : `PLANIFIE` vers `EN_COURS` ou `INTERROMPU` ; `EN_COURS` vers `TERMINE` ou `INTERROMPU`.

## API façade et pilotage du processus (port 8090)

Cette API sert l'interface web, qui n'a ainsi ni à parler SOAP ni à détenir les identifiants des services. Elle exige une session, portée par le cookie `SESSION_STAGES` (HttpOnly, SameSite=Strict, valable 8 heures). Seules `POST /api/auth/connexion`, `POST /api/auth/inscription` et `GET /api/sante` sont publiques.

| Méthode et adresse | Corps | Réponse |
|---|---|---|
| `POST /api/auth/connexion` | `identifiant`, `motDePasse` | 200, identité et rôle, cookie de session ; 400 ; 401 ; 429 après 5 échecs (blocage d'une minute) |
| `POST /api/auth/inscription` | `role` (`ETUDIANT`, `ENTREPRISE`), `motDePasse` (8 caractères minimum) ; étudiant : `matricule`, `nom`, `filiere`, `niveauEtudes`, `email`, `telephone` ; entreprise : `nom`, `secteurActivite`, `responsableContact`, `email`, `telephone` | 201, identité et rôle, cookie de session ; 400 ; 409 (matricule, e-mail ou nom déjà pris) |
| `GET /api/auth/moi` | | 200, identité et rôle ; 401 |
| `POST /api/auth/deconnexion` | | 204 |

Droits par rôle (403 sinon) :

| Rôle | Peut |
|---|---|
| `ETUDIANT` | Consulter les offres, son profil, ses candidatures, stages et notifications ; déposer en son nom ; annuler ses candidatures ; déposer un compte rendu sur son stage |
| `ENTREPRISE` | Consulter les offres ; gérer ses offres ; voir ses dossiers, candidatures reçues et notifications ; rendre ses décisions |
| `RESPONSABLE` | Tout, dont les incidents, l'historique, les rappels, le statut et l'évaluation des stages |

Pour un étudiant ou une entreprise, les filtres `idEtudiant`, `idEntreprise` et `matricule` envoyés sont ignorés : la session les impose.

Pilotage du processus :

| Méthode et adresse | Corps | Réponse |
|---|---|---|
| `POST /api/processus/candidatures` | `matricule`, `idOffre`, `lettreMotivation` (facultatif) | 201, état du processus ; 400 |
| `GET /api/processus` | Filtre facultatif : `matricule` | 200, liste |
| `GET /api/processus/{id}` | | 200, état et activités parcourues ; 404 |
| `GET /api/processus/{id}/diagramme` | Paramètre `surligner=false` pour un diagramme vierge | 200, image PNG ; 404 |
| `GET /api/processus/taches` | Filtre facultatif : `idEntreprise` | 200, dossiers en attente de décision |
| `POST /api/processus/taches/{id}/decision` | `decision` (`ACCEPTEE`, `REFUSEE`, `COMPLEMENT`), `motif`, `dateDebut` | 200, état ; 400, 404 |
| `GET /api/processus/incidents` | | 200, incidents ouverts |
| `POST /api/processus/incidents/{id}/resolution` | `action` (`RELANCER`, `ABANDONNER`) | 200, état ; 400, 404 |

Le champ `etat` vaut `EN_ATTENTE_DECISION`, `INCIDENT`, `REJETEE`, `ACCEPTEE`, `REFUSEE`, `ANNULEE` ou `ABANDONNEE`.

La façade expose aussi en JSON : `GET /api/etudiants`, `GET /api/etudiants/{matricule}`, `GET /api/entreprises`, `GET /api/entreprises/{id}/offres`, `GET` et `POST /api/offres`, `PUT` et `DELETE /api/offres/{id}`, `GET /api/candidatures`, `DELETE /api/candidatures/{id}` (qui prévient le processus en attente), `GET /api/notifications`, `POST /api/notifications/rappels`, `GET /api/stages`, `PATCH /api/stages/{id}` (qui rend l'étudiant `ACTIF` à la fin du stage), les comptes rendus et évaluations d'un stage, et `GET /api/sante`.
