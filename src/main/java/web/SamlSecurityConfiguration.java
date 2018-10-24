package web;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.config.RotatingKeys;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

import static java.util.Base64.getEncoder;

@Configuration
public class SamlSecurityConfiguration extends SamlServiceProviderServerBeanConfiguration {

    @Value("${app.metadataIdp:https://idp.ssocircle.com/meta-idp.xml}")
    private String metadata;
    @Value("${app.entityId:urn:org:endeios:mySamlv2App}")
    private String entityId;
    @Value("${app.entityBaseUrl:http://localhost:8080}")
    private String basePath;
    @Value("${app.alas:samlv2app}")
    private String alias;

    @Override
    protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
        final SamlServerConfiguration samlServerConfiguration = new SamlServerConfiguration();
        final LocalServiceProviderConfiguration serviceProvider = getLocalServiceProviderConfiguration();
        samlServerConfiguration.setServiceProvider(serviceProvider);
        return samlServerConfiguration;
    }

    private LocalServiceProviderConfiguration getLocalServiceProviderConfiguration() {
        final LocalServiceProviderConfiguration serviceProvider = new LocalServiceProviderConfiguration();

        serviceProvider.setSignRequests(false);
        serviceProvider.setWantAssertionsSigned(false);
        serviceProvider.setEntityId(entityId);
        serviceProvider.setSignMetadata(false);
        serviceProvider.setKeys(rotatingKeys());
        serviceProvider.setBasePath(basePath);
        serviceProvider.setAlias(alias);
        List<ExternalIdentityProviderConfiguration> providers = new ArrayList<>();
        providers.add(externalProvider());
        serviceProvider.setProviders(providers);
        return serviceProvider;
    }

    private ExternalIdentityProviderConfiguration externalProvider() {
        final ExternalIdentityProviderConfiguration externalIdentityProviderConfiguration = new ExternalIdentityProviderConfiguration();
        externalIdentityProviderConfiguration.setMetadata(metadata);
        return externalIdentityProviderConfiguration;
    }

    private RotatingKeys rotatingKeys() {
        X509Certificate myCertificate = CertUtil.getCertificateByName("test", "truststore.jks", null);
        final RotatingKeys rotatingKeys = new RotatingKeys();
        SimpleKey activeKey = new SimpleKey();
        activeKey.setName("test");
        try {
            activeKey.setCertificate(getEncoder().encodeToString(myCertificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        rotatingKeys.setActive(activeKey);
        return rotatingKeys;
    }
}
