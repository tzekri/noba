package io.noba.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert l'application React embarquée (classpath:/static, rempli par le Dockerfile racine).
 * Les routes du frontend (/q/demo, /admin…) ne sont pas des fichiers : elles renvoient index.html
 * et c'est React Router qui affiche le bon écran. En développement, le dossier est vide et Vite sert le frontend.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

	private static final Resource INDEX = new ClassPathResource("static/index.html");

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						// Une URL d'API inconnue doit rester une 404, pas renvoyer la page React.
						if (resourcePath.startsWith("api/") || !INDEX.exists()) {
							return null;
						}
						return INDEX;
					}
				});
	}
}
