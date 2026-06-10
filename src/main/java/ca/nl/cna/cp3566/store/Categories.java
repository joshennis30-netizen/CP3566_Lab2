package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * WEB (given — do not change).
 *
 * GET /api/categories -> the list of category names for the dropdown, e.g.
 *   ["Accessories","Activewear","Coats & Jackets","Dresses", ...]
 *
 * Its own class so the word "categories" never collides with the numeric {id}
 * path on /products. Delegates to your ProductDao.categories().
 */
@Path("categories")
@Produces(MediaType.APPLICATION_JSON)
public class Categories {

    private final ProductDao products = AppContext.productDao();

    @GET
    public List<String> list() {
        return products.categories();
    }
}
