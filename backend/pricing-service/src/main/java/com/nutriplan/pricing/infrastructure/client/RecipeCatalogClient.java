package com.nutriplan.pricing.infrastructure.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llama a recipe-service {@code /internal/ingredients/names} con Client Credentials.
 * Acá no hay usuario (corre en un job programado), así que el servicio usa su propia identidad.
 */
@Component
public class RecipeCatalogClient {

    private final RestClient http;

    public RecipeCatalogClient(RestClient.Builder builder, ClientRegistrationRepository registrations,
                               OAuth2AuthorizedClientService clients,
                               @Value("${nutriplan.clients.recipe-url}") String url) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        var oauth = new OAuth2ClientHttpRequestInterceptor(manager);
        oauth.setClientRegistrationIdResolver(request -> "pricing-service");
        oauth.setPrincipalResolver(request -> new AnonymousAuthenticationToken("pricing-service", "pricing-service",
                AuthorityUtils.createAuthorityList("ROLE_SERVICE")));
        this.http = builder.clone().baseUrl(url).requestInterceptor(oauth).build();
    }

    public List<String> ingredientNames() {
        return http.get().uri("/internal/ingredients/names").retrieve().body(new ParameterizedTypeReference<>() { });
    }
}
