# SAMLv2 Authenticated application

I integrated the SAMLv2 spring security plugin in our application some time ago, 
and I must say that was a bit challenging at the beginning: it took me a while 
to have it up and running, because I felt the need to understand what the different pieces
where doing, how they where interacting, and how to choose between the options.

The doc of the plug in is actually very clear, but the style of writing was a bit
too much "manualistic" for me, and it really took me a while to understand how and
why i wanted to combine the pieces.

What follows is a bit of a reconstructed journal of my exploration, in the hope of
putting my notes out from draft. It will help me to keep these things in mind 
(and at hand). Hopefully can be of some use for someone else.

## The structure

We devs learn a lot from others' code, and with git we have the opportunity to snapshot
different versions in a common format. I will make a tag for every interesting step
and report what is done and why.

### 1. The starting step

We start from a pretty basic spring boot application with spring security, without database.
It has a login screen, some users and roles (not used), a public page (the index) 
and a secret page ("/secret"). There is no static resources. I use CDN, and i don't
need to talk about how to serve static resources with spring security.

There are also a couple of tests that ensure that login screen behaves as expected, 
the anonymous resource is available, the secret one needs login.

### Importing the library (v2.0.1)

To import the library, at the time of the writing, you also need to import from an additional repo, the

```groovy
repositories {
....
  maven {
    url "http://repo.spring.io/plugins-release/"
  }
  ...
}
```

And of course add the library itself

```groovy
ext {
  ...
  samlSpringSecurityVersion   = '2.0.0.M17'
  ...
}
...

dependencies {
... 
  compile ("org.springframework.security.extensions:spring-security-saml2-core:${samlSpringSecurityVersion}")
...

}
```

Many Thanks to [Filip Hanik](https://github.com/fhanik) (something in your name screams Czechia, even if you live in Vancouver...) for 
his work and to **pivo**tal for spring (Vidim co tam chces Filipe). 

### Configuring the integration (Core) (c2.0.2)

The first thing we want to do is to set up a SAML Service Provider configuration, extending 
`org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration`
and overriding `SamlServerConfiguration getDefaultHostSamlServerConfiguration` in the following way

```java
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
```

in the private method, we are setting all the interesting information about our setup, like [keys](#key-setup), [providers](#identity-providers),
[alias](#alias) and the [authentication response filter](#authentication-response-filter).

#### Key setup

The key setup interesting part is how the key is extracted from the local key, and set in the `SimpleKey`
The `RotatingKeys` class is basically a key provider. The following configuration is for using only
one key for everything.

```java
  private RotatingKeys rotatingKeys() {
        X509Certificate myCertificate = CertUtil.getCertificateByName("test", "truststore.jks", null);
        final RotatingKeys rotatingKeys = new RotatingKeys();
        SimpleKey activeKey = new SimpleKey();
        activeKey.setName("test");
        try {
            activeKey.setCertificate(getCertificateForKey(myCertificate));
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        rotatingKeys.setActive(activeKey);
        return rotatingKeys;
    }

    private String getCertificateForKey(X509Certificate myCertificate) throws CertificateEncodingException {
        return getEncoder().encodeToString(myCertificate.getEncoded());
    }
```

#### Identity providers

This one is super easy: just create a new `ExternalIdentityProviderConfiguration` and set his metadata to
the url or the xml string body (yep, it can distinguish and behave accordingly), whatever is easier for you.
If you are in a private network you will probably want the second approach.

```java
   private ExternalIdentityProviderConfiguration externalProvider() {
        final ExternalIdentityProviderConfiguration externalIdentityProviderConfiguration = new ExternalIdentityProviderConfiguration();
        externalIdentityProviderConfiguration.setMetadata(metadata);
        return externalIdentityProviderConfiguration;
    }
```

#### Alias

The alias is the internal name that the Service provider uses for preparing the SSO consumer url.
It is not the "Service Provider Name", because that name usually contains characters like ":" being a `urn`,
and the url can result ugly.

#### Authentication response filter

The Authentication response filter is the place where you transform the information 
that comes from the idp to your user. The mechanism that i describe here is an `org.springframework.security.authentication.AuthenticationManager`,
but you will use an `org.springframework.security.authentication.AutheniticationProvider` in more complex cases.

First you need to override 
```java
    @Override
    public Filter spAuthenticationResponseFilter() {
        SamlResponseAuthenticationFilter filter = (SamlResponseAuthenticationFilter) super.spAuthenticationResponseFilter();
        filter.setAuthenticationManager(new SamlAuthenticationManager());
        return filter;
    }
```
Then you set the authentication manager as follows
```java
public class SamlAuthenticationManager implements AuthenticationManager {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        SamlAuthentication samlAuthentication = (SamlAuthentication) authentication;
        return new UsernamePasswordAuthenticationToken(asUser(samlAuthentication), "***", authentication.getAuthorities());
    }

    private UserDetails asUser(SamlAuthentication samlAuthentication) {
        String userName = samlAuthentication.getAssertion().getFirstAttribute("UserID").getValues().get(0).toString();
        String[] authorities = new String[]{samlAuthentication.getAssertingEntityId()};
        return new User(userName, "***", asAuthorities(authorities));
    }
}
```
Of course this is just a stub, maybe in your case you will want to use the email or maybe resolve the UserID against
a database to resolve the authorities: this is already app logic!

### Last Step

Here you just create a subclass for `org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration`
and feed in the configuration you just made
```java
@Configuration
@Order(1)
public class SamlWebSecurityConfiguration extends SamlServiceProviderSecurityConfiguration {

    @Autowired
    public SamlWebSecurityConfiguration(SamlServiceProviderServerBeanConfiguration configuration) {
        super(configuration);
    }
}
```
As a side note, @Order(1) menas that this settings will take precedence on @Order(2). Yes, when there is more than 1 security
configuration you **NEED** to specify an order.

## Links and notable resources

- [Spring security saml v2 integration plugin](https://github.com/spring-projects/spring-security-saml) (AKA Our library)
- [Start up a Jade app with spring](http://josdem.io/techtalk/spring/spring_boot_jade/) (I prefer it to other techs for theses small projects)
- [Jade reference](http://jade-lang.com/reference) and [interactive doc](https://naltatis.github.io/jade-syntax-docs/)
