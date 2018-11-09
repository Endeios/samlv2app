package web;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private MockMvc mockMvc;

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
        String ssoCircle = "https://idp.ssocircle.com";
        ResultActions result = mockMvc.perform(get("/saml/sp/discovery").param("idp", ssoCircle));
        logger.info(getRedirectedUrl(result));
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern(ssoCircle + ":443/sso/SSORedirect/**"));
    }


    @BeforeClass
    private void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()).build();
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
}
