-- Fichier : sql/data.sql
USE gestion_stages;

-- Insertion de 5 Étudiants (dont 2 en filière AMOA)
INSERT INTO Etudiant (matricule, nom, filiere, niveau_etudes, email, telephone, statut) VALUES
('INE-2026-001', 'Oussama Dahmani', 'Innovation et AMOA', 'INE 2', 'dahmani.oussama@ine.inpt.ac.ma', '+212642133295', 'ACTIF'),
('INE-2026-002', 'Jawad Chafik', 'Innovation et AMOA', 'INE 2', 'chafik.jawad@ine.inpt.ac.ma', '+212600000000', 'ACTIF'),
('INE-2026-003', 'Fatima Zahra', 'Ingénierie Logicielle', 'INE 3', 'fatima.zahra@ine.inpt.ac.ma', '+212611111111', 'ACTIF'),
('INE-2026-004', 'Reda Bouabid', 'Cybersécurité', 'INE 2', 'bouabid.reda@ine.inpt.ac.ma', '+212622222222', 'ACTIF'),
('INE-2026-005', 'Sara Alaoui', 'Data Science', 'INE 1', 'sara.alaoui@ine.inpt.ac.ma', '+212633333333', 'SUSPENDU');

-- Insertion de 3 Entreprises
INSERT INTO Entreprise (nom, secteur_activite, email, telephone, responsable_contact) VALUES
('CRM VALUE', 'Relation Client & Services', 'contact@crmvalue.ma', '+212535000000', 'Chef de Projet IT'),
('Bonitasoft', 'Édition de logiciels (BPM)', 'rh@bonitasoft.com', '+33140000000', 'Directeur Technique'),
('Capgemini Engineering', 'Conseil et Services IT', 'recrutement.ma@capgemini.com', '+212522000000', 'Responsable RH');

-- Insertion de 6 Offres de stage
INSERT INTO Offre_Stage (id_entreprise, titre, description, competences_requises, duree_mois, date_limite, places_disponibles, statut) VALUES
(1, 'Stage Assistant Maîtrise d''Ouvrage (AMOA)', 'Analyse des processus et accompagnement client', 'UML, BPMN, SQL, Communication', 3, '2027-02-28', 2, 'ACTIVE'),
(1, 'Développeur Fullstack Junior', 'Développement d''outils internes (Shift Nuit)', 'Node.js, Express, MongoDB', 4, '2027-03-10', 1, 'ACTIVE'),
(2, 'Stage Développeur SOA/BPM', 'Intégration de processus métiers automatisés', 'Java, REST, SOAP, BPMN, Bonita', 6, '2027-03-15', 2, 'ACTIVE'),
(2, 'Analyste Processus Métier', 'Modélisation et optimisation des flux', 'BPMN, Bizagi, Analyse métier', 3, '2027-04-01', 1, 'ACTIVE'),
(3, 'Ingénieur Cloud Infrastructure', 'Déploiement d''infrastructures cloud', 'Linux, OpenStack, Bash', 6, '2027-05-30', 3, 'ACTIVE'),
(3, 'Stage Consultant Cybersécurité', 'Audit et tests d''intrusion des SI', 'Réseaux, Kali Linux, Scripts', 4, '2026-12-30', 1, 'CLOTUREE');

-- Profil académique attendu par chaque offre (utilisé par verifierCompatibilite)
UPDATE Offre_Stage SET filieres_cibles = 'Innovation et AMOA', niveau_minimum = 1 WHERE id_offre = 1;
UPDATE Offre_Stage SET filieres_cibles = 'Ingénierie Logicielle,Innovation et AMOA', niveau_minimum = 1 WHERE id_offre = 2;
UPDATE Offre_Stage SET filieres_cibles = 'Innovation et AMOA,Ingénierie Logicielle', niveau_minimum = 2 WHERE id_offre = 3;
UPDATE Offre_Stage SET filieres_cibles = 'Innovation et AMOA', niveau_minimum = 2 WHERE id_offre = 4;
UPDATE Offre_Stage SET niveau_minimum = 2 WHERE id_offre = 5;
UPDATE Offre_Stage SET filieres_cibles = 'Cybersécurité', niveau_minimum = 2 WHERE id_offre = 6;
