package web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

@Configuration
@Order(1)
public class SamlWebSecurityConfiguration extends SamlServiceProviderSecurityConfiguration {

    @Autowired
    public SamlWebSecurityConfiguration(SamlServiceProviderServerBeanConfiguration configuration) {
        super(configuration);
    }
}
