package ca.nl.cna.cp3566.store;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ====================  YOUR CODE  ====================
 * The concrete registration. The abstract {@link RegistrationService} already
 * hashes the password, checks for a duplicate email (409), and inserts the user
 * in a transaction. You implement only the validation rules.
 * =====================================================
 */
public class StandardRegistrationService extends RegistrationService {

    public StandardRegistrationService(UserDao users) {
        super(users);   // wiring handed in by AppContext — leave as-is
    }

    @Override
    protected Map<String, String> validate(RegisterRequest req) {
        // TODO: return a map of {field -> message} for every invalid field, or an
        //       EMPTY map if everything is fine. Suggested rules:
        //   - "name":     required (not null / not blank)
        //   - "email":    must look like an email — a simple regex is enough, e.g.
        //                 "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
        //   - "password": at least 8 characters
        // Example:
        //   Map<String,String> errors = new LinkedHashMap<>();
        //   if (req == null || req.name() == null || req.name().isBlank())
        //       errors.put("name", "Name is required.");
        //   ... etc ...
        //   return errors;
        throw new UnsupportedOperationException("TODO: implement validate");
    }
}
