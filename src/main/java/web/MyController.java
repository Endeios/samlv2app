package web;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MyController {

    private ItemService itemService;

    @Autowired
    public MyController(ItemService itemService) {
        this.itemService = itemService;
    }

    @RequestMapping("/")
    String index(Model model) {
        model.addAttribute("currentTime", new Date());
        model.addAttribute("title", "Public");
        model.addAttribute("items", itemService.getPublicItems());
        return "index";
    }

    @RequestMapping("/secret")
    String secret(Model model, Authentication authentication) {
        model.addAttribute("currentTime", new Date());
        model.addAttribute("title", "Secret");
        model.addAttribute("items", itemService.getuPrivateItems());
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        model.addAttribute("username", principal.getUsername());
        return "secret";
    }
}
