package datapotter.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an enum for accessor generation: the {@code @Data} idea, reshaped for a type that cannot
 * extend anything.
 *
 * <p>Generates one sealed interface {@code Foo_I} carrying a {@code default} accessor per instance
 * field, which the enum picks up by implementing it:
 * <pre>
 * {@code @AsUuid(UuidEncoding.BASE64URL)}
 * {@code @EnumData}
 * public enum OrderCoverage implements Described, OrderCoverage_I {
 *     NONE("N-40v1pMekz01Qdm7NFXiA", "No order file in hand."),
 *     ORDER_HELD("atHGV0wnvUKZaRe7JTDoAw", "An order file is in hand.", "2018 SLP 4774");
 *
 *     final String uuid, description;
 *     final List&lt;String&gt; examples;
 *     OrderCoverage(String uuid, String description, String... examples) {
 *         this.uuid = uuid; this.description = description; this.examples = List.of(examples);
 *     }
 * }
 * </pre>
 * There is no {@code uuid()}, no {@code description()}, no {@code examples()} to write, and no
 * {@code self()} either.
 *
 * <h3>Why an interface rather than a generated base class</h3>
 * <p>{@code @Data} hangs its accessors on a generated {@code Foo_A} that the class extends. An enum
 * already extends {@code Enum} and cannot extend anything else, so the accessors have to arrive as
 * interface {@code default} methods, which need an instance to read fields from. That is
 * {@link EnumData_I#self()}, and because {@code Foo_I} is {@code sealed} and permits only
 * {@code Foo}, the generated interface can implement {@code self()} itself with a cast that is total
 * by construction. Hence nothing to write in the enum.
 *
 * <h3>Two requirements on the enum</h3>
 * <ul>
 *   <li><b>Fields must not be {@code private}</b> — an interface default in the same package reads
 *       them directly, so package-private is the narrowest that works. Same rule {@code @Data}
 *       places on a child class's fields, for the same reason.</li>
 *   <li><b>A shared hand-written interface stays listed on the enum</b> ({@code Described} above).
 *       That is how the generator learns to put it on {@code Foo_I}, and it has to land there: an
 *       abstract method inherited from one interface and a {@code default} inherited from an
 *       unrelated one is a compile error (JLS 9.4.1.3), so the two cannot sit side by side.</li>
 * </ul>
 *
 * <h3>Relationship to {@link AsUuid} / {@link AsName}</h3>
 * <p>Orthogonal, and composable. {@code @AsUuid}/{@code @AsName} declare what a constant becomes at
 * the storage boundary — a fact the enum's <em>consumers</em> need. {@code @EnumData} declares that
 * the enum's own accessors are generated. Where both are present the generator also emits
 * {@code Foo_I.fromStorage(String)}, the reflection-free reverse lookup, as a {@code switch} over
 * literals already known at compile time, and makes {@code Foo_I} extend {@link HasUuid} unless a
 * trait already reaches it.
 *
 * <p><b>No field has to hold the uuid, and none has to be named anything.</b> Where a field happens
 * to yield {@code uuid()}, that is the accessor. Where none does, {@code uuid()} is generated from
 * the same per-constant literals — so an id is intrinsic metadata of the constant, the way
 * {@link Enum#name()} is, rather than instance state the author must remember to declare.
 *
 * <p><b>Opt-in and additive.</b> An enum carrying only {@code @AsUuid}/{@code @AsName} behaves
 * exactly as before, hand-written accessors and all.
 *
 * @see EnumData_I
 * @see AsUuid
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EnumData {

    /**
     * Hand-written interfaces the generated {@code Foo_I} must extend, so the accessors it generates
     * satisfy their abstract methods.
     *
     * <p><b>Most interfaces do NOT belong here — declare those on the enum as usual.</b> The test is
     * narrow: name an interface here only when the GENERATED accessors are what satisfy its abstract
     * methods. Then it must land on {@code Foo_I} rather than beside it, because a default method on
     * {@code Foo_I} does not implement an abstract method of an interface {@code Foo_I} knows
     * nothing about, and javac rejects the enum for not overriding it (JLS 9.4.1.3 for the sibling
     * case, where both are inherited). Naming the trait here is what relates the two.
     *
     * <p>Measured, both ways:
     * <pre>
     * interface Described { String description(); }          // satisfied by a generated accessor
     * enum Foo implements Foo_I, Described { ... }           // ERROR: does not override description()
     * {@code @EnumData(traits = Described.class)}            // correct
     *
     * interface Vocab { String name(); default List&lt;String&gt; aliases() { ... } }
     * enum Foo implements Foo_I, Vocab { ... }               // fine: name() comes from Enum,
     * </pre>                                                 // aliases() has its own default
     *
     * <h3>Which form to use where BOTH compile — a design choice, not a rule</h3>
     *
     * <p>An interface that clashes with nothing can go either place, and the shorter form is a
     * legitimate choice:
     * <pre>
     * {@code @EnumData}                          enum Foo implements Foo_I, Vocab   // shorter
     * {@code @EnumData(traits = Vocab.class)}    enum Foo implements Foo_I          // sturdier
     * </pre>
     *
     * <p><b>What tips it is what happens LATER.</b> The short form compiles only while nothing the
     * trait declares is override-equivalent with a generated accessor. Add a field whose accessor
     * collides — an {@code aliases} field under a {@code Vocab} that defaults {@code aliases()} — and
     * it stops compiling with <i>"inherits unrelated defaults for aliases()"</i>, a message that does
     * not name the fix. Where that edit is expected rather than hypothetical, prefer {@code traits}.
     *
     * <p>The second consideration is consistency within a package. Where some enums must use
     * {@code traits} anyway, "short where possible" leaves two shapes side by side and a reader has
     * to work out which rule governs which enum. Both consumer projects chose one uniform shape for
     * that reason; a package where no trait can ever clash would reasonably choose the short one.
     *
     * <p>Naming a trait here leaves the enum implementing its generated interface and nothing else,
     * with {@code Foo_I} carrying the trait onward:
     * <pre>
     * {@code @AsUuid(UuidEncoding.BASE64URL)}
     * {@code @EnumData(traits = Described.class)}
     * public enum OrderCoverage implements OrderCoverage_I { ... }
     * </pre>
     *
     * <p>{@link HasUuid} is NOT listed here, ever. It follows from {@link AsUuid}, so the generator
     * adds it unconditionally — never merely when no trait happens to reach it, which would make
     * this enum's identity depend on someone else's declaration holding still.
     */
    Class<?>[] traits() default {};
}
