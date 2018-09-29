package web;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import web.domain.User;

import static org.testng.AssertJUnit.assertEquals;

public class UserServiceTest  extends AbstractTestNGSpringContextTests {
    @Test
    public void givenUserService_whenAskingForASpecificUser_thenTheRightUserIsRetrieved() {
        UserService service = new UserService();
        User user = service.getUserByName("bruno");
        assertEquals(user.getPassword(),"bruno");
        final String[] expected = {"USER","BRUNO"};
        assertEquals(expected.length,user.getRoles().length);
        assertEquals(expected[0],user.getRoles()[0]);
        assertEquals(expected[1],user.getRoles()[1]);
    }
}
