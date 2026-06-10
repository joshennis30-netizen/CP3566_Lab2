package ca.nl.cna.cp3566.store;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * ARCHITECTURE (given — you implement validate() in StandardRegistrationService).
 *
 * The registration TEMPLATE METHOD, the same shape as {@link CheckoutService}.
 * {@link #register} is {@code final}: it fixes the safe sequence and you only fill
 * the validation rules.
 *
 * The fixed flow:
 *   1. validate(req)               YOUR step: field checks -> 422 with messages
 *   2. hash the password           (PasswordHash.create — never store plaintext)
 *   3. in ONE transaction:
 *        a. emailExists?           -> 409 Conflict (and the UNIQUE column backs it up)
 *        b. users.insert(...)      create the account, get the new id
 *   4. return the User (no password field — safe to send back)
 *
 * Notice you never get to skip hashing or the duplicate check: the structure
 * guarantees them. You decide only WHAT counts as valid input.
 */
public abstract class RegistrationService {

    protected final UserDao users;

    protected RegistrationService(UserDao users) {
        this.users = users;
    }

    /** THE TEMPLATE METHOD — do not override (it is final). */
    public final User register(RegisterRequest req) {
        Map<String, String> errors = validate(req);            // step 1 (YOUR step)
        if (!errors.isEmpty()) throw ApiException.validation(errors);

        String name = req.name().trim();
        String emailLower = req.email().trim().toLowerCase();
        String hash = PasswordHash.create(req.password());     // step 2 (given)
        String createdAt = OffsetDateTime.now().toString();

        int id = Database.inTransaction(c -> {                 // step 3 (one transaction)
            if (users.emailExists(c, emailLower)) {
                throw ApiException.conflict("An account with that email already exists.");
            }
            return users.insert(c, name, emailLower, hash, createdAt);
        });

        return new User(id, name, req.email().trim());         // step 4
    }

    /**
     * YOUR step — return a map of {field -> message} for every invalid field, or
     * an EMPTY map if the input is fine. Suggested rules (LAB2.docx): name not
     * blank; email looks like an email; password at least 8 characters. The keys
     * you use ("name", "email", "password") appear in the 422 response's "fields".
     */
    protected abstract Map<String, String> validate(RegisterRequest req);
}
