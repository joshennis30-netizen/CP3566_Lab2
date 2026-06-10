package ca.nl.cna.cp3566.store;

/**
 * ARCHITECTURE (given — the one wiring point. You normally leave this as-is.)
 *
 * This is the app's "composition root": the single place that decides WHICH
 * concrete classes are used. The web resources ask AppContext for a ProductDao /
 * OrderDao / service; AppContext hands back your concrete implementations. Doing
 * the wiring here (not inside the resources) is a poor-man's dependency
 * injection — the resources stay ignorant of JDBC, and you could swap an
 * implementation in ONE place.
 *
 * It is already wired to the concrete classes you will complete:
 *   JdbcProductDao, JdbcOrderDao, JdbcUserDao,
 *   StandardCheckoutService, StandardRegistrationService.
 *
 * The DAOs are stateless, so we make a single shared instance of each (cheap and
 * fine). If you add your OWN implementation classes, point these methods at them.
 */
public final class AppContext {

    private static final ProductDao PRODUCTS = new JdbcProductDao();
    private static final OrderDao   ORDERS   = new JdbcOrderDao();
    private static final UserDao    USERS    = new JdbcUserDao();

    private static final CheckoutService CHECKOUT =
            new StandardCheckoutService(PRODUCTS, ORDERS);
    private static final RegistrationService REGISTRATION =
            new StandardRegistrationService(USERS);

    private AppContext() { }

    public static ProductDao productDao() { return PRODUCTS; }
    public static OrderDao orderDao() { return ORDERS; }
    public static UserDao userDao() { return USERS; }
    public static CheckoutService checkoutService() { return CHECKOUT; }
    public static RegistrationService registrationService() { return REGISTRATION; }
}
