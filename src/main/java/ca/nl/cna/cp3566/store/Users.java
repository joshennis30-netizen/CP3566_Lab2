package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * WEB (given — do not change). Registration endpoint.
 *
 *   POST /api/users  { "name": "...", "email": "...", "password": "..." }
 *     -> 201 Created with the new User (no password), or
 *        422 (invalid input, with per-field messages) / 409 (email taken).
 *
 * The checks and hashing live in your {@link RegistrationService}; this method
 * just hands the body over and returns 201 on success.
 */
@Path("users")
@Produces(MediaType.APPLICATION_JSON)
public class Users {

    private final RegistrationService registration = AppContext.registrationService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterRequest req) {
        User user = registration.register(req);
        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/users/" + user.id())
                .entity(user)
                .build();
    }
}
