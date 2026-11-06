package ma.inpt.soa.processus.clients;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import ma.inpt.soa.common.exception.ServiceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Client SOAP : construit l'enveloppe décrite par le WSDL et renvoie le contenu de la réponse.
 * Chaque enfant de l'élément de réponse donne une entrée ; une valeur simple est rangée sous la clé "valeur".
 */
public class SoapClient {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private final String url;
    private final String espaceDeNoms;

    public SoapClient(String url, String espaceDeNoms) {
        this.url = url;
        this.espaceDeNoms = espaceDeNoms;
    }

    public String getUrl() {
        return url;
    }

    /** @param parametres valeurs simples ou Map imbriquée pour un objet (ex. "etudiant" : {...}) */
    public List<Map<String, String>> appeler(String operation, Map<String, ?> parametres) throws ServiceException {
        HttpRequest requete = HttpSupport.requete(url)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(enveloppe(operation, parametres), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> reponse = HttpSupport.envoyer(requete);
        if (reponse.statusCode() != 200 && reponse.statusCode() != 500) {
            throw HttpSupport.indisponible(requete);
        }
        Document document;
        try {
            document = parser().parse(new ByteArrayInputStream(reponse.body().getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw HttpSupport.indisponible(requete);
        }
        NodeList fautes = document.getElementsByTagNameNS(SOAP_NS, "Fault");
        if (fautes.getLength() > 0) {
            throw faute((Element) fautes.item(0));
        }
        return lireReponse(document);
    }

    public Map<String, String> appelerUn(String operation, Map<String, ?> parametres) throws ServiceException {
        return Services.premier(appeler(operation, parametres));
    }

    private static ServiceException faute(Element faute) {
        String message = texte(faute, "faultstring");
        NodeList codes = faute.getElementsByTagName("code");
        String code = codes.getLength() > 0 ? codes.item(0).getTextContent() : ServiceException.INDISPONIBLE;
        return new ServiceException(code, message == null ? "Erreur du service SOAP." : message);
    }

    private static String texte(Element parent, String nom) {
        NodeList liste = parent.getElementsByTagName(nom);
        return liste.getLength() > 0 ? liste.item(0).getTextContent() : null;
    }

    private static List<Map<String, String>> lireReponse(Document document) {
        List<Map<String, String>> resultats = new ArrayList<>();
        Element corps = (Element) document.getElementsByTagNameNS(SOAP_NS, "Body").item(0);
        Element reponse = premierElement(corps);
        if (reponse == null) {
            return resultats;
        }
        for (Node n = reponse.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Map<String, String> ligne = new LinkedHashMap<>();
                if (premierElement(n) == null) {
                    ligne.put("valeur", n.getTextContent());
                } else {
                    for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if (c instanceof Element) {
                            ligne.put(c.getLocalName(), c.getTextContent());
                        }
                    }
                }
                resultats.add(ligne);
            }
        }
        return resultats;
    }

    private static Element premierElement(Node parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                return (Element) n;
            }
        }
        return null;
    }

    private String enveloppe(String operation, Map<String, ?> parametres) throws ServiceException {
        try {
            Document d = parser().newDocument();
            Element enveloppe = d.createElementNS(SOAP_NS, "soapenv:Envelope");
            d.appendChild(enveloppe);
            Element corps = d.createElementNS(SOAP_NS, "soapenv:Body");
            enveloppe.appendChild(corps);
            Element op = d.createElementNS(espaceDeNoms, "ns:" + operation);
            corps.appendChild(op);
            ajouter(d, op, parametres);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter sortie = new StringWriter();
            t.transform(new DOMSource(d), new StreamResult(sortie));
            return sortie.toString();
        } catch (ParserConfigurationException | TransformerException e) {
            throw ServiceException.validation("Requête SOAP invalide.");
        }
    }

    private static void ajouter(Document d, Element parent, Map<String, ?> valeurs) {
        for (Map.Entry<String, ?> entree : valeurs.entrySet()) {
            Object valeur = entree.getValue();
            if (valeur == null || "".equals(valeur)) {
                continue;
            }
            Element e = d.createElementNS(null, entree.getKey());
            if (valeur instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> imbrique = (Map<String, ?>) valeur;
                ajouter(d, e, imbrique);
            } else {
                e.setTextContent(valeur.toString());
            }
            parent.appendChild(e);
        }
    }

    private static javax.xml.parsers.DocumentBuilder parser() throws ParserConfigurationException {
        DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        fabrique.setNamespaceAware(true);
        fabrique.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return fabrique.newDocumentBuilder();
    }
}
