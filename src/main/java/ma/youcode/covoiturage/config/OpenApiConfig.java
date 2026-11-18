package ma.youcode.covoiturage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plateforme de Covoiturage Multi-Étapes API")
                        .version("1.0.0")
                        .description("API REST backend pour la gestion de covoiturages avec arrêts multiples")
                        .contact(new Contact().name("YouCode").email("contact@youcode.ma")));
    }
}
