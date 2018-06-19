package test;

import java.text.SimpleDateFormat;

import org.apache.commons.codec.binary.Base64;

/**
 * Classe fournissant des methodes utilitaires pour la verification et la generation des tickets de SSO
 */
public final class SSOUtils {

    /**
     * Le truc qui permet de passer en base 64
     */
    private static final Base64 BASE64_ENCODER = new Base64();

    /**
     * Duree de vie par defaut d'un ticket : 1 heure
     */
    private static final int DEFAULT_TICKET_LIFE_TIME = 3600;

    /**
     * La duree de vie d'un ticket exprimee en secondes
     */
    private static int ticketLifeTimeSeconds = SSOUtils.DEFAULT_TICKET_LIFE_TIME;

    /**
     * @param ticketLifeTimeSeconds
     *            the ticketLifeTimeSeconds to set
     */
    public static void setTicketLifeTimeSeconds(final int ticketLifeTimeSeconds) {
        SSOUtils.ticketLifeTimeSeconds = ticketLifeTimeSeconds;
    }

    /**
     * La phrase secrete qui permet de sceller les tickets
     */
    private static String secretPhrase = "toto";

    /**
     * @param secretPhrase
     *            the secretPhrase to set
     */
    public static void setSecretPhrase(final String secretPhrase) {
        SSOUtils.secretPhrase = secretPhrase;
    }

    /**
     * Le format de date e utiliser pour la gestion du SSO
     */
    public static final SimpleDateFormat SSO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");

    private SSOUtils() {
        init();
    }

    /**
     * Initialise les donnees de SSOUtils
     */
    private void init() {
        // TODO alimenter #secretPhrase
        // TODO alimenter #ticketLifeTimeSeconds

    }

}
