package sn.com.developer.paymentapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;

/**
 * Filtre d'authentification par clé API pour sécuriser les endpoints de l'API de paiement.
 *
 * <p>Ce filtre intercepte toutes les requêtes HTTP entrantes et vérifie la présence
 * d'une clé API valide dans le header HTTP. Les requêtes avec une clé API valide
 * sont automatiquement authentifiées, tandis que celles avec une clé invalide
 * ou manquante sont rejetées avec un code d'erreur 401.</p>
 *
 * <p>Hérite de {@link OncePerRequestFilter} pour garantir qu'il ne s'exécute
 * qu'une seule fois par requête, même en cas de redirections internes.</p>
 *
 * <h3>Configuration requise :</h3>
 * <pre>
 * payment:
 *   api:
 *     key: "your-secret-api-key-here"
 * </pre>
 *
 * <h3>Utilisation :</h3>
 * <pre>
 * curl -H "API-KEY: your-secret-api-key-here" http://localhost:8080/api/v1/payments
 * </pre>
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    /**
     * Clé API configurée dans les propriétés de l'application.
     * Cette valeur est injectée depuis payment.api.key dans application.yml
     */
    @Value("${payment.api.key}")
    private String configuredApiKey;

    /**
     * Nom du header HTTP contenant la clé API.
     * Les clients doivent envoyer leur clé API dans ce header.
     */
    private static final String HEADER_NAME = "API-KEY";

    /**
     * Méthode principale du filtre qui traite chaque requête HTTP entrante.
     *
     * <p>Cette méthode :</p>
     * <ol>
     *   <li>Récupère la clé API du header HTTP "API-KEY"</li>
     *   <li>Compare cette clé avec la clé configurée dans l'application</li>
     *   <li>Si la clé est valide, crée un objet d'authentification Spring Security</li>
     *   <li>Si la clé est invalide ou manquante, retourne une erreur 401</li>
     *   <li>Exclut certains chemins (Swagger, Actuator) de la validation</li>
     * </ol>
     *
     * @param request la requête HTTP entrante
     * @param response la réponse HTTP
     * @param filterChain la chaîne de filtres à continuer
     * @throws Exception si une erreur survient pendant le traitement
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        try {
            String requestApiKey = request.getHeader(HEADER_NAME);

            if (requestApiKey != null && requestApiKey.equals(configuredApiKey)) {
                // Clé API valide - création de l'authentification
                var authentication = new UsernamePasswordAuthenticationToken(
                        "apiKeyUser",  // Principal générique pour les utilisateurs API
                        null,          // Pas de credentials stockés
                        Collections.emptyList() // Aucun rôle assigné par défaut
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else if (!isSwaggerPath(request.getRequestURI())) {
                // Clé API invalide ou manquante (sauf pour les chemins exclus)
                log.warn("Requête rejetée : Clé API invalide pour l'URI: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Clé API invalide");
                return;
            }

            // Continuer la chaîne de filtres
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Erreur dans ApiKeyAuthFilter pour l'URI: {}", request.getRequestURI(), ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne");
            } catch (Exception ignored) {
                // Ignore les erreurs de réponse si la réponse est déjà committée
            }
        }
    }

    /**
     * Vérifie si le chemin de la requête correspond à un chemin exclu de l'authentification.
     *
     * <p>Les chemins exclus incluent :</p>
     * <ul>
     *   <li>/swagger-ui/* - Interface Swagger UI</li>
     *   <li>/v3/api-docs/* - Documentation OpenAPI</li>
     *   <li>/api-docs/* - Documentation API alternative</li>
     *   <li>/actuator/* - Endpoints Spring Boot Actuator</li>
     * </ul>
     *
     * @param path le chemin de la requête à vérifier
     * @return true si le chemin est exclu de l'authentification, false sinon
     */
    private boolean isSwaggerPath(String path) {
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator");
    }
}