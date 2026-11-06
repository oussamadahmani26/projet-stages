package ma.inpt.soa.rest;

/** Corps JSON renvoyé pour toute réponse en erreur. */
public class ErreurApi {
    private final String code;
    private final String message;

    public ErreurApi(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
