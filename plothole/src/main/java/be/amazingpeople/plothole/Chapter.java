package be.amazingpeople.plothole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A chapter of a {@link Story}.
 *
 * <ul>
 *   <li>On a <b>method</b>: the chapter is a single test.</li>
 *   <li>On a <b>non-static inner class</b>: the chapter is a container of
 *       {@link Paragraph}-annotated methods, which share the chapter's fields
 *       and can read and write the story's fields.</li>
 * </ul>
 *
 * Chapters run in ascending {@link #value() number}, whatever their order in the source file.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Chapter {

    /** The chapter number; unique within the story. */
    int value();

    /** What happens in this chapter. */
    String title();
}
