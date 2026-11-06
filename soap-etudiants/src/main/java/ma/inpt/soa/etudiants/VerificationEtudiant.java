package ma.inpt.soa.etudiants;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/** Résultat de la vérification d'un étudiant : existence, statut et droit de candidater. */
@XmlAccessorType(XmlAccessType.FIELD)
public class VerificationEtudiant {
    private boolean existe;
    private boolean eligible;
    private String statut;
    private String motif;

    public VerificationEtudiant() {
    }

    public VerificationEtudiant(boolean existe, boolean eligible, String statut, String motif) {
        this.existe = existe;
        this.eligible = eligible;
        this.statut = statut;
        this.motif = motif;
    }

    public boolean isExiste() { return existe; }
    public boolean isEligible() { return eligible; }
    public String getStatut() { return statut; }
    public String getMotif() { return motif; }
}
