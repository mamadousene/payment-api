package sn.com.developer.paymentapi.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Générateur d'identifiants de transaction thread-safe et performant.
 * Utilise une combinaison de timestamp, counter atomique et randomness pour garantir l'unicité.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Component
public final class TransactionIdGenerator {

    // Alphabet pour l'encodage base62 (plus compact que base64, sans caractères spéciaux)
    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();

    // Epoch personnalisé pour économiser des bits (1er janvier 2024)
    private static final long CUSTOM_EPOCH = 1704067200000L; // 2024-01-01T00:00:00Z

    // Compteur atomique pour éviter les collisions dans la même milliseconde
    private final AtomicLong counter = new AtomicLong(0);

    // Instance singleton thread-safe (initialization-on-demand holder idiom)
    private static final class InstanceHolder {
        private static final TransactionIdGenerator INSTANCE = new TransactionIdGenerator();
    }

    private TransactionIdGenerator() {
    }

    /**
     * Retourne l'instance singleton du générateur.
     */
    public static TransactionIdGenerator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Génère un identifiant de transaction unique.
     * Format: [timestamp_base62][counter_base62][random_base62]
     *
     * @return ID de transaction unique (environ 12-15 caractères)
     */
    public String generate() {
        // Timestamp depuis epoch personnalisé (économise ~10 ans de bits)
        long timestamp = Instant.now().toEpochMilli() - CUSTOM_EPOCH;

        // Counter pour éviter les collisions dans la même milliseconde
        long count = counter.incrementAndGet() % 1000; // Reset tous les 1000

        // Partie aléatoire pour robustesse supplémentaire
        int randomPart = ThreadLocalRandom.current().nextInt(0, BASE * BASE); // 2 caractères base62

        // Construction de l'ID
        return toBase62(timestamp) +
                toBase62(count) +
                toBase62(randomPart);
    }

    /**
     * Génère un ID de transaction avec préfixe personnalisé.
     *
     * @param prefix Préfixe à ajouter (ex: "TXN", "PAY", etc.)
     * @return ID avec préfixe
     */
    public String generateWithPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generate();
        }
        return prefix.toUpperCase() + "_" + generate();
    }

    /**
     * Génère un ID de transaction cryptographiquement sécurisé.
     * Plus lent mais approprié pour les transactions sensibles.
     *
     * @return ID sécurisé
     */
    public String generateSecure() {
        SecureRandom secureRandom = new SecureRandom();
        long timestamp = Instant.now().toEpochMilli() - CUSTOM_EPOCH;
        long count = counter.incrementAndGet() % 1000;
        int randomPart = secureRandom.nextInt(BASE * BASE * BASE); // 3 caractères base62

        return toBase62(timestamp) +
                toBase62(count) +
                toBase62(randomPart);
    }

    /**
     * Convertit un nombre en représentation base62.
     * Plus compact que base64 et sans caractères spéciaux problématiques.
     */
    private String toBase62(long value) {
        if (value == 0) return "0";

        StringBuilder result = new StringBuilder();
        long num = Math.abs(value);

        while (num > 0) {
            result.insert(0, BASE62_ALPHABET.charAt((int)(num % BASE)));
            num /= BASE;
        }

        return result.toString();
    }

    /**
     * Utilitaire pour valider le format d'un ID généré par cette classe.
     *
     * @param transactionId ID à valider
     * @return true si l'ID semble valide
     */
    public boolean isValidFormat(String transactionId) {
        if (transactionId == null) return false;

        // Retire le préfixe s'il existe
        String cleanId = transactionId.contains("_") ?
                transactionId.substring(transactionId.lastIndexOf("_") + 1) :
                transactionId;

        // Vérifie que tous les caractères sont dans l'alphabet base62
        return cleanId.length() >= 8 &&
                cleanId.length() <= 20 &&
                cleanId.chars().allMatch(c -> BASE62_ALPHABET.indexOf(c) >= 0);
    }

    /**
     * Méthode de convenance pour usage fluent.
     * Exemple: transactionIdGenerator.generate()
     */
    public String generate(Object... ignored) {
        return generate();
    }
}