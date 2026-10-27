package be.amazingpeople.plothole;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.TestAbortedException;

/**
 * Base class of every {@link Story}. It reads the {@link Chapter}s and {@link Paragraph}s of the
 * concrete class and turns them into a JUnit dynamic test tree, ordered by number.
 *
 * <p>The first failing step is the <em>plot hole</em>. Every step after it is reported as aborted
 * ("story interrupted") instead of failing in a noisy cascade.
 */
public abstract class PlotHoleStory {

    private final AtomicReference<String> plotHole = new AtomicReference<>();

    @TestFactory
    @DisplayName("Table of contents")
    public final Stream<DynamicNode> tableOfContents() {
        Map<Integer, DynamicNode> chapters = new TreeMap<>();
        Class<?> story = getClass();

        // Chapters that are a single test: annotated methods.
        for (Method method : story.getDeclaredMethods()) {
            Chapter chapter = method.getAnnotation(Chapter.class);
            if (chapter == null) {
                continue;
            }
            requireNoParameters(method);
            String label = chapterLabel(chapter);
            register(chapters, chapter.value(), label,
                    DynamicTest.dynamicTest(label, () -> step(label, method, this)));
        }

        // Chapters made of paragraphs: annotated inner classes.
        for (Class<?> inner : story.getDeclaredClasses()) {
            Chapter chapter = inner.getAnnotation(Chapter.class);
            if (chapter == null) {
                continue;
            }
            requireInnerClass(inner);
            String label = chapterLabel(chapter);

            // Created when its first paragraph runs, so field initialisers can see earlier chapters.
            Lazy<Object> chapterInstance = new Lazy<>(() -> instantiate(inner));

            Map<Integer, DynamicNode> paragraphs = new TreeMap<>();
            for (Method method : inner.getDeclaredMethods()) {
                Paragraph paragraph = method.getAnnotation(Paragraph.class);
                if (paragraph == null) {
                    continue;
                }
                requireNoParameters(method);
                String paragraphLabel = "§ %d.%d – %s"
                        .formatted(chapter.value(), paragraph.value(), paragraph.title());
                register(paragraphs, paragraph.value(), paragraphLabel,
                        DynamicTest.dynamicTest(paragraphLabel,
                                () -> step(paragraphLabel, method, chapterInstance.get())));
            }
            if (paragraphs.isEmpty()) {
                throw new IllegalStateException(
                        label + " has no @Paragraph methods; make it a method if it is a single test");
            }
            register(chapters, chapter.value(), label,
                    DynamicContainer.dynamicContainer(label, paragraphs.values()));
        }

        if (chapters.isEmpty()) {
            throw new IllegalStateException(story.getName() + " has no @Chapter: a story needs a plot");
        }
        return chapters.values().stream();
    }

    private void step(String label, Method method, Object target) throws Throwable {
        String hole = plotHole.get();
        if (hole != null) {
            throw new TestAbortedException("Story interrupted by plot hole in " + hole);
        }
        try {
            method.setAccessible(true);
            method.invoke(target);
        } catch (InvocationTargetException e) {
            plotHole.compareAndSet(null, label);
            throw e.getCause();
        } catch (ReflectiveOperationException | RuntimeException e) {
            plotHole.compareAndSet(null, label);
            throw e;
        }
    }

    private Object instantiate(Class<?> chapterClass) {
        try {
            Constructor<?> constructor = chapterClass.getDeclaredConstructor(chapterClass.getDeclaringClass());
            constructor.setAccessible(true);
            return constructor.newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot open chapter " + chapterClass.getName(), e);
        }
    }

    private static String chapterLabel(Chapter chapter) {
        return "Chapter %d – %s".formatted(chapter.value(), chapter.title());
    }

    private static void register(Map<Integer, DynamicNode> into, int number, String label, DynamicNode node) {
        if (into.putIfAbsent(number, node) != null) {
            throw new IllegalStateException(
                    "Duplicate number " + number + " at \"" + label + "\" (a plot hole in the outline)");
        }
    }

    private static void requireNoParameters(Method method) {
        if (method.getParameterCount() != 0) {
            throw new IllegalStateException(method + " must not take parameters");
        }
    }

    private static void requireInnerClass(Class<?> chapterClass) {
        if (Modifier.isStatic(chapterClass.getModifiers())) {
            throw new IllegalStateException("Chapter class " + chapterClass.getName()
                    + " must be a non-static inner class, so it can see the state of the story");
        }
    }

    private static final class Lazy<T> {
        private final Supplier<T> supplier;
        private T value;

        Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        synchronized T get() {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
