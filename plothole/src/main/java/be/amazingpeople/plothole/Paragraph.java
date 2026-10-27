package be.amazingpeople.plothole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A paragraph of a {@link Chapter}: one test inside a chapter that is an inner class.
 * Paragraphs run in ascending {@link #value() number}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Paragraph {

    /** The paragraph number; unique within the chapter. */
    int value();

    /** What happens in this paragraph. */
    String title();
}
