package ma.inpt.soa.common.model;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;

/** Sérialise les dates au format ISO (AAAA-MM-JJ) dans les messages SOAP. */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
    @Override
    public LocalDate unmarshal(String valeur) {
        return valeur == null || valeur.isBlank() ? null : LocalDate.parse(valeur.trim());
    }

    @Override
    public String marshal(LocalDate valeur) {
        return valeur == null ? null : valeur.toString();
    }
}
