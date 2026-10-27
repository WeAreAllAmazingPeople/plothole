package be.amazingpeople.plothole.sample;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import be.amazingpeople.plothole.Chapter;
import be.amazingpeople.plothole.Paragraph;
import be.amazingpeople.plothole.PlotHoleStory;
import be.amazingpeople.plothole.Story;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.boot.test.web.server.LocalServerPort;

@Story("Customer places an order")
class OrderStory extends PlotHoleStory {

    @LocalServerPort
    int port;

    RequestSpecification shop;

    // The narrative state: what earlier chapters hand over to later ones.
    long customerId;
    long basketId;

    @BeforeAll
    void openTheShop() {
        shop = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
    }

    @Chapter(value = 1, title = "A customer registers")
    void register() {
        customerId = given().spec(shop)
                .body("""
                        {"name":"Ada"}
                        """)
                .when().post("/customers")
                .then().statusCode(201).body("name", equalTo("Ada"))
                .extract().jsonPath().getLong("id");
    }

    @Chapter(value = 2, title = "They fill their basket")
    class FillBasket {

        @Paragraph(value = 1, title = "an empty basket is opened")
        void open() {
            basketId = given().spec(shop)
                    .when().post("/customers/{id}/baskets", customerId)
                    .then().statusCode(201)
                    .extract().jsonPath().getLong("id");
        }

        @Paragraph(value = 2, title = "two copies of a book are added")
        void addBooks() {
            given().spec(shop)
                    .body("""
                            {"sku":"BOOK-1","quantity":2,"unitPriceCents":1250}
                            """)
                    .when().post("/baskets/{id}/items", basketId)
                    .then().statusCode(201);
        }

        @Paragraph(value = 3, title = "a pen is added")
        void addPen() {
            given().spec(shop)
                    .body("""
                            {"sku":"PEN-7","quantity":1,"unitPriceCents":300}
                            """)
                    .when().post("/baskets/{id}/items", basketId)
                    .then().statusCode(201).body("items", is(2));
        }

        @Paragraph(value = 4, title = "the total adds up")
        void total() {
            given().spec(shop)
                    .when().get("/baskets/{id}", basketId)
                    .then().statusCode(200)
                    .body("items", hasSize(2))
                    .body("totalCents", equalTo(2800));
        }
    }

    @Chapter(value = 3, title = "And they pay")
    void pay() {
        given().spec(shop)
                .when().post("/baskets/{id}/pay", basketId)
                .then().statusCode(200)
                .body("status", equalTo("PAID"))
                .body("totalCents", equalTo(2800));
    }

    @Chapter(value = 4, title = "The shop remembers them")
    void orderHistory() {
        given().spec(shop)
                .when().get("/customers/{id}/orders", customerId)
                .then().statusCode(200)
                .body("size()", is(1))
                .body("[0].totalCents", equalTo(2800));
    }
}
