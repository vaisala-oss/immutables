/*
    Copyright 2014-2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * This annotation provides namespace for annotations for immutable value object generation.
 * Use one of the nested annotation.
 * @see Value.Immutable
 * @see Value.Include
 * @see Value.Enclosing
 */
// @Target({}) // may hide from auto-completion
@Retention(RetentionPolicy.SOURCE)
public @interface Value {
  /**
   * Instruct processor to generate immutable implementation of abstract value type.
   * Classes, Interface and Annotation types are supported including top level and non-private
   * static inner types.
   * <p>
   * Annotation has attributes to configure generation of immutable implementation classes, which
   * are usually configured per-type: should the builder ({@link #builder()}) be generated or
   * instances interned ({@link #intern()}). You can use {@link Style} or custom style annotation to
   * tune naming conventions and other settings of code-generation, along with default value for
   * per-type attributes ({@link Style#defaults()})
   * <p>
   * Generated accessor methods have annotation copied from original accessor method. However
   * {@code org.immutables.*} and {@code java.lang.*} are not copied.
   * <em>Be warned that such immutable object may contain attributes with types that are not
   * guaranteed to be immutable, thus not every object will be recursively immutable.
   * While this may be useful in some cases,
   * one should generally avoid creating immutable object with attribute values that could be mutated.</em>
   * <p>
   * @see Style
   * @see Include
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface Immutable {

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}. To access singleton instance use
     * {@code .of()} static accessor method.
     * <p>
     * This requires that all attributes have default value (including collections which can be left
     * empty). If some required attributes exist it will result in compilation error. Note that in
     * case object do not have attributes, singleton instance will be generated automatically.
     * <p>
     * Note that {@code singleton=true} does not imply that only one instance of given abstract
     * type. But it does mean that only one "default" instance of the immutable implementation type
     * exist.
     * @return if generate singleton default instance
     */
    boolean singleton() default false;

    /**
     * If {@code intern=true} then instances will be strong interned on construction.
     * Default is {@literal false}.
     * @return if generate strongly interned instances
     */
    boolean intern() default false;

    /**
     * If {@code copy=false} then generation of copying methods will be disabled.
     * This appies to static "copyOf" methods as well as modify-by-copy "withAttributeName" methods
     * which returns modified copy using structural sharing where possible.
     * Default value is {@literal true}, i.e generate copy methods.
     * @return if generate copy methods
     */
    boolean copy() default true;

    /**
     * If {@code prehash=true} then {@code hashCode} will be precomputed during construction.
     * This could speed up collection lookups for objects with lots of attributes and nested
     * objects.
     * In general, use this when {@code hashCode} computation is expensive and will be used a lot.
     * Note that if {@link Style#privateNoargConstructor()} == <code>true</code> this option will be
     * ignored.
     * @return if generate hash code precomputing
     */
    boolean prehash() default false;

