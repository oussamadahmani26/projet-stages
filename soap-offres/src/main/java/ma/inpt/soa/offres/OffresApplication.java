package ma.inpt.soa.offres;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.soap.SoapServer;

public final class OffresApplication {
    private OffresApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return SoapServer.publier(port, "/ws/offres", new OffreService());
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8083);
        demarrer(port);
        System.out.println("Service SOAP Offres : http://localhost:" + port + "/ws/offres?wsdl");
    }
}
