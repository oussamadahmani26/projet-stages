package ma.inpt.soa.offres;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/** Résultat de verifierCompatibilite avec le profil académique de l'étudiant. */
@XmlAccessorType(XmlAccessType.FIELD)
public class CompatibiliteOffre {
    private boolean compatible;
    private String motif;

    public CompatibiliteOffre() {
    }

    public CompatibiliteOffre(boolean compatible, String motif) {
        this.compatible = compatible;
        this.motif = motif;
    }

    public boolean isCompatible() { return compatible; }
    public String getMotif() { return motif; }
}
