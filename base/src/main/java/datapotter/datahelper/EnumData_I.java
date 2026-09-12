package datapotter.datahelper;

/**
 * Root of every {@code @EnumData}-generated {@code Foo_I} interface, exactly as
 * {@link DataHelper_I} is the root of every {@code @Data}-generated {@code Foo_I}. The suffix names
 * the generated Java kind, never the source kind — an interface is {@code _I} whether it was
 * generated from a class or from an enum, and the two can never collide because an enum and a class
 * of the same name cannot share a package.
 *
 * <p>Carries the one thing an interface default needs that an interface cannot otherwise have: the
 * instance whose fields it is about to read. Generated accessors are all of the shape
 * {@code default String description() { return self().description; }}.
 *
 * <p>{@link #self()} is <em>not</em> implemented by the enum. The generated {@code Foo_I} is
 * {@code sealed} and permits only {@code Foo}, so it supplies
 * {@code default Foo self() { return (Foo) this; }} — a cast the seal makes total.
 *
 * @param <E> the enum implementing this, which is also the only permitted subtype of its own
 *            generated interface
 * @see EnumData
 */
public interface EnumData_I<E extends Enum<E> & EnumData_I<E>> {

    /** This constant, typed as the concrete enum. Supplied by the generated interface. */
    E self();
}
