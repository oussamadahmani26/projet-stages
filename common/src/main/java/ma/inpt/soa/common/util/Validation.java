package ma.inpt.soa.common.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.regex.Pattern;
import ma.inpt.soa.common.exception.ServiceException;

/** Contrôles de données communs aux services SOAP et REST. */
public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEPHONE = Pattern.compile("^\\+?[0-9 .-]{6,20}$");

    private Validation() {
    }

    public static String texte(String valeur, String champ, int max) throws ServiceException {
        if (valeur == null || valeur.isBlank()) {
            throw ServiceException.validation("Le champ '" + champ + "' est obligatoire.");
        }
        return texteOptionnel(valeur, champ, max);
    }

    public static String texteOptionnel(String valeur, String champ, int max) throws ServiceException {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String nettoye = valeur.trim();
        if (nettoye.length() > max) {
            throw ServiceException.validation("Le champ '" + champ + "' dépasse " + max + " caractères.");
        }
        return nettoye;
    }

    public static String email(String valeur, String champ) throws ServiceException {
        String nettoye = texte(valeur, champ, 160);
        if (!EMAIL.matcher(nettoye).matches()) {
            throw ServiceException.validation("Le champ '" + champ + "' n'est pas une adresse e-mail valide.");
        }
        return nettoye;
    }

    public static String telephone(String valeur) throws ServiceException {
        String nettoye = texteOptionnel(valeur, "telephone", 20);
        if (nettoye != null && !TELEPHONE.matcher(nettoye).matches()) {
            throw ServiceException.validation("Le champ 'telephone' n'est pas un numéro valide.");
        }
        return nettoye;
    }

    public static String parmi(String valeur, String champ, String... autorisees) throws ServiceException {
        if (valeur == null || !Arrays.asList(autorisees).contains(valeur)) {
            throw ServiceException.validation(
                    "Le champ '" + champ + "' doit valoir : " + String.join(", ", autorisees) + ".");
        }
        return valeur;
    }

    public static int entier(Integer valeur, String champ, int min, int max) throws ServiceException {
        if (valeur == null || valeur < min || valeur > max) {
            throw ServiceException.validation(
                    "Le champ '" + champ + "' doit être compris entre " + min + " et " + max + ".");
        }
        return valeur;
    }

    public static LocalDate date(LocalDate valeur, String champ) throws ServiceException {
        if (valeur == null) {
            throw ServiceException.validation("Le champ '" + champ + "' est obligatoire (format AAAA-MM-JJ).");
        }
        return valeur;
    }
}
