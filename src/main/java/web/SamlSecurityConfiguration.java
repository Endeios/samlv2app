package web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.log.SAMLLogger;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.metadata.MetadataMemoryProvider;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessor;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static web.UserService.asAuthorities;

@EnableWebSecurity
@Order(2)
public class SamlSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Value("${app.entityId:test}")
    private String entityId;
    @Value("${app.entityBaseUrl:http://localhost:8080}")
    private String entityBaseUrl;
    private Log log = LogFactory.getLog(SecurityConfiguration.class);
    @Value("${app.metadataIdp:https://idp.ssocircle.com/meta-idp.xml}")
    private String metaIdpXml;

    @Bean
    public static SAMLBootstrap samlBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() throws MetadataProviderException {
        final MetadataGeneratorFilter metadataGeneratorFilter = new MetadataGeneratorFilter(metadataGenerator());
        metadataGeneratorFilter.setManager(manager());
        metadataGeneratorFilter.setDisplayFilter(metadataDisplayFilter());
        return metadataGeneratorFilter;
    }

    @Bean
    public SAMLProcessor processor() {
        Collection<SAMLBinding> bindings = new ArrayList<>();
        final VelocityEngine velocityEngine = VelocityFactory.getEngine();

        bindings.add(createHttpRedirectDeflateBinding());
        bindings.add(createHttpPostBinding(velocityEngine));
        bindings.add(createArtifactBinding(velocityEngine));
        bindings.add(createHttpSOAP11Binding());
        bindings.add(createHttpPAOS11Binding());
        return new SAMLProcessorImpl(bindings);
    }

    @Bean
    public SAMLEntryPoint samlEntryPoint() throws MetadataProviderException {
        final SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setMetadata(manager());
        samlEntryPoint.setContextProvider(contextProvider());
        samlEntryPoint.setWebSSOprofile(webSSOProfile());
        samlEntryPoint.setDefaultProfileOptions(defaultOptions());
        samlEntryPoint.setSamlLogger(samlLogger());
        return samlEntryPoint;
    }

    @Bean
    public WebSSOProfileOptions defaultOptions() {
        final WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        webSSOProfileOptions.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        return webSSOProfileOptions;
    }

    @Bean
    public SAMLLogger samlLogger() {
        final SAMLDefaultLogger samlDefaultLogger = new SAMLDefaultLogger();
        samlDefaultLogger.setLogErrors(true);
        samlDefaultLogger.setLogMessages(true);
        return samlDefaultLogger;
    }

    @Bean("idp-default")
    public ExtendedMetadataDelegate samlExtendedMetadataProvider() {
        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(idpMetadataProvider(), extendedMetadata());
        extendedMetadataDelegate.setMetadataTrustCheck(true);
        extendedMetadataDelegate.setMetadataRequireSignature(false);
        return extendedMetadataDelegate;
    }

    @Bean
    public SAMLProcessingFilter samlProcessingFilter() throws Exception {
        final SAMLProcessingFilter samlProcessingFilter = new SAMLProcessingFilter();
        samlProcessingFilter.setContextProvider(contextProvider());
        samlProcessingFilter.setSAMLProcessor(processor());
        samlProcessingFilter.setAuthenticationManager(authenticationManager());
        return samlProcessingFilter;
    }

    @Bean
    public SAMLAuthenticationProvider authenticationProvider() {
        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.setUserDetails(samlUserDetailsService());
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
    }

    @Bean("hokWebSSOprofileConsumer")
    public WebSSOProfileConsumer hokWebSSOProfileConsumer() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean("webSSOprofileConsumer")
    public WebSSOProfileConsumer webSSOProfileConsumer() {
        return new WebSSOProfileConsumerImpl();
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

    private HTTPRedirectDeflateBinding createHttpRedirectDeflateBinding() {
        return new HTTPRedirectDeflateBinding(parserPool());
    }

    private ParserPool parserPool() {
        return ParserPoolHolder.getPool();
    }

    private HTTPPostBinding createHttpPostBinding(VelocityEngine velocityEngine) {
        return new HTTPPostBinding(parserPool(), velocityEngine);
    }

    private HTTPArtifactBinding createArtifactBinding(VelocityEngine velocityEngine) {
        return new HTTPArtifactBinding(parserPool(), velocityEngine, createArtifactResolutionProfile());
    }

    private HTTPSOAP11Binding createHttpSOAP11Binding() {
        return new HTTPSOAP11Binding(parserPool());
    }

    private HTTPPAOS11Binding createHttpPAOS11Binding() {
        return new HTTPPAOS11Binding(parserPool());
    }

    private HTTPSOAP11Binding createSoapBinding() {
        return new HTTPSOAP11Binding(parserPool());
    }

    private ArtifactResolutionProfile createArtifactResolutionProfile() {
        final ArtifactResolutionProfileImpl artifactResolutionProfile =
                new ArtifactResolutionProfileImpl(httpClient());
        artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(createSoapBinding()));
        return artifactResolutionProfile;
    }

    private HttpClient httpClient() {
        return new HttpClient(new MultiThreadedHttpConnectionManager());
    }

    private MetadataProvider idpMetadataProvider() {
        Timer timer = new Timer();
        try {
            final HTTPMetadataProvider httpMetadataProvider = new HTTPMetadataProvider(timer, httpClient(), metaIdpXml);
            httpMetadataProvider.setParserPool(parserPool());
            return httpMetadataProvider;
        } catch (MetadataProviderException e) {
            log.error("Error initializing remote Idp SAMLv2 metadata: " + e.getCause().getMessage());
        }
        return new MetadataMemoryProvider(null);
    }

    private SAMLUserDetailsService samlUserDetailsService() {
        return new SAMLUserDetailsService() {
            @Override
            public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
                return new User(credential.getAttributeAsString("userID"), "", asAuthorities(new String[]{credential.getRemoteEntityID()}));
            }
        };
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .addFilterAfter(metadataDisplayFilter(), AnonymousAuthenticationFilter.class)
                //.addFilterBefore(metadataDisplayFilter(), CsrfFilter.class)
                .addFilterBefore(metadataGeneratorFilter(), MetadataDisplayFilter.class)
                .httpBasic().authenticationEntryPoint(samlEntryPoint())

        ;
    }

    @Bean
    protected MetadataDisplayFilter metadataDisplayFilter() throws MetadataProviderException {
        MetadataDisplayFilter metadataDisplayFilter = new MetadataDisplayFilter();
        metadataDisplayFilter.setContextProvider(contextProvider());
        metadataDisplayFilter.setManager(manager());
        return metadataDisplayFilter;
    }

    @Bean
    protected MetadataManager manager() throws MetadataProviderException {
        List<MetadataProvider> providers = new ArrayList<>();
        providers.add(spMetadata());
        providers.add(samlExtendedMetadataProvider());
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

    @Bean("webSSOprofile")
    protected WebSSOProfile webSSOProfile() throws MetadataProviderException {
        final WebSSOProfileImpl webSSOProfile = new WebSSOProfileImpl();
        webSSOProfile.setMetadata(manager());
        webSSOProfile.setProcessor(processor());
        return webSSOProfile;
    }
}