    /**
     * If {@code builder=false}, disables generation of {@code builder()}. Default is
     * {@literal true}.
     * @return if generate builder
     */
    boolean builder() default true;
  }

  /**
   * Includes specified abstract value types into generation of processing.
   * This is usually used to generate immutable implementation of classes from different
   * packages that source code cannot be changed to place {@literal @}{@code Value.Immutable}.
   * Only public types of suppored kinds is supported (see {@link Value.Immutable}).
   */
  @Documented
  @Target({ElementType.TYPE, ElementType.PACKAGE})
  public @interface Include {
    Class<?>[] value();
  }

  /**
   * This annotation could be applied to top level class which contains nested abstract
   * value types to provide namespacing for the generated implementation classes.
   * Immutable implementation classes will be generated as classes enclosed into special "umbrella"
   * top level class, essentialy named after annotated class with "Immutable" prefix (prefix could
   * be customized using {@link Style#typeImmutableEnclosing()}). This could mix
   * with {@link Value.Immutable} annotation, so immutable implementation class will contains
   * nested immutable implementation classes.
   * <p>
   * Implementation classes nested under top level class with "Immutable" prefix
   * <ul>
   * <li>Have simple names without "Immutable" prefix
   * <li>Could be star-imported for easy clutter-free usage.
   * </ul>
   * <p>
   *
   * <pre>
   * {@literal @}Value.Enclosing
   * class GraphPrimitives {
   *   {@literal @}Value.Immutable
   *   interace Vertex {}
   *   {@literal @}Value.Immutable
   *   static class Edge {}
   * }
   * ...
   * import ...ImmutableGraphPrimitives.*;
   * ...
   * Edge.builder().build();
   * Vertex.builder().build();
   * </pre>
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface Enclosing {}

  /**
   * This kind of attribute cannot be set during building, but they are eagerly computed from other
   * attributes and stored in field. Should be applied to non-abstract method - attribute value
   * initializer.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Derived {}

  /**
   * Annotates accessor that should be turned in set-able generated attribute. However, it is
   * non-mandatory to set it via builder. Default value will be assigned to attribute if none
   * supplied, this value will be obtained by calling method annotated this annotation.
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  public @interface Default {}

  /**
   * Annotate attribute as <em>auxiliary</em> and it will be stored and will be accessible, but will
   * be excluded from generated {@code equals}, {@code hashCode} and {@code toString} methods.
   * {@link Lazy Lazy} attributes are always <em>auxiliary</em>.
   * @see Value.Immutable
   * @see Value.Derived
   * @see Value.Default
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  public @interface Auxiliary {}

  /**
   * Lazy attributes cannot be set, defined as method that computes value, which is invoke lazily
   * once and only once in a thread safe manner.
   *
   * <pre>
   * &#064;Value.Immutable
   * public abstract class Order {
   * 
   *   public abstract List&lt;Item&gt; items();
   * 
   *   &#064;Value.Lazy
   *   public int totalCost() {
   *     int cost = 0;
   * 
   *     for (Item i : items())
   *       cost += i.count() * i.price();
   * 
   *     return cost;
   *   }
   * }
   * </pre>
   * <p>
   * This kind of attribute cannot be set during building, but they are lazily computed from other
   * attributes and stored in non-final field, but initialization is guarded by synchronization with
   * volatile field check. Should be applied to non-abstract method - attribute value initializer.
   * <p>
   * In general, lazy attribute initializer is more safe than using {@link Derived} attributes, lazy
   * attribute's initializer method body can refer to abstract mandatory and container attributes as
   * well as to other lazy attributes. Though lazy attributes act as {@link Auxiliary}.
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  public @interface Lazy {}

  /**
   * Works with {@link Value.Immutable} classes to mark abstract accessor method be included as
   * "{@code of(..)}" constructor parameter.
   * <p>
   * Following rules applies:
   * <ul>
   * <li>No constructor generated if none of methods have {@link Value.Parameter} annotation</li>
   * <li>For object to be constructible with a constructor - all non-default and non-derived
   * attributes should be annotated with {@link Value.Parameter}.
   * </ul>
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @Retention(RetentionPolicy.CLASS)
  public @interface Parameter {
    /**
     * Used to specify order of constructor argument. It defaults to zero and allows for
     * non-contiguous order values (arguments are sorted ascending by this order value).
     * <p>
     * <em>This attribute was introduced as potentially not all annotation processors could use source
     * order of elements, i.e. order of declaration appearance in a source file.
     * To support portable constructor argument definitions,
     * developer should supply argument order explicitly.
     * As of version 1.0, we implemented workaround for
     * the Eclipse compiler, so it is not strictly needed to specify order,
     * but it still might be needed if you wish to reorder arguments</em>
     * @return order
     */
    int order() default 0;
  }

  /**
   * Annotates method that should be invoked internally to validate invariants
   * after instance had been created, but before returned to a client.
   * Annotated method must be parameter-less (non-private) method and have a {@code void} return
   * type,
   * which also should not throw a checked exceptions.
   * <p>
   * Precondition checking should not be used to validate against context dependent business rules,
   * but to preserve consistency and guarantee that instances will be usable. Precondition check
   * methods runs when immutable object <em>instantiated and all attributes are initialized</em>,
   * but <em>before returned to caller</em>. Any instance that failed precondition check is
   * unreachable to caller due to runtime exception.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Check {}

  /**
   * Specified natural ordering for the implemented {@link SortedSet}, {@link NavigableSet} or
   * {@link SortedMap}, {@link NavigableMap}. It an error to annotate
   * sorted collection of elements which are not implementing {@link Comparable}.
   * Non-annotated special collection will be
   * generated/implemented as "nothing-special" attributes.
   * @see ReverseOrder
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  public @interface NaturalOrder {}

  /**
   * Specified reversed natural ordering for the implemented {@link SortedSet}, {@link NavigableSet}
   * or {@link SortedMap}, {@link NavigableMap}. It an error to annotate
   * sorted collection of elements which are not implementing {@link Comparable}.
   * Non-annotated special collection will be
   * generated/implemented as "nothing-special" attributes.
   * @see Collections#reverseOrder()
   * @see NaturalOrder
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface ReverseOrder {}

  /**
   * Generate modifiable implementation of abstract value class. Modifiable implementation class
   * might be useful when you either need overflexible builder or, alternatively, partially built
   * representation of value type.
   * This annotation could be used as companion to {@link Immutable} to
   * provide modifiable variant that is convertible back and forth to immutable form. When it is
   * used in a standalone manner, i.e. without using
   * <p>
   * Generated class will have name with "Modifiable" prefix by default which can be customizable
   * using {@link Style#typeModifiable() "typeModifiable" style}. Use {@code create()} factory
   * method to create instances or using "new" operator which is also depends on
   * {@link Style#create() "create" style} . Generated modifiable class will have setter methods
   * that return {@code this} for chained invocation. Getters will be of the same shape as defined
   * by abstract value types.
   * <p>
   * <em>Note: unlike {@literal @}{@link Immutable}, this annotation has very little of additional "magic"
   * and customisations implemented. Annotation like {@link Include}, {@link Enclosing}, {@link Lazy} do not work with
   * modifiable implementation</em>
   * <p>
   * <em>This is beta functionality that is likely to change</em>
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.CLASS)
  public @interface Modifiable {}

  /**
   * Naming and structural style could be used to customize convention of the generated
   * immutable implementations and companion classes. It could be placed on a class or package
   * directly or serve as meta annotation. When used as meta-annotation, then annotation could
   * be placed on a class, surrounding top level class or even a package (declared in
   * {@code package-info.java}). This
   * annotation more of example of how to define your own styles as meta-annotation rather than a
   * useful annotation. When using meta-annotation
   * <p>
   * <em>
   * Be careful to not use keywords or inappropriate characters as parts of naming templates.
   * Some sneaky collisions may only manifest as compilation errors in generated code.</em>
   * <p>
   * <em>Specific styles will be ignored for a immutable type enclosed with class which is annotated
   * as {@literal @}{@link Value.Enclosing}. So define styles on the enclosing class.
   * In this way there will be no issues with the naming and structural conventions
   * mismatch on enclosing and nested types.</em>
   */
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  public @interface Style {
    /**
     * Patterns to recognize accessors. For example <code>get = {"is*", "get*"}</code> will
     * mimick style of bean getters. If none specified or if none matches, then raw accessor name
     * will be taken literally.
     * <p>
     * By default only {@code get*} prefix is recognized, along with falling back to use accessor
     * name literally. It is up to you if you want to use "get" prefixes or not. Original author is
     * leaning towards not using noisy prefixes for attributes in immutable objects, drawing
     * similarity with annotation attributes, however usage of "get" is neither recommended, nor
     * discouraged.
     * <p>
     * <em>This is detection pattern, not formatting pattern. It defines how to recognize name, not how to derive name</em>
     * @return naming template
     */
    String[] get() default "get*";

    /**
     * Builder initialization method. i.e. "setter" in builder.
     * Do not confuse with {@link #set()}
     * @return naming template
     */
    String init() default "*";

    /**
     * Modify-by-copy "with" method.
     * @see #init()
     * @see #set()
     * @return naming template
     */
    String with() default "with*";

    /**
     * Add value to collection attribute from iterable
     * @return naming template
     */
    String add() default "add*";

    /**
     * Add all values to collection attribute from iterable
     * @return naming template
     */
    String addAll() default "addAll*";

    /**
     * Puts entry to a map attribute
     * @return naming template
     */
    String put() default "put*";

    /**
     * Puts all entries to a map attribute
     * @return naming template
     */
    String putAll() default "putAll*";

    /**
     * Copy constructor method name.
     * @return naming template
     */
    String copyOf() default "copyOf";

    /**
     * Constructor method name.
     * <p>
     * Since version {@code 2.1.5} you can also use "new" template string to generate public
     * constructor instead of factory. The public constructor functionality is experimentatal. Note
     * that having public constructor configured will not work if {@link Check} or
     * {@link Immutable#singleton()} is used and certain other functionality. In such cases compile
     * error would be raised.
     * @return naming template
     */
    String of() default "of";

    /**
     * Singleton accessor method name
     * @return naming template
     */
    String instance() default "of";

    /**
     * Builder creator method. This naming allow special keyword "new" value.
     * This will customize builder to be created using constructor rather than
     * factory method.
     * @see #newBuilder()
     * @see #build()
     * @return naming template
     */
    String builder() default "builder";

    /**
     * Builder creator method, it differes from {@link #builder()} in that this naming is used for
     * builders that are external to immutable objects, such as top level builders for values or
     * factories. This naming allow special keyword "new" value, which is the default.
     * "new" will customize builder to be created using constructor rather than
     * factory method.
     * @see #build()
     * @return naming template
     */
    String newBuilder() default "new";

    /**
     * Method to initialize builder with values from instance.
     * @return naming template
     */
    String from() default "from";

    /**
     * Instance creation method on builder.
     * @return naming template
     */
    String build() default "build";

    /**
     * Naming template for the {@code buildOrThrow} method which accept exception factory function
     * for throwing in case not all mandatory properties are set. Non-default (i.e. not empty)
     * template will essentially enable generation of this method, acting both as a naming template
     * and as a feature flag.
     * <p>
     * Generation of build or throws method requires presense of a function type on the classpath,
     * provided either by Java 8 or Guava on Java 7. If used on java 7 without Guava, this style
     * will have no effect: no method will be generated.
     * <p>
     * <em>Note: This attribute-style is experimental and may be changed in near releases.
     * You should not rely on it if don't ready to change code on minor updates of the annotation processor</em>
     * @return naming template
     */
    String buildOrThrow() default "";

    /**
     * Method to determine if all required attributes are set.
     * Default method name choice for this is mostly random.
     * @return naming template
     */
    String isInitialized() default "isInitialized";

    /**
     * Method to determine if attribute is set
     * @return naming template
     */
    String isSet() default "*IsSet";

    /**
     * Modifiable object "setter" method. Used for mutable implementations.
     * Do not confuse with {@link #init()}
     * @return naming template
     */
    String set() default "set*";

    /**
     * Unset attribute method. Used for mutable implementations.
     * @return naming template
     */
    String unset() default "unset*";

    /**
     * Clear all collection attributes and unset(or other container). Used for mutable
     * implementations.
     * @return naming template
     */
    String clear() default "clear";

    /**
     * Factory method for modifiable implementation, could be "new" to create objects using
     * constructor.
     * @return naming template
     */
    String create() default "create";

    /**
     * Method to convert to instanse of modifiable type to "canonical" immutable instance.
     * @return naming template
     */
    String toImmutable() default "toImmutable";

    /**
     * Generated builder class name.
     * @return naming template
     */
    String typeBuilder() default "Builder";

    /**
     * Inner builder class name which will be matched to be extend/super for generated builder.
     * @return naming template
     */
    String typeInnerBuilder() default "Builder";

    /**
     * Naming templates to detect base/raw type name from provided abstract value type name.
     * If none specified or if none matches, then raw type name will be taken literally the same as
     * abstract value type name.
     * <p>
     * <em>This is detection pattern, not formatting pattern. It defines how to recognize name, not how to derive name</em>
     * @return naming templates
     */
    String[] typeAbstract() default "Abstract*";

    /**
     * Name template to generate immutable implementation type by using base/raw type name.
     * Use {@link #typeAbstract()} to customize base/raw name inference.
     * @return naming template
     */
    String typeImmutable() default "Immutable*";

    /**
     * Umbrella nesting class name generated using {@link Enclosing}.
     * @return naming template
     */
    String typeImmutableEnclosing() default "Immutable*";

    /**
     * Immutable class name when generated under umbrella class using {@link Enclosing} annotation.
     * @see #typeImmutable()
     * @see #typeImmutableEnclosing()
     * @return naming template
     */
    String typeImmutableNested() default "*";

    /**
     * Modifiable companion class name template
     * @return naming template
     */
    String typeModifiable() default "Modifiable*";

    /*
     * Generated "with" interface name. Used to detect demand and generate "with" interface.
     * @return naming template
     * @deprecated not implemented / experimental. May be used or removed in the next versions
     */
    @Deprecated
    String typeWith() default "With";

    /**
     * Specify default options for the generated immutable objects.
     * If at least one attribute is specifid in inline {@literal @}{@link Immutable} annotation,
     * then this default will not be taken into account, objects will be generated using attributes
     * from inline annotation.
     * @return default configuration
     */
    Immutable defaults() default @Immutable;

    /**
     * When {@code true} &mdash; forces to generate code which use only JDK 7+ standard library
     * classes. It is {@code false} by default, however usage of JDK-only classes will be turned on
     * automatically if <em>Google Guava</em> library is not found in classpath. The generated code
     * will have subtle differences, but nevertheless will be functionally equivalent.
     * <p>
     * <em>Note that some additional annotation processors (for example mongo repository generator)
     * may not work without Guava being accessible to the generated classes,
     * and thus will not honor this attribute</em>
     * @return if forced JDK-only class usage
     */
    boolean jdkOnly() default false;

    /**
     * When {@code true} &mdash; forces to generate strict builder code. Strict builders are forward
     * only. For collections and maps, there's no set/reset methods are generated in
     * favor of using additive only initializers. For regular attributes, initializers could be
     * called only once, subsequent reinitialization with throw exception.
     * Also, "from" method (named by {@link #from()}) will not be generated on builder: it
     * becomes error-inviting to reinitialize builder values.
     * <p>
     * Usage of strict builders helps to prevent initialization mistakes early on.
     * @return if strict builder enabled
     */
    boolean strictBuilder() default false;

    /**
     * When {@code true} &mdash; all settable attributes are considered as they are annotated with
     * {@link Value.Parameter}. When explicit {@link Value.Parameter} are specified,
     * {@code allParameter} feature will be disabled. This allows to use default all parameters if
     * enabled, or override this if needed by specifying explicit parameters.
     * <p>
     * This style could be used to create special tuple-style annotations:
     *
     * <pre>
     * {@literal @}Value.Style(
     *     typeImmutable = "*Tuple",
     *     allParameters = true,
     *     defaults = {@literal @}Value.Immutable(builder = false))
     * public @interface Tuple {}
     * 
     * {@literal @}Tuple
     * {@literal @}Value.Immutable
     * interface Color {
     *   int red();
     *   int green();
     *   int blue();
     * }
     * 
     * ColorTuple.of(0xFF, 0x00, 0xFE);
     * </pre>
     * @return if all attributes will be considered parameters
     */
    boolean allParameters() default false;

    /**
     * This funny-named named attribute, when enabled makes default accessor methods defined in
     * interfaces/traits to behave as if they annotated as {@literal @}{@link Value.Default}.
     * This is not a default behaviour to preserve compatibility and also to have an choice to not
     * opt-in for this new functionality when not needed.
     * @return if consider default method accessors as {@literal @}{@code Value.Default}
     */
    boolean defaultAsDefault() default false;

    /**
     * Enable if you needed to copy header comments from an originating source file with abstract
     * types to generated (derived) implementation classes. It is off by default because not often
     * needed (as generated files are transient and not stored in version control), but adds up to
     * the processing time.
     * @return if copy header comments to generated classes.
     */
    boolean headerComments() default false;

    /**
     * List type of annotations to copy over from abstract value type to immutable implementation
     * class. Very often this functionality is not needed when annotations are declared as
     * {@link Inherited}, but there are cases where you need to pass specific non-inherited
     * annotations to the implementation class. In general, copying all type-level annotations is
     * not very safe for annotation processing and some other annotation consumers. By default, no
     * annotations are copied unless you specify non-empty annotation type list as value
     * for {@code passAnnotations} attribute. Howerver there are some special annotations which are
     * copied using special logic, such as {@code Nullable} annotations (and Jackson annotations)
     * <p>
     * This style parameter is experimental and may change in future.
     * @return types of annotations to pass to an immutable implementation class and its
     *         attributes.
     */
    Class<? extends Annotation>[] passAnnotations() default {};

    /**
     * List of additional annotations to pass through for any jackson json object
     * @Deprecated this is no longer necessary.
     * @return types of annotations to pass to the json methods on an immutable implementation class
     */
    Class<? extends Annotation>[] additionalJsonAnnotations() default {};

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. It is a good idea to not specify such attributes inline with immutable values, but
     * rather create style annotation (@see Style).
     * @return implementation visibility
     */
    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    /**
     * Specify whether init and copy methods for an unwrapped {@code X} of {@code Optional<X>}
     * should accept {@code null} values as empty value. By default nulls are rejected in favor of
     * explicit conversion using {@code Optional.ofNullable}. Please note that initializers that
     * take explicit {@code Optional} value always reject nulls regardless of this setting.
     * @return optional elements accept nullables
     */
    boolean optionalAcceptNullable() default false;

    /**
     * Generate {@code SuppressWarnings("all")} in generated code. Set this to {@code false} to
     * expose all warnings in a generated code. To suppress other warnings issued by Immutables use
     * explicit annotations {@literal @}{@code SuppressWarning("immutables")} or {@literal @}
     * {@code SuppressWarning("all")}
     * @return {@code true} if will generate suppress all warnings, enabled by default.
     */
    boolean generateSuppressAllWarnings() default true;

    /**
     * Generate a default no argument constructor in generated code. Note that this property will
     * be ignored if {@link Immutable#singleton()} returns <code>true</code>.
     * @return {@code true} if will generate a default no argument constructor, disabled by default.
     */
    boolean privateNoargConstructor() default false;

    /**
     * Enabling {@code attributelessSingleton} switches to old behavior of 2.0.x version when
     * immutables wich had no attributes defined acted as .
     * As of 2.1 we are more strict and explicit with singletons and are not generating it by
     * default, only when {@link Immutable#singleton()} is explicitly enabled.
     * @return {@code true} no attribute immutables will be auto-singletons.
     */
    boolean attributelessSingleton() default false;

    /**
     * As of 2.1 we are switching to safe generation of derived and default values when there are
     * more than one such attribute. This {@code unsafeDefaultAndDerived} style could be enabled to
     * revert to old, unsafe behavior.
     * <p>
     * In order to initialize default and derived attributes method bodies (initializers) will be
     * invoked. Intializers could refer to other attributes, some of which might be also derived or
     * uninitialized default values. As it's extremely difficult to reliably inspect initializer
     * methods bodies and compute proper ordering, we use some special generated code which figures
     * it out in runtime. If there will be a cycle in initializers, then
     * {@link IllegalStateException} will be thrown.
     * <p>
     * If you set {@code unsafeDefaultAndDerived} to {@code true}, then simpler, unsafe code will be
     * generated. With usafe code you cannot refer to other default or derived attributes in
     * initializers as otherwise result will be undefined as order of initialization is not
     * guaranteed.
     * @return {@code true} if old unsafe (but potentially with less overhead) generation should be
     *         used.
     */
    boolean unsafeDefaultAndDerived() default false;

    /**
     * When enabled: {@code clear} method will be generated on builder to reset state of a builder.
     * Primarily designed for resource constrained environments to minimize allocations. This
     * functionality is disabled by default as usually it's better to create fresh builders with a
     * clean state: in server side java it may be more efficient to allocate new builder than clean
     * previously allocated one.
     * <p>
     * <em>Note: this functionality is experimental and may be changed in further versions</em>
     * @see #clear()
     * @return {@code true} if clean method would be generated.
     */
    boolean clearBuilder() default false;

    /**
     * Deep analysis of immutable types enables additional convenience features, such as special
     * initializers for types with constuctor shortcut. It also enables to use immutable type as
     * actual implementation type (with covariant return override in accessor implementation).
     * <p>
     * Disabled by default as, speculatively, this might significantly add-up to processing time. *
     * <p>
     * <em>Note: this functionality is experimental and may be changed in further versions</em>
     * @return {@code true} if deep detection is enabled.
     */
    boolean deepImmutablesDetection() default false;

    /**
     * Makes abstract value type predominantly used in generated signatures rather than immutable
     * implementation class. In case of {@link #visibility()} is more restrictive than
     * {@link #builderVisibility()} (for example is {@code PRIVATE}), then this
     * feature is turned on automatically.
     * <p>
     * <em>Note: not all generators or generation modes might honor this attribute</em>
     * @return {@code true} if methods of generated builders and other classes should return
     *         abstract type, rather than work with immutable implementation class.
     */
    boolean overshadowImplementation() default false;

    /**
     * By default builder is generated as inner builder class nested in immutable value class.
     * Setting this to {@code true} will flip the picture — immutable implementation class will be
     * nested inside builder, which will be top level class. In case if {@link #visibility()} is set
     * to {@link ImplementationVisibility#PRIVATE} this feature is turned on automatically.
     * @return {@code true} if builder should be generated as top level class and implementation
     *         will became static inner class inside builder.
     */
    boolean implementationNestedInBuilder() default false;

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. It is a good idea to not specify such attributes inline with immutable values, but
     * rather create style annotation (@see Style).
     * @return implementation visibility
     */
    BuilderVisibility builderVisibility() default BuilderVisibility.PUBLIC;

    /**
     * Exception to throw when an immutable object is in an invalid state. I.e. when some mandatory
     * attributes are missing and immutable object cannot be built. The runtime exception class must
     * have a constructor that takes a single string, otherwise there will be compile error in the
     * generated code. The default exception type is {@link IllegalStateException}.
     * @return exception type
     */
    Class<? extends RuntimeException> throwForInvalidImmutableState() default IllegalStateException.class;

    /**
     * If implementation visibility is more restrictive than visibility of abstract value type, then
     * implementation type will not be exposed as a return type of {@code build()} or {@code of()}
     * constructon methods. Builder visibility will follow.
     */
    public enum ImplementationVisibility {
      /**
       * Generated implementation class forced to be public.
       */
      PUBLIC,

      /**
       * Visibility is the same as abstract value type
       */
      SAME,

      /**
       * Visibility is the same, but it is not returned from build and factory method, instead
       * abstract value type returned.
       * @deprecated use combination with {@link Style#overshadowImplementation()}
       */
      @Deprecated
      SAME_NON_RETURNED,

      /**
       * Implementation will have package visibility
       */
      PACKAGE,

      /**
       * Allowed only when builder is enabled or nested inside enclosing type.
       * Builder visibility will follow the umbrella class visibility.
       */
      PRIVATE
    }

    public enum BuilderVisibility {
      /**
       * Generated builder visibility is forced to be public.
       */
      PUBLIC,
      /**
       * Generated builder visibility is the same as abstract value type
       */
      SAME,
      /**
       * Generated builder visibility is forced to be package-private.
       */
      PACKAGE
    }
  }
}
