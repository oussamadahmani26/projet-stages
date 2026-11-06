package ma.inpt.soa.etudiants;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.soap.SoapServer;

public final class EtudiantsApplication {
    private EtudiantsApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return SoapServer.publier(port, "/ws/etudiants", new EtudiantService());
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8081);
        demarrer(port);
        System.out.println("Service SOAP Etudiants : http://localhost:" + port + "/ws/etudiants?wsdl");
    }
}
