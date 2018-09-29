package web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import web.domain.Item;

import static java.util.Arrays.stream;

@Service
public class ItemService {

    private static Object[][] secretItems = {
            {100L, "First Secret item", "1000,1"}
    };

    private static Object[][] punyItems = {
            {1L, "First Public item", "100,1"}
    };

    public List<Item> getPublicItems() {

        return stream(punyItems).map(Item::new).collect(Collectors.toList());
    }

    public List<Item> getuPrivateItems() {

        return stream(secretItems).map(Item::new).collect(Collectors.toList());

    }
}
