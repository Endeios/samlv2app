package web;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml.SamlAuthentication;

import static web.UserService.asAuthorities;

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
