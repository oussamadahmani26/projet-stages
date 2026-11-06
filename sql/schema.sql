-- Fichier : sql/schema.sql
-- Compatible MySQL 8 et MariaDB 10.x. Rejouable : les tables existantes sont conservées.
CREATE DATABASE IF NOT EXISTS gestion_stages CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestion_stages;

CREATE TABLE IF NOT EXISTS Etudiant (
    id_etudiant    INT AUTO_INCREMENT PRIMARY KEY,
    matricule      VARCHAR(30)  NOT NULL UNIQUE,
    nom            VARCHAR(120) NOT NULL,
    filiere        VARCHAR(120) NOT NULL,
    niveau_etudes  VARCHAR(30)  NOT NULL,
    email          VARCHAR(160) NOT NULL UNIQUE,
    telephone      VARCHAR(20),
    statut         ENUM('ACTIF','SUSPENDU','EN_STAGE') NOT NULL DEFAULT 'ACTIF'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Entreprise (
    id_entreprise       INT AUTO_INCREMENT PRIMARY KEY,
    nom                 VARCHAR(160) NOT NULL UNIQUE,
    secteur_activite    VARCHAR(160) NOT NULL,
    email               VARCHAR(160) NOT NULL,
    telephone           VARCHAR(20),
    responsable_contact VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Offre_Stage (
    id_offre             INT AUTO_INCREMENT PRIMARY KEY,
    id_entreprise        INT NOT NULL,
    titre                VARCHAR(200) NOT NULL,
    description          TEXT NOT NULL,
    competences_requises VARCHAR(500) NOT NULL,
    duree_mois           INT NOT NULL,
    date_limite          DATE NOT NULL,
    places_disponibles   INT NOT NULL DEFAULT 1,
    statut               ENUM('ACTIVE','CLOTUREE') NOT NULL DEFAULT 'ACTIVE',
    -- Profil académique attendu. NULL = offre ouverte à tous.
    filieres_cibles      VARCHAR(500) NULL COMMENT 'Filières séparées par des virgules',
    niveau_minimum       INT NULL COMMENT 'Année minimale : 2 accepte INE 2 et INE 3',
    CONSTRAINT chk_offre_duree CHECK (duree_mois > 0),
    CONSTRAINT chk_offre_places CHECK (places_disponibles >= 0),
    CONSTRAINT fk_offre_entreprise FOREIGN KEY (id_entreprise) REFERENCES Entreprise(id_entreprise) ON DELETE RESTRICT,
    INDEX idx_offre_statut (statut, date_limite)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Candidature (
    id_candidature    INT AUTO_INCREMENT PRIMARY KEY,
    id_etudiant       INT NOT NULL,
    id_offre          INT NOT NULL,
    lettre_motivation TEXT NULL,
    date_depot        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_maj          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    statut            ENUM('DEPOSEE','EN_COURS_EVALUATION','ACCEPTEE','REFUSEE','ANNULEE') NOT NULL DEFAULT 'DEPOSEE',
    motif             VARCHAR(500) NULL,
    -- Garantit au niveau base l'absence de candidature en double.
    CONSTRAINT uq_candidature UNIQUE (id_etudiant, id_offre),
    CONSTRAINT fk_candidature_etudiant FOREIGN KEY (id_etudiant) REFERENCES Etudiant(id_etudiant) ON DELETE RESTRICT,
    CONSTRAINT fk_candidature_offre FOREIGN KEY (id_offre) REFERENCES Offre_Stage(id_offre) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Stage (
    id_stage       INT AUTO_INCREMENT PRIMARY KEY,
    id_candidature INT NOT NULL UNIQUE,
    id_etudiant    INT NOT NULL,
    id_offre       INT NOT NULL,
    date_debut     DATE NOT NULL,
    date_fin       DATE NOT NULL,
    statut         ENUM('PLANIFIE','EN_COURS','TERMINE','INTERROMPU') NOT NULL DEFAULT 'PLANIFIE',
    CONSTRAINT chk_stage_dates CHECK (date_fin > date_debut),
    CONSTRAINT fk_stage_candidature FOREIGN KEY (id_candidature) REFERENCES Candidature(id_candidature) ON DELETE RESTRICT,
    CONSTRAINT fk_stage_etudiant FOREIGN KEY (id_etudiant) REFERENCES Etudiant(id_etudiant) ON DELETE RESTRICT,
    CONSTRAINT fk_stage_offre FOREIGN KEY (id_offre) REFERENCES Offre_Stage(id_offre) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Compte_Rendu (
    id_compte_rendu INT AUTO_INCREMENT PRIMARY KEY,
    id_stage        INT NOT NULL,
    periode         VARCHAR(60) NOT NULL,
    contenu         TEXT NOT NULL,
    auteur          VARCHAR(120) NOT NULL,
    date_depot      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cr_stage FOREIGN KEY (id_stage) REFERENCES Stage(id_stage) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Evaluation (
    id_evaluation INT AUTO_INCREMENT PRIMARY KEY,
    id_stage      INT NOT NULL,
    note          DECIMAL(4,2) NOT NULL,
    commentaire   TEXT NULL,
    evaluateur    VARCHAR(120) NOT NULL,
    date_depot    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_eval_note CHECK (note BETWEEN 0 AND 20),
    CONSTRAINT fk_eval_stage FOREIGN KEY (id_stage) REFERENCES Stage(id_stage) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Notification (
    id_notification    INT AUTO_INCREMENT PRIMARY KEY,
    type               ENUM('CONFIRMATION_DEPOT','RECEPTION_ENTREPRISE','CANDIDATURE_ACCEPTEE','CANDIDATURE_REFUSEE',
                            'CANDIDATURE_REJETEE','DEMANDE_COMPLEMENT','CANDIDATURE_ANNULEE','RAPPEL_DATE_LIMITE') NOT NULL,
    destinataire_type  ENUM('ETUDIANT','ENTREPRISE') NOT NULL,
    destinataire_id    INT NULL,
    destinataire_email VARCHAR(160) NOT NULL,
    sujet              VARCHAR(200) NOT NULL,
    message            TEXT NOT NULL,
    id_candidature     INT NULL,
    id_offre           INT NULL,
    statut_envoi       ENUM('SIMULEE','ENVOYEE','ECHEC') NOT NULL DEFAULT 'SIMULEE',
    date_envoi         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_dest (destinataire_type, destinataire_id),
    INDEX idx_notif_cand (id_candidature)
) ENGINE=InnoDB;

-- Comptes de connexion à l'interface. Le mot de passe n'est conservé que sous forme d'empreinte PBKDF2.
CREATE TABLE IF NOT EXISTS Compte (
    id_compte         INT AUTO_INCREMENT PRIMARY KEY,
    identifiant       VARCHAR(160) NOT NULL UNIQUE,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role              ENUM('ETUDIANT','ENTREPRISE','RESPONSABLE') NOT NULL,
    matricule         VARCHAR(30) NULL,
    id_entreprise     INT NULL,
    CONSTRAINT fk_compte_etudiant FOREIGN KEY (matricule) REFERENCES Etudiant(matricule) ON DELETE CASCADE,
    CONSTRAINT fk_compte_entreprise FOREIGN KEY (id_entreprise) REFERENCES Entreprise(id_entreprise) ON DELETE CASCADE
) ENGINE=InnoDB;
