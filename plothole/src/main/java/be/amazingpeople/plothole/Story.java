package be.amazingpeople.plothole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AliasFor;

/**
 * Marks a class as a story: an ordered list of {@link Chapter chapters} that together tell one
 * end-to-end scenario. A story is a {@link SpringBootTest} (the application boots once for the whole
 * story) with a single test instance, so fields carry state from chapter to chapter.
 *
 * <p>Extend {@link PlotHoleStory} to give the class its chapters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(Story.StoryDisplayNameGenerator.class)
public @interface Story {

    /** The title of the story. */
    String value();

    /** Defaults to a real server on a random port, which is what RestAssured needs. */
    @AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
    WebEnvironment webEnvironment() default WebEnvironment.RANDOM_PORT;

    @AliasFor(annotation = SpringBootTest.class, attribute = "classes")
    Class<?>[] classes() default {};

    class StoryDisplayNameGenerator extends DisplayNameGenerator.Standard {

        @Override
        public String generateDisplayNameForClass(Class<?> testClass) {
            return AnnotationSupport.findAnnotation(testClass, Story.class)
                    .map(story -> "📖 " + story.value())
                    .orElseGet(() -> super.generateDisplayNameForClass(testClass));
        }
    }
}


