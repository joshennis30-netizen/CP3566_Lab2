package ca.nl.cna.cp3566.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * Malformed JSON in a request body comes out as a uniform 400 instead of a bare
 * framework error. (Main.java registers Jackson "without exception mappers" so
 * this one wins.)
 */
@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException ex) {
        return Response.status(400)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "bad_json",
                        "message", "Request body was not valid JSON."))
                .build();
    }
}
