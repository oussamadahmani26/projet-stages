# Opérations SOAP

Les trois services sont en SOAP 1.1, style document/literal, décrits par un WSDL public.

**Sécurité** : le WSDL est lisible sans authentification ; toute opération exige HTTP Basic (401 sinon), et HTTPS s'active par configuration.

**Erreurs** : une erreur métier produit une faute SOAP (HTTP 500) sans trace technique. L'élément `code` du détail vaut `VALIDATION`, `INTROUVABLE`, `CONFLIT` ou `INDISPONIBLE`.

```xml
<S:Fault>
  <faultcode>S:Server</faultcode>
  <faultstring>Aucun étudiant avec le matricule NOPE.</faultstring>
  <detail>
    <ns2:ServiceException xmlns:ns2="http://soa.inpt.ma/etudiants">
      <code>INTROUVABLE</code>
      <message>Aucun étudiant avec le matricule NOPE.</message>
    </ns2:ServiceException>
  </detail>
</S:Fault>
```

## Service 1 : Étudiants

WSDL : `http://localhost:8081/ws/etudiants?wsdl`, espace de noms `http://soa.inpt.ma/etudiants`.

| Opération | Paramètres | Résultat | Erreurs |
|---|---|---|---|
| `ajouterEtudiant` | `etudiant` | `etudiant` créé, statut `ACTIF` par défaut | VALIDATION, CONFLIT (matricule ou e-mail déjà pris) |
| `consulterEtudiant` | `matricule` | `etudiant` | INTROUVABLE |
| `listerEtudiants` | | liste de `etudiant` | |
| `modifierEtudiant` | `etudiant` | `etudiant` modifié (le matricule identifie la ligne, le statut n'est pas modifié) | VALIDATION, INTROUVABLE, CONFLIT |
| `supprimerEtudiant` | `matricule` | | INTROUVABLE, CONFLIT (candidatures ou stages existants) |
| `verifierEtudiant` | `matricule` | `verification` : `existe`, `eligible`, `statut`, `motif` | |
| `changerStatutEtudiant` | `matricule`, `statut` | `etudiant` | VALIDATION, INTROUVABLE |

`etudiant` : `matricule`, `nom`, `filiere`, `niveauEtudes`, `email`, `telephone` (facultatif), `statut` (`ACTIF`, `SUSPENDU`, `EN_STAGE`). Seul un étudiant `ACTIF` est éligible.

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:e="http://soa.inpt.ma/etudiants">
  <soapenv:Body>
    <e:verifierEtudiant><matricule>INE-2026-005</matricule></e:verifierEtudiant>
  </soapenv:Body>
</soapenv:Envelope>
```

Réponse :

```xml
<ns2:verifierEtudiantResponse xmlns:ns2="http://soa.inpt.ma/etudiants">
  <verification>
    <existe>true</existe>
    <eligible>false</eligible>
    <statut>SUSPENDU</statut>
    <motif>Statut de l'étudiant : SUSPENDU.</motif>
  </verification>
</ns2:verifierEtudiantResponse>
```

## Service 2 : Entreprises

WSDL : `http://localhost:8082/ws/entreprises?wsdl`, espace de noms `http://soa.inpt.ma/entreprises`.

| Opération | Paramètres | Résultat | Erreurs |
|---|---|---|---|
| `ajouterEntreprise` | `entreprise` | `entreprise` créée | VALIDATION, CONFLIT (nom déjà pris) |
| `consulterEntreprise` | `idEntreprise` | `entreprise` | INTROUVABLE |
| `listerEntreprises` | | liste de `entreprise` | |
| `modifierEntreprise` | `entreprise` avec `idEntreprise` | `entreprise` modifiée | VALIDATION, INTROUVABLE, CONFLIT |
| `supprimerEntreprise` | `idEntreprise` | | INTROUVABLE, CONFLIT (offres existantes) |
| `verifierEntreprise` | `idEntreprise` | `referencee` (booléen) | VALIDATION |
| `consulterOffresEntreprise` | `idEntreprise` | liste de `offre` | INTROUVABLE |

`entreprise` : `nom`, `secteurActivite`, `email`, `telephone` (facultatif), `responsableContact`.

## Service 3 : Offres de stage

WSDL : `http://localhost:8083/ws/offres?wsdl`, espace de noms `http://soa.inpt.ma/offres`.

| Opération | Paramètres | Résultat | Erreurs |
|---|---|---|---|
| `creerOffre` | `offre` | `offre` créée, statut `ACTIVE` | VALIDATION (dont date limite passée), CONFLIT (entreprise inconnue) |
| `consulterOffre` | `idOffre` | `offre` | INTROUVABLE |
| `listerOffres` | | liste de `offre` | |
| `modifierOffre` | `offre` avec `idOffre` et `statut` | `offre` modifiée | VALIDATION, INTROUVABLE, CONFLIT |
| `supprimerOffre` | `idOffre` | | INTROUVABLE, CONFLIT (candidatures existantes) |
| `verifierDisponibilite` | `idOffre` | `disponibilite` : `existe`, `disponible`, `motif`, `idEntreprise`, `titre`, `dureeMois`, `statut`, `dateLimite`, `placesDisponibles` | VALIDATION |
| `verifierCompatibilite` | `idOffre`, `filiere`, `niveauEtudes` | `compatibilite` : `compatible`, `motif` | VALIDATION, INTROUVABLE |
| `reserverPlace` | `idOffre` | `reservee` (booléen) : décrément atomique, `false` s'il n'y a plus de place | INTROUVABLE |
| `libererPlace` | `idOffre` | compensation de `reserverPlace` | INTROUVABLE |

`offre` : `idEntreprise`, `titre`, `description`, `competencesRequises`, `dureeMois` (1 à 12), `dateLimite` (AAAA-MM-JJ), `placesDisponibles`, `statut` (`ACTIVE`, `CLOTUREE`), `filieresCibles` (facultatif, séparées par des virgules ; vide : toutes), `niveauMinimum` (facultatif : 2 accepte « INE 2 » et « INE 3 »).
