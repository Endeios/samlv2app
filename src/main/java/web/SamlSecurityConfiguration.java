package web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Filter;

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.metadata.MetadataMemoryProvider;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@EnableWebSecurity
@Order(2)
public class SamlSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Value("${app.entityId:test}")
    private String entityId;
    @Value("${app.entityBaseUrl:http://localhost:8080}")
    private String entityBaseUrl;


    @Bean
    public static SAMLBootstrap samlBootstrap() {
        return new SAMLBootstrap();
    }

    private MetadataProvider spMetadata() {
        EntityDescriptor descriptor = metadataGenerator().generateMetadata();
        return new MetadataMemoryProvider(descriptor);
    }

    private MetadataGenerator metadataGenerator() {
        final MetadataGenerator metadataGenerator = new MetadataGenerator();
        metadataGenerator.setEntityId(entityId);
        metadataGenerator.setEntityBaseURL(entityBaseUrl);
        metadataGenerator.setKeyManager(keyManager());
        metadataGenerator.setExtendedMetadata(extendedMetadata());
        return metadataGenerator;
    }

    private ExtendedMetadata extendedMetadata() {
        final ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setLocal(true);
        extendedMetadata.setIdpDiscoveryEnabled(false);
        return extendedMetadata;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .addFilterAfter(metadataDisplayFilter(), SecurityContextPersistenceFilter.class)
        ;
    }


    @Bean
    protected Filter metadataDisplayFilter() throws MetadataProviderException {
        MetadataDisplayFilter metadataDisplayFilter = new MetadataDisplayFilter();
        metadataDisplayFilter.setContextProvider(contextProvider());
        metadataDisplayFilter.setManager(manager());
        return metadataDisplayFilter;
    }

    @Bean
    protected MetadataManager manager() throws MetadataProviderException {
        List<MetadataProvider> providers = new ArrayList<>();
        providers.add(spMetadata());
        MetadataManager manager = new MetadataManager(providers);
        manager.setHostedSPName(entityId);
        return manager;
    }

    @Bean
    protected SAMLContextProvider contextProvider() {
        final SAMLContextProviderImpl samlContextProvider = new SAMLContextProviderImpl();
        samlContextProvider.setKeyManager(keyManager());
        return samlContextProvider;
    }

    @Bean
    protected KeyManager keyManager() {
        final HashMap<String, String> passwords = new HashMap<>();
        passwords.put("test", "");
        return new JKSKeyManager(new ClassPathResource("truststore.jks"), null, passwords, "test");
    }

}
