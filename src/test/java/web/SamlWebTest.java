package web;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.helper.SamlTestObjectHelper;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml.saml2.authentication.Response;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

@WebAppConfiguration
@SpringBootTest
@ComponentScan(basePackageClasses = App.class)
@Test
public class SamlWebTest extends AbstractTestNGSpringContextTests {

    private static Logger logger = LoggerFactory.getLogger(SamlWebTest.class);

    @Autowired
    MockHttpSession session;
    @Autowired
    MockHttpServletRequest request;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    @Qualifier("spSamlServerConfiguration")
    private SamlServerConfiguration config;
    @Autowired
    private SamlTransformer transformer;
    @Autowired
    private SamlProviderProvisioning<ServiceProviderService> provisioning;
    @Autowired
    Clock samlTime;

    private MockMvc mockMvc;
    private SamlTestObjectHelper helper;

    private static final String IDP_SSOCIRCLE_COM = "https://idp.ssocircle.com";

    @Test
    public void testGetMetadataPageCanBeAccessedAnonymously() throws Exception {
        String url = "/saml/sp/metadata";
        ResultActions result = mockMvc.perform(get(url))
                .andExpect(status().is2xxSuccessful());

        logger.info(result.andReturn().getResponse().getContentAsString());
        logger.info(result.andReturn().getResponse().getErrorMessage());
        final Collection<String> headerNames = result.andReturn().getResponse().getHeaderNames();
        for (String name : headerNames) {

            logger.info(name + ":" + result.andReturn().getResponse().getHeader(name));
        }
        logger.info("Status :" + result.andReturn().getResponse().getStatus());
    }

    @Test
    public void testRedirect() throws Exception {
        ResultActions result = mockMvc.perform(get("/saml/sp/discovery").param("idp", IDP_SSOCIRCLE_COM));
        logger.info(getRedirectedUrl(result));
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern(IDP_SSOCIRCLE_COM + ":443/sso/SSORedirect/**"));
    }

    @Test
    public void testAuthnRequest() throws Exception {
        AuthenticationRequest authn = getAuthenticationRequest();
        assertNotNull(authn);
    }

    @Test
    public void processResponse() throws Exception {
        ServiceProviderService provider = provisioning.getHostedProvider();
        config.getServiceProvider().setWantAssertionsSigned(false);
        String idpEntityId = "https://idp.ssocircle.com";
        AuthenticationRequest authn = getAuthenticationRequest();
        IdentityProviderMetadata idp = provider.getRemoteProvider(idpEntityId);
        ServiceProviderMetadata sp = provider.getMetadata();
        final String principal = "test-user@test.com";
        Assertion assertion = helper.assertion(sp, idp, authn, principal, NameId.PERSISTENT);
        assertion.setAttributes(makeAttributesWithUserID(principal));
        Response response = helper.response(
                authn,
                assertion,
                sp,
                idp
        );

        String xmlResponse = transformer.toXml(response);
        logger.info("<<<<<<<<<<<<<\n\n " + prettyPrintDocument(xmlResponse));
        String encoded = transformer.samlEncode(xmlResponse, false);
        mockMvc.perform(
                post("/saml/sp/SSO/alias/safeq6")
                        .param("SAMLResponse", encoded)
        )
                .andExpect(status().isFound())
                .andExpect(authenticated().withUsername(principal));
    }

    private AuthenticationRequest getAuthenticationRequest() throws Exception {
        String redirect = mockMvc.perform(
                get("/saml/sp/discovery/alias/" + config.getServiceProvider().getAlias())
                        .param("idp", IDP_SSOCIRCLE_COM)
        )
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        assertNotNull(redirect);
        Map<String, String> params = queryParams(new URI(redirect));
        assertNotNull(params);
        assertFalse(params.isEmpty());
        String request = params.get("SAMLRequest");
        assertNotNull(request);
        String xml = transformer.samlDecode(request, true);
        logger.info(">>>>>>>>>>>>>>>>>>>>>>\n\n"+ prettyPrintDocument(xml));
        return (AuthenticationRequest) transformer.fromXml(xml, null, null);
    }
    private List<Attribute> makeAttributesWithUserID(String principal) {
        final List<Attribute> attributes = new ArrayList<>();
        Attribute userIdAttribute = new Attribute();
        userIdAttribute.setName("UserID");
        userIdAttribute.setValues(Collections.singletonList(principal));
        attributes.add(userIdAttribute);
        return attributes;
    }
    @BeforeClass
    private void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()).build();
        /*
        very useful object i got from the samlv2 sample project test: i really hope they put it in
        some test package, because enables testing without a provider, forging the responses!
        */
        helper = new SamlTestObjectHelper(samlTime);
    }

    private MockHttpServletResponse getResponse(ResultActions result) {
        return result.andReturn().getResponse();
    }

    private String getRedirectedUrl(ResultActions result) {
        return getResponse(result).getRedirectedUrl();
    }

    @BeforeMethod
    private void beforeMethod() {
    }

    private static Map<String, String> queryParams(URI url) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(
                    UriUtils.decode(pair.substring(0, idx), UTF_8.name()),
                    UriUtils.decode(pair.substring(idx + 1), UTF_8.name())
            );
        }
        return queryPairs;
    }

    private static String prettyPrintDocument(String xmlString) throws TransformerException, IOException, ParserConfigurationException, SAXException {

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xmlString)));

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
