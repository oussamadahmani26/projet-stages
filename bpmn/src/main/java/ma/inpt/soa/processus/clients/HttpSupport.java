package ma.inpt.soa.processus.clients;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.config.TlsSupport;
import ma.inpt.soa.common.exception.ServiceException;

/** Appels HTTP authentifiés, avec nouvelles tentatives lorsque la connexion n'a pas pu s'établir. */
final class HttpSupport {
    private static final int TENTATIVES = 3;
    private static final Duration PAUSE = Duration.ofMillis(300);
    private static final HttpClient CLIENT = creerClient();

    private HttpSupport() {
    }

    private static HttpClient creerClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3));
        if (TlsSupport.actif()) {
            builder.sslContext(TlsSupport.contexte());
        }
        return builder.build();
    }

    static HttpRequest.Builder requete(String url) {
        String jeton = AppConfig.utilisateurServices() + ":" + AppConfig.motDePasseServices();
        return HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization",
                        "Basic " + Base64.getEncoder().encodeToString(jeton.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Seuls les échecs de connexion sont retentés : la requête n'a alors pas atteint le service,
     * ce qui exclut tout traitement en double.
     */
    static HttpResponse<String> envoyer(HttpRequest requete) throws ServiceException {
        for (int essai = 1; ; essai++) {
            try {
                return CLIENT.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (ConnectException | HttpConnectTimeoutException e) {
                if (essai == TENTATIVES) {
                    throw indisponible(requete);
                }
                pause(requete);
            } catch (IOException e) {
                throw indisponible(requete);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw indisponible(requete);
            }
        }
    }

    private static void pause(HttpRequest requete) throws ServiceException {
        try {
            Thread.sleep(PAUSE.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw indisponible(requete);
        }
    }

    static ServiceException indisponible(HttpRequest requete) {
        return new ServiceException(ServiceException.INDISPONIBLE,
                "Service indisponible : " + requete.uri().getHost() + ":" + requete.uri().getPort()
                        + requete.uri().getPath());
    }
}
