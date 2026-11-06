package ma.inpt.soa.processus;

import ma.inpt.soa.common.config.AppConfig;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;

/** Moteur Flowable embarqué. Son état (processus en attente, historique) survit aux redémarrages. */
public final class Moteur {
    public static final String CLE_PROCESSUS = "candidature";
    public static final String MESSAGE_ANNULATION = "annulationCandidature";
    private static ProcessEngine moteur;

    private Moteur() {
    }

    public static synchronized ProcessEngine moteur() {
        if (moteur == null) {
            moteur = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                    .setJdbcUrl(AppConfig.get("FLOWABLE_DB_URL", "jdbc:h2:file:./data/flowable"))
                    .setJdbcDriver("org.h2.Driver")
                    .setJdbcUsername("sa")
                    .setJdbcPassword("")
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                    .setHistory("full")
                    .setAsyncExecutorActivate(false)
                    .buildProcessEngine();
            moteur.getRepositoryService().createDeployment()
                    .name("candidature")
                    .addClasspathResource("processes/candidature.bpmn20.xml")
                    .enableDuplicateFiltering()
                    .deploy();
        }
        return moteur;
    }

    public static synchronized void arreter() {
        if (moteur != null) {
            moteur.close();
            moteur = null;
        }
    }
}
