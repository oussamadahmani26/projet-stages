package ma.inpt.soa.common.exception;

public class ServiceException extends Exception {
    public static final String VALIDATION = "VALIDATION";
    public static final String INTROUVABLE = "INTROUVABLE";
    public static final String CONFLIT = "CONFLIT";
    public static final String INDISPONIBLE = "INDISPONIBLE";
    public static final String NON_AUTHENTIFIE = "NON_AUTHENTIFIE";
    public static final String INTERDIT = "INTERDIT";
    public static final String TROP_DE_TENTATIVES = "TROP_DE_TENTATIVES";

    private final String code;

    // Ne prend qu'un message en paramètre pour éviter de propager la cause (la stack trace d'origine)
    public ServiceException(String message) {
        this(INDISPONIBLE, message);
    }

    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Catégorie d'erreur, exposée dans la faute SOAP et traduite en code HTTP côté REST. */
    public String getCode() {
        return code;
    }

    public static ServiceException validation(String message) {
        return new ServiceException(VALIDATION, message);
    }

    public static ServiceException introuvable(String message) {
        return new ServiceException(INTROUVABLE, message);
    }

    public static ServiceException conflit(String message) {
        return new ServiceException(CONFLIT, message);
    }
}
