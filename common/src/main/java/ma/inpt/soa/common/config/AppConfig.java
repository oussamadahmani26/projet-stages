package ma.inpt.soa.common.config;

/** Configuration lue dans les propriétés système puis les variables d'environnement. */
public final class AppConfig {
    private AppConfig() {
    }

    public static String get(String nom, String defaut) {
        String valeur = System.getProperty(nom);
        if (valeur == null) {
            valeur = System.getenv(nom);
        }
        return valeur == null ? defaut : valeur;
    }

    public static int getInt(String nom, int defaut) {
        return Integer.parseInt(get(nom, Integer.toString(defaut)));
    }

    public static String utilisateurServices() {
        return get("SOA_USER", "soa_client");
    }

    public static String motDePasseServices() {
        return get("SOA_PASSWORD", "soa_dev_password");
    }

    public static boolean motDePasseParDefaut() {
        return "soa_dev_password".equals(motDePasseServices());
    }
}
