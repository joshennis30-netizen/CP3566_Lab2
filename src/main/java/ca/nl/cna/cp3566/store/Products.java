package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * WEB (given — do not change). The catalogue HTTP endpoints.
 *
 * Notice how thin this is: it parses the query string into a {@link ProductQuery},
 * hands it to the {@link ProductDao} (your code), and shapes the HTTP response.
 * It has NO idea whether the data lives in SQL, a file, or memory — it depends
 * only on the abstract ProductDao. That is the layering you are implementing
 * behind it. It will start working the moment your JdbcProductDao does.
 *
 *   GET /api/products?q=&category=&minPrice=&maxPrice=&sort=&page=&pageSize=
 *       -> JSON array for that page + X-Total-Count header + RFC-5988 Link header
 *   GET /api/products/{id}  -> one product, or a uniform 404
 */
@Path("products")
@Produces(MediaType.APPLICATION_JSON)
public class Products {

    private final ProductDao products = AppContext.productDao();

    @GET
    public Response browse(
            @QueryParam("q") String q,
            @QueryParam("category") String category,
            @QueryParam("minPrice") Double minPrice,
            @QueryParam("maxPrice") Double maxPrice,
            @QueryParam("sort") String sort,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("24") int pageSize) {

        ProductQuery query = new ProductQuery(q, category, minPrice, maxPrice, sort, page, pageSize);
        Page result = products.search(query);

        Response.ResponseBuilder rb = Response.ok(result.items())
                .header("X-Total-Count", result.total());

        String link = linkHeader(query, result);
        if (!link.isEmpty()) rb.header("Link", link);
        return rb.build();
    }

    @GET
    @Path("{id}")
    public Product one(@PathParam("id") int id) {
        return products.findById(id)
                .orElseThrow(() -> ApiException.notFound("No product with id " + id));
    }

    // ---- RFC-5988 pagination links (given) ---------------------------------

    private static String linkHeader(ProductQuery q, Page page) {
        int size = page.pageSize();
        int current = page.page();
        int last = Math.max(1, (int) Math.ceil(page.total() / (double) size));

        List<String> parts = new ArrayList<>();
        if (current < last) parts.add(rel(q, current + 1, size, "next"));
        if (current > 1)    parts.add(rel(q, current - 1, size, "prev"));
        parts.add(rel(q, last, size, "last"));
        return String.join(", ", parts);
    }

    private static String rel(ProductQuery q, int page, int pageSize, String rel) {
        StringBuilder url = new StringBuilder("/api/products?page=").append(page)
                .append("&pageSize=").append(pageSize);
        addParam(url, "q", q.q());
        addParam(url, "category", q.category());
        addParam(url, "minPrice", q.minPrice() == null ? null : String.valueOf(q.minPrice()));
        addParam(url, "maxPrice", q.maxPrice() == null ? null : String.valueOf(q.maxPrice()));
        addParam(url, "sort", q.sort());
        return "<" + url + ">; rel=\"" + rel + "\"";
    }

    private static void addParam(StringBuilder url, String name, String value) {
        if (value == null || value.isBlank()) return;
        url.append('&').append(name).append('=')
           .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
