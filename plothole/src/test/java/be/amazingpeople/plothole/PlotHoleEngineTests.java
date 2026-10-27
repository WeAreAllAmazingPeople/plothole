package be.amazingpeople.plothole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;

/**
 * Runs small fixture stories through JUnit itself (EngineTestKit) and checks what JUnit reports.
 *
 * <p>The fixtures are nested classes: Surefire skips them, but an IDE "run all in package" would run
 * the deliberately broken one.
 */
class PlotHoleEngineTests {

    @Test
    void stepsRunInNumberOrderWhateverTheSourceOrder() {
        Events tests = run(ScrambledStory.class).testEvents();

        assertThat(startedNames(tests)).containsExactly(
                "Chapter 1 – one",
                "§ 2.1 – first",
                "§ 2.2 – second",
                "Chapter 3 – three");
        tests.assertStatistics(stats -> stats.started(4).succeeded(4));
    }

    @Test
    void aPlotHoleInterruptsTheRestOfTheStory() {
        Events tests = run(BrokenStory.class).testEvents();

        tests.assertStatistics(stats -> stats.started(5).succeeded(2).failed(1).aborted(2));
        assertThat(names(tests.failed())).containsExactly("§ 2.2 – the price is wrong");
    }

    @Test
    void duplicateNumbersAreRejected() {
        EngineExecutionResults results = run(DuplicateStory.class);

        assertThat(results.allEvents().failed().count()).isEqualTo(1);
        assertThat(results.testEvents().started().count()).isZero();
    }

    private static EngineExecutionResults run(Class<?> story) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(story)).execute();
    }

    private static List<String> startedNames(Events events) {
        return names(events.started());
    }

    private static List<String> names(Events events) {
        return events.stream().map(event -> event.getTestDescriptor().getDisplayName()).toList();
    }

    // ---- fixtures -----------------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    static class Config {
    }

    @Story(value = "Scrambled", classes = Config.class, webEnvironment = WebEnvironment.NONE)
    static class ScrambledStory extends PlotHoleStory {

        @Chapter(value = 3, title = "three")
        void three() {
        }

        @Chapter(value = 2, title = "two")
        class Two {
            @Paragraph(value = 2, title = "second")
            void second() {
            }

            @Paragraph(value = 1, title = "first")
            void first() {
            }
        }

        @Chapter(value = 1, title = "one")
        void one() {
        }
    }

    @Story(value = "Broken", classes = Config.class, webEnvironment = WebEnvironment.NONE)
    static class BrokenStory extends PlotHoleStory {

        @Chapter(value = 1, title = "fine")
        void fine() {
        }

        @Chapter(value = 2, title = "the shop")
        class Shop {
            @Paragraph(value = 1, title = "all is well")
            void ok() {
            }

            @Paragraph(value = 2, title = "the price is wrong")
            void broken() {
                throw new AssertionError("boom");
            }

            @Paragraph(value = 3, title = "never told")
            void neverReached() {
                throw new AssertionError("must not run");
            }
        }

        @Chapter(value = 3, title = "the ending")
        void ending() {
            throw new AssertionError("must not run");
        }
    }

    @Story(value = "Duplicate", classes = Config.class, webEnvironment = WebEnvironment.NONE)
    static class DuplicateStory extends PlotHoleStory {

        @Chapter(value = 1, title = "once")
        void once() {
        }

        @Chapter(value = 1, title = "twice")
        void twice() {
        }
    }
}
