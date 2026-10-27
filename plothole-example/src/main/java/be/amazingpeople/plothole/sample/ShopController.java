package be.amazingpeople.plothole.sample;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** A tiny in-memory shop: just enough API to tell a story about. */
@RestController
class ShopController {

    record NewCustomer(String name) {
    }

    record Item(String sku, int quantity, int unitPriceCents) {
    }

    private static final class Basket {
        final long customerId;
        final List<Item> items = new CopyOnWriteArrayList<>();
        volatile boolean paid;

        Basket(long customerId) {
            this.customerId = customerId;
        }

        int totalCents() {
            return items.stream().mapToInt(item -> item.quantity() * item.unitPriceCents()).sum();
        }
    }

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, String> customers = new ConcurrentHashMap<>();
    private final Map<Long, Basket> baskets = new ConcurrentHashMap<>();

    @PostMapping("/customers")
    ResponseEntity<?> register(@RequestBody NewCustomer body) {
        long id = ids.incrementAndGet();
        customers.put(id, body.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(json("id", id, "name", body.name()));
    }

    @PostMapping("/customers/{id}/baskets")
    ResponseEntity<?> openBasket(@PathVariable("id") long customerId) {
        if (!customers.containsKey(customerId)) {
            return ResponseEntity.notFound().build();
        }
        long id = ids.incrementAndGet();
        baskets.put(id, new Basket(customerId));
        return ResponseEntity.status(HttpStatus.CREATED).body(json("id", id));
    }

    @PostMapping("/baskets/{id}/items")
    ResponseEntity<?> addItem(@PathVariable("id") long id, @RequestBody Item item) {
        Basket basket = baskets.get(id);
        if (basket == null) {
            return ResponseEntity.notFound().build();
        }
        if (basket.paid) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        basket.items.add(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(json("items", basket.items.size()));
    }

    @GetMapping("/baskets/{id}")
    ResponseEntity<?> basket(@PathVariable("id") long id) {
        Basket basket = baskets.get(id);
        if (basket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(json(
                "id", id,
                "items", basket.items,
                "totalCents", basket.totalCents(),
                "paid", basket.paid));
    }

    @PostMapping("/baskets/{id}/pay")
    ResponseEntity<?> pay(@PathVariable("id") long id) {
        Basket basket = baskets.get(id);
        if (basket == null) {
            return ResponseEntity.notFound().build();
        }
        if (basket.paid || basket.items.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        basket.paid = true;
        return ResponseEntity.ok(json("status", "PAID", "totalCents", basket.totalCents()));
    }

    @GetMapping("/customers/{id}/orders")
    List<Map<String, Object>> orders(@PathVariable("id") long customerId) {
        return baskets.entrySet().stream()
                .filter(entry -> entry.getValue().customerId == customerId && entry.getValue().paid)
                .map(entry -> json("basketId", entry.getKey(), "totalCents", entry.getValue().totalCents()))
                .toList();
    }

    private static Map<String, Object> json(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
