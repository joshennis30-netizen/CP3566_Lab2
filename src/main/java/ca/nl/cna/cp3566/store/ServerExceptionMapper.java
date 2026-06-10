package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * The last-resort mapper for anything we did NOT plan for: a NullPointerException,
 * a bug, or — in this starter — the UnsupportedOperationException thrown by a
 * method you have not implemented yet. The more specific mappers (ApiException,
 * WebApplicationException, JsonProcessingException) win over this one; only the
 * truly-unexpected lands here. It logs the stack trace to the console (so you can
 * debug) and returns a uniform 500 JSON instead of a framework HTML page.
 */
@Provider
public class ServerExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        ex.printStackTrace();   // so you can see what broke in the server console
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "server_error", "message", message))
                .build();
    }
}
