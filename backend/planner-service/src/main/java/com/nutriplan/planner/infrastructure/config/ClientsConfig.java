package com.nutriplan.planner.infrastructure.config;

import com.nutriplan.common.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP hacia otros microservicios. Dos estrategias de autenticación service-to-service:
 * <ol>
 *   <li><b>Token propagation</b> (recipe-service): reenvía el JWT del usuario.</li>
 *   <li><b>Client Credentials</b> (pricing-service): token propio del servicio.</li>
 * </ol>
 */
@Configuration
@EnableResilientMethods // habilita @Retryable / @ConcurrencyLimit de Spring Framework 7
public class ClientsConfig {

    @Bean
    RestClient recipeRestClient(RestClient.Builder builder, @Value("${nutriplan.clients.recipe-url}") String url) {
        return builder.clone()
                .baseUrl(url)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(CurrentUser.bearerToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Manager que obtiene/renueva tokens fuera de un request HTTP del usuario (uso típico en
     * servicios). Solo habilitamos el grant client_credentials.
     */
    @Bean
    OAuth2AuthorizedClientManager serviceAuthorizedClientManager(ClientRegistrationRepository registrations,
                                                                 OAuth2AuthorizedClientService clients) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    RestClient pricingRestClient(RestClient.Builder builder, OAuth2AuthorizedClientManager manager,
                                 @Value("${nutriplan.clients.pricing-url}") String url) {
        var oauth = new OAuth2ClientHttpRequestInterceptor(manager);
        oauth.setClientRegistrationIdResolver(request -> "planner-service");
        // El token es del SERVICIO, no del usuario: usamos siempre el mismo "principal"
        oauth.setPrincipalResolver(request -> new AnonymousAuthenticationToken("planner-service",
                "planner-service", AuthorityUtils.createAuthorityList("ROLE_SERVICE")));
        return builder.clone()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestInterceptor(oauth)
                .build();
    }
}
