package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * The one place an ApiException becomes an HTTP response. Jersey finds it via
 * @Provider (the same package scan that finds the @Path classes). Throw
 * ApiException.conflict("...") anywhere and it comes out as:
 *   HTTP 409
 *   { "error": "conflict", "message": "..." }
 * Registration validation also attaches a "fields" object.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.error());
        body.put("message", ex.getMessage() == null ? "" : ex.getMessage());
        if (ex.fields() != null) body.put("fields", ex.fields());
        return Response.status(ex.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
