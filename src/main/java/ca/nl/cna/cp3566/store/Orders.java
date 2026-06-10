package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * WEB (given — do not change). The order HTTP endpoints.
 *
 * Again, thin: it calls your {@link CheckoutService} / {@link OrderDao} and maps
 * the result to HTTP. All the interesting logic (validation, stock, pricing,
 * transaction) lives behind the service, which is exactly where you implement it.
 *
 *   POST /api/orders                -> 201 Created (+ Location), or 200 on an
 *                                      idempotent replay; errors are 422/404/409.
 *   GET  /api/orders/{orderNumber}  -> 200 with the order, or 404.
 */
@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
public class Orders {

    private final CheckoutService checkout = AppContext.checkoutService();
    private final OrderDao orders = AppContext.orderDao();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderRequest req,
                                @HeaderParam("Idempotency-Key") String idempotencyKey) {

        CheckoutResult result = checkout.checkout(req, idempotencyKey);
        Order order = result.order();
        String location = "/api/orders/" + order.orderNumber();

        if (result.replayed()) {
            // Nothing new was created — 200, and a header so a client can tell.
            return Response.ok(order)
                    .header("Location", location)
                    .header("Idempotency-Replayed", "true")
                    .build();
        }
        return Response.status(Response.Status.CREATED)
                .header("Location", location)
                .entity(order)
                .build();
    }

    @GET
    @Path("{orderNumber}")
    public Order getOrder(@PathParam("orderNumber") String orderNumber) {
        return orders.findByNumber(orderNumber)
                .orElseThrow(() -> ApiException.notFound("No order with number " + orderNumber));
    }
}
