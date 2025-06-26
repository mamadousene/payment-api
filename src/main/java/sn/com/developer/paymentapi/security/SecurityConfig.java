package sn.com.developer.paymentapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de sécurité Spring Security pour l'API de paiement.
 *
 * <p>Cette classe configure la sécurité de l'application en utilisant une approche
 * d'authentification par clé API. Elle définit les règles d'autorisation pour
 * les différents endpoints et intègre le filtre d'authentification personnalisé.</p>
 *
 * <h3>Stratégie de sécurité :</h3>
 * <ul>
 *   <li><strong>Authentification par clé API</strong> : Tous les endpoints API nécessitent une clé API valide</li>
 *   <li><strong>Endpoints publics</strong> : Documentation (Swagger) et monitoring (Actuator) accessibles sans authentification</li>
 *   <li><strong>CSRF désactivé</strong> : Approprié pour une API REST stateless</li>
 *   <li><strong>Filtre personnalisé</strong> : Intégration du ApiKeyAuthFilter avant l'authentification standard</li>
 * </ul>
 *
 * <h3>Architecture de sécurité :</h3>
 * <pre>
 * Requête HTTP → ApiKeyAuthFilter → Autorisation → Endpoint
 *                     ↓
 *               Vérification API-KEY
 * </pre>
 *
 * <h3>Endpoints publics (sans authentification) :</h3>
 * <ul>
 *   <li>/swagger-ui.html - Page principale Swagger</li>
 *   <li>/swagger-ui/** - Ressources Swagger UI</li>
 *   <li>/v3/api-docs/** - Documentation OpenAPI 3.0</li>
 *   <li>/api-docs/** - Documentation API</li>
 *   <li>/actuator/** - Endpoints de monitoring Spring Boot</li>
 * </ul>
 *
 * <h3>Sécurité en production :</h3>
 * <p><strong>ATTENTION :</strong> Les endpoints Swagger et Actuator sont actuellement
 * publics pour faciliter le développement. En production, il est recommandé de :</p>
 * <ul>
 *   <li>Désactiver Swagger ou le protéger par authentification</li>
 *   <li>Restreindre l'accès aux endpoints Actuator sensibles</li>
 *   <li>Utiliser HTTPS pour toutes les communications</li>
 * </ul>
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 * @see ApiKeyAuthFilter
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Filtre d'authentification par clé API.
     * Injecté automatiquement par Spring et utilisé dans la chaîne de filtres de sécurité.
     */
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    /**
     * Configure la chaîne de filtres de sécurité pour l'application.
     *
     * <p>Cette méthode définit :</p>
     * <ol>
     *   <li><strong>Désactivation CSRF</strong> : Approprié pour une API REST stateless</li>
     *   <li><strong>Règles d'autorisation</strong> : Définit quels endpoints sont publics vs protégés</li>
     *   <li><strong>Intégration du filtre API</strong> : Ajoute le ApiKeyAuthFilter avant l'authentification standard</li>
     *   <li><strong>Configuration HTTP Basic</strong> : Fallback pour compatibilité (rarement utilisé)</li>
     * </ol>
     *
     * <h4>Flux de traitement :</h4>
     * <pre>
     * 1. Requête reçue
     * 2. Vérification si endpoint public → Autoriser directement
     * 3. Sinon, passer par ApiKeyAuthFilter
     * 4. Si authentifié → Autoriser l'accès
     * 5. Sinon → Retourner 401 Unauthorized
     * </pre>
     *
     * <h4>Configuration CSRF :</h4>
     * <p>CSRF est désactivé car cette API est conçue pour être stateless.
     * Les clients utilisent des clés API plutôt que des sessions, ce qui
     * rend les attaques CSRF non pertinentes.</p>
     *
     * <h4>Ordre des filtres :</h4>
     * <p>Le ApiKeyAuthFilter est placé avant UsernamePasswordAuthenticationFilter
     * pour intercepter les requêtes en premier et éviter le traitement inutile
     * des mécanismes d'authentification standards.</p>
     *
     * @param http l'objet HttpSecurity pour configurer la sécurité web
     * @return la chaîne de filtres de sécurité configurée
     * @throws Exception si une erreur survient pendant la configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Désactivation CSRF - Approprié pour API REST stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Configuration des autorisations d'accès
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics - Documentation et monitoring (MODE DEV)
                        // ATTENTION: En production, considérer la sécurisation de ces endpoints
                        .requestMatchers(
                                "/swagger-ui.html",     // Page principale Swagger
                                "/swagger-ui/**",       // Ressources Swagger UI (CSS, JS, etc.)
                                "/v3/api-docs/**",      // Documentation OpenAPI 3.0
                                "/api-docs/**",         // Documentation API alternative
                                "/actuator/**"          // Endpoints Spring Boot Actuator
                        ).permitAll()

                        // Tous les autres endpoints nécessitent une authentification
                        .anyRequest().authenticated()
                )

                // Ajout du filtre d'authentification par clé API
                // Placé avant UsernamePasswordAuthenticationFilter pour priorité
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Configuration HTTP Basic comme fallback (rarement utilisé)
                .httpBasic(Customizer.withDefaults())

                // Ajout d'un AuthenticationEntryPoint personnalisé pour éviter la page login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Cle API manquante ou invalide\"}");
                        })
                )

                // Construction de la chaîne de sécurité
                .build();
    }
}