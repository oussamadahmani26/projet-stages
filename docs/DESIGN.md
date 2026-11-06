# DESIGN.md : Le dossier de stage

Système de design de l'interface web, au format en neuf sections popularisé par Awesome Design. À lire avant toute modification de `frontend/`.

## 1. Thème visuel et atmosphère

L'application reprend le monde d'un dossier administratif : papier chaud, encre vert sapin, chemises à onglet et tampons de statut. Le ton est calme et sérieux, avec une densité d'outil de travail. Les pages montrent l'information réelle, sans illustration ni texte décoratif.

## 2. Palette et rôles

| Nom | Hex | Rôle |
|---|---|---|
| Papier | `#F3EEE3` | Fond de page |
| Feuille | `#FBF8F1` | Surfaces, panneaux |
| Encre | `#1E2A26` | Texte principal |
| Discret | `#5B5F55` | Texte secondaire |
| Filet | `#D8D0BF` | Bordures |
| Sapin | `#0F4C3A` | Accent unique, action principale, statut positif |
| Sapin foncé | `#0A3729` | Panneau de connexion, survol |
| Vermillon | `#B23A22` | Refus, panne, incident, échéance proche |
| Ocre | `#7D5300` | Attente |
| Ardoise | `#2C5673` | Information |

Les couleurs d'état accompagnent toujours un libellé : aucune information ne repose sur la couleur seule.

## 3. Typographie

| Rôle | Police | Graisse et taille |
|---|---|---|
| Titres | Fraunces, repli Georgia | 600 ; `h1` de 2,1 à 3,4 rem |
| Texte, boutons, champs | Instrument Sans, repli Segoe UI | 400 à 600 ; 15 px |
| Codes, dates, tampons, en-têtes de colonnes | IBM Plex Mono, repli Consolas | 400 à 500 ; 0,7 à 0,84 rem |

Les chiffres en colonne utilisent `tabular-nums`. Les polices viennent de Google Fonts ; sans réseau, les replis prennent le relais et l'interface reste complète.

## 4. Composants

- **Section (chemise) :** un onglet portant le titre et son compteur, posé sur une feuille.
- **Tampon :** statut en capitales à chasse fixe, double filet, sans fond.
- **Fiche d'offre :** entreprise, échéance J-n, titre, compétences, puis correspondance de profil et action.
- **Parcours :** Dépôt, Évaluation, Décision. L'étape courante est mise en avant et la décision prend sa couleur.
- **Boutons :** principal en sapin, secondaire à filet, danger en vermillon. Pendant un envoi, le bouton est désactivé et son libellé finit par « … ».
- **Champs :** label visible, exemple dans le placeholder, erreur en ligne avec focus sur le champ concerné.
- **Confirmation :** dialogue dont le bouton nomme l'action, par exemple « Supprimer l'offre ». Le focus part sur « Conserver ».
- **Compteurs :** chiffres avec libellé. Aucun graphique : ces effectifs se lisent mieux en nombres.

## 5. Mise en page

Largeur maximale de 1200 px, marges de 16 px au minimum. La hiérarchie est alignée à gauche : un titre, un chapô, puis les sections par ordre d'urgence. L'accès s'affiche en écran partagé : le récit du parcours réel à gauche, le formulaire à droite. Espacements principaux : 0,5 / 0,75 / 1 / 1,2 / 2,25 rem.

## 6. Profondeur

Hiérarchie par filets et par nuances de papier. Les seules ombres concernent les annonces flottantes et le bouton actif du choix de compte. Aucun dégradé, aucun verre dépoli.

## 7. À faire, à éviter

**À faire :** nommer l'action dans le bouton ; dire quoi faire dans un état vide ou une erreur ; afficher des données réelles ; respecter `prefers-reduced-motion`.

**À éviter :** violet, dégradés et emoji ; texte décoratif ; `transition: all` ; éléments inclinés ; parallaxe et carrousels ; chiffres inventés ; information portée par la couleur seule.

## 8. Comportement adaptatif

| Largeur | Changement |
|---|---|
| Moins de 980 px | Navigation sur sa propre ligne, parcours sous le titre de la candidature |
| Moins de 820 px | Accès sur une colonne, sans la liste des étapes ; dossiers et services sur une colonne |
| Moins de 560 px | Formulaires sur une colonne, nom de la marque réduit au logo |

Les tableaux défilent dans leur panneau, jamais la page. Les zones d'action font au moins 38 px de haut, 44 px pour l'action principale de l'accès.

## 9. Guide pour un agent

Reprendre les couleurs de la section 2 sans en ajouter. Toute nouvelle section passe par `section(id, titre, compte, corps, actions)`. Tout statut passe par `tampon(statut)`. Échapper tout contenu dynamique avec `esc()`. Vérifier le rendu à 1440, 768 et 390 px avant de livrer.
