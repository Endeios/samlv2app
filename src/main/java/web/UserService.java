package web;

import java.util.HashMap;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Service
public class UserService implements UserDetailsService {

    private static final HashMap<String, User> myUsers = makeUsers();

    public static List<SimpleGrantedAuthority> asAuthorities(String[] arrayOfAuthorities) {
        return stream(arrayOfAuthorities).map(SimpleGrantedAuthority::new).collect(toList());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return myUsers.get(username);
    }

    private static HashMap<String, User> makeUsers() {
        String[][] users = {
                {"admin", "admin", "USER", "ADMIN"},
                {"user", "user", "USER"},
                {"external", "external", "USER"},
                {"bruno", "bruno", "USER", "BRUNO"},
        };

        HashMap<String, User> usersDB = new HashMap<>();

        stream(users).forEach(user -> usersDB.put(user[0], makeUser(user)));

        return usersDB;
    }

    private static User makeUser(String[] user) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        final String[] arrayOfAuthorities = copyOfRange(user, 2, user.length);
        final List<SimpleGrantedAuthority> authorities =
                asAuthorities(arrayOfAuthorities);
        return new User(user[0], encoder.encode(user[1]), true, true, true, true, authorities);
    }
}
