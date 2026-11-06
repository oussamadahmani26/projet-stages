package ma.inpt.soa.entreprises;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.soap.SoapServer;

public final class EntreprisesApplication {
    private EntreprisesApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return SoapServer.publier(port, "/ws/entreprises", new EntrepriseService());
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8082);
        demarrer(port);
        System.out.println("Service SOAP Entreprises : http://localhost:" + port + "/ws/entreprises?wsdl");
    }
}
