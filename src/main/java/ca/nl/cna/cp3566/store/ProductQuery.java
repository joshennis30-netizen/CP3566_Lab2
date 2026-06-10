package ca.nl.cna.cp3566.store;

/**
 * MODEL (given — do not change).
 *
 * All the catalogue search inputs in one object, instead of passing seven loose
 * arguments around. The {@link Products} resource builds one of these from the
 * query string and hands it to {@link ProductDao#search}.
 *
 * The normalising helpers below already clamp the paging for you — use them in
 * your SQL so you do not have to re-do the maths:
 *
 *   safePage()  -> page number, never below 1
 *   safeSize()  -> page size, clamped to 1..100
 *   offset()    -> the SQL OFFSET = (safePage() - 1) * safeSize()
 *
 * Any of q / category / minPrice / maxPrice / sort may be null or blank, meaning
 * "do not filter on this".
 */
public record ProductQuery(String q, String category, Double minPrice, Double maxPrice,
                           String sort, int page, int pageSize) {

    public int safePage() { return Math.max(1, page); }

    public int safeSize() { return Math.max(1, Math.min(pageSize, 100)); }

    public int offset() { return (safePage() - 1) * safeSize(); }
}
