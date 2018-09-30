package web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@SpringBootTest
@ComponentScan(basePackageClasses = App.class)
@Test
public class BasicWebTest extends AbstractTestNGSpringContextTests {

    private static Logger logger = LoggerFactory.getLogger(BasicWebTest.class);
    @Autowired
    MockHttpSession session;
    @Autowired
    MockHttpServletRequest request;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;


    @Test
    public void testGetIndexPage() throws Exception {
        String url = "/";
        mockMvc.perform(get(url))
                .andExpect(status().is2xxSuccessful())
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("title"))
        ;
    }

    @Test
    public void testGetSecretPage() throws Exception {
        String url = "/secret";
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"))
        ;
    }

    @Test
    public void testGetSecretPageWithUser() throws Exception {
        String url = "/secret";
        mockMvc.perform(
                    get(url)
                    .with(user("bruno").password("pass").roles("USER","BRUNO"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("title"))
        ;
    }

    @BeforeClass
    private void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()).build();
    }

    @BeforeMethod
    private void beforeMethod() {
    }
}
