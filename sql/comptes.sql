-- Fichier : sql/comptes.sql
-- Comptes de DÉMONSTRATION : mots de passe connus, à ne jamais utiliser en production.
--   Étudiants   : identifiant = matricule, mot de passe etudiant2026
--   Entreprises : identifiant = e-mail,    mot de passe entreprise2026
--   Responsable : identifiant = responsable, mot de passe responsable2026
-- Rejouable : les comptes existants sont conservés. À exécuter après schema.sql et data.sql.
USE gestion_stages;

INSERT IGNORE INTO Compte (identifiant, mot_de_passe_hash, role, matricule, id_entreprise) VALUES
('INE-2026-001', 'pbkdf2$120000$njVLDnYV1Yn6ykXaa10Enw==$9tDH0FNBh8UvlcvgYu76c0enQypVqtm7EsYdN7bP0W0=', 'ETUDIANT', 'INE-2026-001', NULL),
('INE-2026-002', 'pbkdf2$120000$mTDcmF8y3YRZEvby6LV1vA==$fZ/CoXwvh3isMUqcNa0mSd41IFRLucRBjBGbw5kmd18=', 'ETUDIANT', 'INE-2026-002', NULL),
('INE-2026-003', 'pbkdf2$120000$W3wSkfJP6FJ6TbFv/jgkSQ==$b66j03gNM2WLMBZzXXDjh4i9Ay6RgzE6PvTZPaML1hc=', 'ETUDIANT', 'INE-2026-003', NULL),
('INE-2026-004', 'pbkdf2$120000$4iyQPy7BHb12n3N2xzz1lQ==$I/vM4Gl9yMlyeuviaqYMA2XfuqEWwUPxG5S6piym9+I=', 'ETUDIANT', 'INE-2026-004', NULL),
('INE-2026-005', 'pbkdf2$120000$adonCXbjdocVIwScnRH7bg==$cXQU6KNkyRdpkUzhTkWRywqN/TcHJIJM4SPRav0UsiI=', 'ETUDIANT', 'INE-2026-005', NULL),
('contact@crmvalue.ma', 'pbkdf2$120000$70JhCVnBHDV7ZKPVM1rKLA==$4Gb4eBaDXkdcOklK6TouTe237P2P6FPfFi92+9EcmN0=', 'ENTREPRISE', NULL, 1),
('rh@bonitasoft.com', 'pbkdf2$120000$8KSPnSbsueZrccp2EoT0EQ==$khhkwQGayVf+SXlWeVQHC1ULVvF3JRzoimwSixa07p8=', 'ENTREPRISE', NULL, 2),
('recrutement.ma@capgemini.com', 'pbkdf2$120000$clCy2DE0dXcc4aiDyxICLg==$Hd+gYzKT3/7OEkJGf07EyeYPb0wHeH/naw+qIiKXWtE=', 'ENTREPRISE', NULL, 3),
('responsable', 'pbkdf2$120000$NaEUnnbVRJo2GTDq7jt63A==$+6GKfczNSpA1M9/AOMjK0trhpSabRJf6TggD1N+Z5A8=', 'RESPONSABLE', NULL, NULL);
