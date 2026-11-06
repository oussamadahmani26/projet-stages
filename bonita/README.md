# Processus dans Bonita Studio

Ce dossier contient le processus de candidature pour Bonita Studio 10.4 (2025.2) : `Candidature_Stage-1.0.proc`, ses trois formulaires (`web_page/`), le tout regroupé avec sa configuration dans `Candidature_Stage.bos`. Ses tâches de service appellent les mêmes services SOAP et REST que la version Flowable.

## Avant de commencer

1. Les six services doivent tourner : `scripts/demarrer.ps1` (voir le README principal).
2. Si `SOA_PASSWORD` a été changé, remplacer `soa_dev_password` dans les scripts des tâches.

## Importer

Créer un projet dédié, pour ne pas mélanger ce processus avec un autre travail :

1. Dans le Studio : **Fichier > Nouveau projet**.
2. **Fichier > Importer > Fichier .bos**, puis choisir `bonita/Candidature_Stage.bos`.
3. Ouvrir le diagramme `Candidature_Stage (1.0)`.

Si l'import du `.bos` échoue, copier `Candidature_Stage-1.0.proc` dans `app/diagrams` et le contenu de `web_page/` dans `app/web_page` du projet (sous `Bureau\bonita\workspace\<projet>`), puis actualiser le Studio. L'acteur `Utilisateur` est alors à associer au groupe `/acme` : pool sélectionné, **Configurer**.

## Les trois formulaires

Ils sont générés depuis les contrats par l'UI Designer, puis adaptés : libellés en français, champs obligatoires, listes de choix fermées. Ils sont déjà reliés au processus et se modifient dans le Studio, onglet **Exécution > Formulaire**.

| Formulaire | Élément | Champs |
|---|---|---|
| `depotCandidature` | Le pool (formulaire d'instanciation) | Matricule et numéro de l'offre, obligatoires ; lettre de motivation |
| `decisionEntreprise` | La tâche « Décision de l'entreprise » | Décision (liste : accepter, refuser, demander un complément) ; motif ; date de début au format AAAA-MM-JJ |
| `traiterIncident` | La tâche « Traiter l'incident » | Action (liste : relancer, abandonner) |

## Exécuter

Sélectionner le pool et cliquer sur **Exécuter** : le portail s'ouvre sur le formulaire de dépôt. Les décisions se prennent ensuite dans **Tâches**. Le résultat se voit dans l'interface de la plateforme, http://localhost:8090/, avec le compte du responsable, puisque les deux versions partagent les mêmes services.

Pour montrer la panne, arrêter un service (voir `docs/scenarios-demo.md`), déposer une candidature : le processus s'arrête sur « Traiter l'incident ». Redémarrer le service et choisir `RELANCER`.

## Fonctionnement

Une variable de processus `ctx` porte les données du dossier. Chaque tâche de service exécute un script Groovy, visible dans **Exécution > Opérations**, qui appelle les services. Si un service échoue, le script range l'erreur dans `ctx.erreur`, et les tâches suivantes du groupe ne font plus rien. La passerelle « Service disponible ? » oriente alors vers « Traiter l'incident ».

## Différences avec la version Flowable

| Point | Flowable | Bonita |
|---|---|---|
| Erreurs | Événements d'erreur au bord des sous-processus | Passerelles « Service disponible ? » |
| Regroupement des étapes | Sous-processus | Tâches enchaînées dans un couloir |
| Annulation pendant l'évaluation | Événement de message, processus terminé proprement | Non modélisée : une annulation fait échouer la mise à jour de statut, et le cas passe en incident |
| Échec d'une notification de rejet ou de complément | Incident | Signalé dans `ctx.avertissement`, le processus continue |
| Formulaires | Interface de la plateforme | Formulaires Bonita générés à partir des contrats |

## Ce qui a été vérifié

**Sur le moteur Bonita 10.4, depuis le Studio :** une candidature complète acceptée. Le processus se déploie, le formulaire de dépôt déclenche les appels SOAP et REST, la candidature passe en évaluation et la tâche arrive à l'entreprise. Après « Accepter », la candidature est `ACCEPTEE`, le stage `PLANIFIE` créé avec les bonnes dates, l'étudiant `EN_STAGE`, une place est consommée et les trois notifications sont émises.

**Hors moteur :** 16 contrôles jouent les flux, les conditions et les scripts du fichier contre les vrais services : acceptation, complément, refus, cinq rejets, doublon, pannes puis reprise ou abandon.

**Pas encore exécuté sur le moteur Bonita :** le rejet, le refus, le complément, la panne d'un service et sa reprise. L'import par `Candidature_Stage.bos` n'a pas été essayé non plus : le processus et ses formulaires ont été installés directement dans le dossier du projet.

## Pièges rencontrés

Trois exigences du Studio ne sont pas signalées par le modèle EMF, et sont utiles à connaître pour modifier ce fichier à la main :

- Tous les éléments d'un pool doivent porter des noms différents.
- Le champ `content` d'un formulaire contient l'UUID de la page (`web_page/.metadata/.index.json`), et son champ `name` le nom de la page.
- Après une modification du fichier sur le disque, fermer l'onglet du diagramme sans enregistrer et actualiser le projet, sinon le Studio garde l'ancienne version.
