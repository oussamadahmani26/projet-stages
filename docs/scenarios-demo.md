# Scénarios de démonstration

Trame pour la vidéo, sur une base fraîchement initialisée (voir le README) et avec la plateforme démarrée. Durée estimée : 10 à 12 minutes. Ouvrir http://localhost:8090/. Les comptes sont listés dans le README. Dans ce qui suit, « espace Étudiant, *Nom* » signifie : se connecter avec le matricule de cet étudiant ; et de même pour les autres rôles. Utiliser **Se déconnecter** entre deux rôles.

## 1. Services seuls

1. Dans un navigateur, montrer le WSDL : http://localhost:8081/ws/etudiants?wsdl
2. Dans Postman, importer `docs/postman-collection.json` et lancer le dossier « 1. SOAP », puis « 2. REST ». Montrer un `verifierEtudiant`, une faute SOAP, un 201, un 404 et un 409.

## 2. Connexion

1. Montrer un mauvais mot de passe : la page affiche l'erreur.
2. Se connecter comme étudiant, puis comme entreprise : chacun arrive directement sur sa page.
3. Avec **Créer un compte**, inscrire un nouvel étudiant : il arrive connecté sur sa page.

## 3. Candidature acceptée

1. Espace **Étudiant**, profil *Oussama Dahmani* : candidater à *Stage Assistant Maîtrise d'Ouvrage*. L'annonce confirme la transmission.
2. Espace **Entreprise**, profil *CRM VALUE* : ouvrir le dossier, cliquer sur **Accepter**.
3. Espace **Responsable** : la candidature est *Acceptée*, le stage *Planifié*. Dans **Historique des processus**, cliquer sur **Voir le parcours** : le diagramme surligne le chemin parcouru.
4. Retour à l'espace **Étudiant** : profil *En stage*, notifications de dépôt et d'acceptation.

## 4. Candidatures rejetées par le processus

Chaque rejet affiche son motif, sans enregistrer de candidature.

| Profil | Action | Motif attendu |
|---|---|---|
| *Sara Alaoui* (suspendue) | Candidater à *Ingénieur Cloud* | Statut de l'étudiant : SUSPENDU |
| *Reda Bouabid* (Cybersécurité) | Candidater à *Stage Assistant Maîtrise d'Ouvrage* | La filière ne correspond pas à l'offre |
| *Sara Alaoui* | Candidater à *Stage Assistant Maîtrise d'Ouvrage* | Plusieurs motifs à la fois |

## 5. Refus, complément et annulation

1. *Jawad Chafik* candidate à *Analyste Processus Métier*. Entreprise *Bonitasoft* : saisir un motif, cliquer sur **Demander un complément**. Le dossier reste à traiter et l'étudiant a une notification.
2. *Bonitasoft* : **Refuser** avec un motif. L'étudiant voit le refus et le motif.
3. *Fatima Zahra* candidate à *Stage Développeur SOA/BPM*, puis clique sur **Annuler** : le dossier disparaît de l'espace de *Bonitasoft*. Elle peut candidater de nouveau.

## 6. Panne d'un service

C'est le scénario de gestion des erreurs : la partie à montrer le plus clairement.

1. Espace **Responsable** : tous les services sont disponibles.
2. Arrêter le service Notifications. Son numéro de processus est la 5e ligne de `.logs/pids.txt` :

   ```powershell
   Stop-Process -Id (Get-Content .logs\pids.txt)[4]
   ```

3. *Fatima Zahra* candidate à une offre. L'annonce indique qu'un responsable a été alerté.
4. Espace **Responsable** : le service est rouge, un incident s'affiche avec son étape et sa cause. **Relancer** échoue tant que le service est coupé.
5. Redémarrer le service :

   ```powershell
   $env:PORT=8085; Start-Process java -ArgumentList "-jar","rest-notifications\target\rest-notifications-1.0-SNAPSHOT-app.jar" -WindowStyle Hidden; Remove-Item Env:\PORT
   ```

6. **Relancer** : le traitement reprend à la bonne étape, sans candidature en double. Le dossier arrive chez l'entreprise.

## 7. Suivi du stage

1. Espace **Responsable**, section **Stages** : **Ouvrir le suivi**, puis **Passer à : En cours**.
2. Ajouter un compte rendu et une évaluation.
3. **Passer à : Terminé** : l'étudiant redevient *Actif* et peut candidater de nouveau.

## 8. Tests automatisés

Lancer `mvn clean package` et montrer le résumé des 41 tests.
