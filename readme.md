# SAMLv2 Authenticated application (skippable)

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

## The structure (skippable)

We devs learn a lot from others' code, and with git we have the opportunity to snapshot
different versions in a common format. I will make a tag for every interesting step
and report what is done and why. Only the **main releases are "buildable"**, which means that 
minors _could_ be buildable, but not always: they are there to give anchor for the code state.

### 1. The starting step (skippable)

We start from a pretty basic spring boot application with spring security, without database.
It has a login screen, some users and roles (not used), a public page (the index) 
and a secret page ("/secret"). There is no static resources. I use CDN, and i don't
need to talk about how to serve static resources with spring security.

There are also a couple of tests that ensure that login screen behaves as expected, 
the anonymous resource is available, the secret one needs login.

### 2. The Real Deal : Putting together the SAMLv2 Plugin configuration (core)

When you will check out the v1.0 of this app, you will feel quite at home: usual classes in usual places, usual behaviour. Now the real
deal: let's discuss how to start with SAMLv2 plugin for spring.

#### 2.1 Adding the plugin (v1.0.1)

There exists a plugin for spring security that helps integrating the SSO standard
with spring: you can find it [here](https://projects.spring.io/spring-security-saml/),
and is by [Vladimír Schäfer](mailto:vladimir.schafer@gmail.com) (may he be blessed
for his effort). 

#### 2.2 Adding the local metadata v1.0.2

I am not going to explain SAMLv2 and te glossary here: 
there are a lot of nice resources online for that! But I can suggest some!

-[Brief introduction to samlv2](https://auth0.com/blog/how-saml-authentication-works/) (this should be your starting point)
-[Another introduction with different words](https://developers.onelogin.com/saml)
-[Wiki article on samlv2](https://en.wikipedia.org/wiki/SAML_2.0) (I know, usually these things suck on wikipedia, but this one is worth, it I promise)


As first step, I would like to suggest to set up your local metadata 
(the metadata of your service provider). To do so the **first step is
to create a _java key store_**, a place where you can securely store cryptographic
keys (and certificates!) for your application. I like to use a tool called [Portecle](http://portecle.sourceforge.net/)
but of course you can use the keytool or any other tool to manipulate jks stores.

Said keystore will have a RSA 2048 Key aliased and called (as in CNAME) "test", with empty or a password of your choice,
as show in the following
```java
    @Bean
    protected KeyManager keyManager() {
        final HashMap<String, String> passwords = new HashMap<>();
        passwords.put("test", "");
        return new JKSKeyManager(new ClassPathResource("truststore.jks"), null, passwords, "test");
    }

```


After this is the turn of the actual filter that will show the metadata display filter
```java
    @Bean
    protected Filter metadataDisplayFilter() throws MetadataProviderException {
        MetadataDisplayFilter metadataDisplayFilter = new MetadataDisplayFilter();
        metadataDisplayFilter.setContextProvider(contextProvider());
        metadataDisplayFilter.setManager(manager());
        return metadataDisplayFilter;
    }
```
This bean needs a context provider (which knows how to populate the data about the local SP) and a metadata manager. The metadata manager,
as the name suggests, is your one stopper for all your metadata needs: it contains the configurations of the saml authentications and gives
such information or request to the other beans. it is configures as follows

```java
    @Bean
    protected MetadataManager manager() throws MetadataProviderException {
        List<MetadataProvider> providers = new ArrayList<>();
        // this list has all the metadata providers 
        providers.add(spMetadata());
        MetadataManager manager = new MetadataManager(providers);
        // so we tell the entity ID that is "US"
        manager.setHostedSPName(entityId);
        return manager;
    }
```

The metadata manager, needs of course some provider (i.e the entities and their configuration representation as saml understand them).
For this first iteration we are going to add just the SP metadata, by the means of a in memory metadata provider, that takes his data from a 
generated entity (from the metadata generator), which itself respect the configuration of an extended metadata object, as shown below.

```java
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

```

But if you try to run this configuration, it will load and then crash, because it will complain of some null pointer exception. The fact is that you also 
need to initialize the XML generators , using the following 

```java
    @Bean
    public static SAMLBootstrap samlBootstrap() {
        return new SAMLBootstrap();
    }

```

Now you should be able to see the metadata on [http://localhost:8080/saml/metadata](http://localhost:8080/saml/metadata)!

# 2.3 Interfacing to remote metadata v1.0.3

Is now time to set the config for the external service. The first step is to set the entry point: in the configure method of the saml configuration,
we add an entry point

```java
http
...
...
                .httpBasic().authenticationEntryPoint(samlEntryPoint())
```

and of course the entry point itself

```java
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
```

We set the context provider (analyzes http requests and responses to get saml info), the web sso profile 
( with the list of supported transport methods; in the example there is the configuration that enables all that can be enabled)
some default options (basically setting the redirect binding) and the saml logger (that optionally dumps the saml payload to console,
so we can inspect it)

but this is not enough, we still need to provide the metadata of the identity provider. We will use the following


```java

    @Bean("idp-default")
    public ExtendedMetadataDelegate samlExtendedMetadataProvider() {
        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(idpMetadataProvider(), extendedMetadata());
        extendedMetadataDelegate.setMetadataTrustCheck(true);
        extendedMetadataDelegate.setMetadataRequireSignature(false);
        return extendedMetadataDelegate;
    }
```

in which we manipulate some settings and set the idp metadata provider, and here we actully put the xml link 

```java
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
```

and the extended metadata (which is how you specify some secondary options)

```java
    private ExtendedMetadata extendedMetadata() {
        final ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setLocal(true);
        extendedMetadata.setIdpDiscoveryEnabled(false);
        return extendedMetadata;
    }
```


From here, just start your application and log in with a user on the SSO: a session will be created for it, using the data from the idp provider instead
that the data from the local application.

## Special thanks 

To [YSoft](http://www.ysoft.com), for being a company where one can work productively, and to **Scan Team** For being the best team i have ever been in!

## Links and notable resources

- [Start up a Jade app with spring](http://josdem.io/techtalk/spring/spring_boot_jade/) (I prefer it to other techs for theses small projects)
- [Jade reference](http://jade-lang.com/reference) and [interactive doc](https://naltatis.github.io/jade-syntax-docs/)
