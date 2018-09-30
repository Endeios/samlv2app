package web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static web.UserService.asAuthorities;

@Test
public class UserServiceTest  extends AbstractTestNGSpringContextTests {
    @Test
    public void givenUserService_whenAskingForASpecificUser_thenTheRightUserIsRetrieved() {
        UserService service = new UserService();
        UserDetails user = service.loadUserByUsername("bruno");
        assertTrue(new BCryptPasswordEncoder().matches("bruno",user.getPassword()));
        List<SimpleGrantedAuthority> expected = asAuthorities(new String[]{"USER", "BRUNO"});
        List<? extends GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
        assertEquals(expected.size(), authorities.size());
        authorities.removeAll(expected);
        assertTrue(authorities.size()==0);
    }
}
