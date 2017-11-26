package edu.hm.hafner.analysis.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Issues;
import edu.hm.hafner.analysis.Priority;
import static edu.hm.hafner.analysis.assertj.Assertions.*;
import static edu.hm.hafner.analysis.assertj.SoftAssertions.*;

/**
 * Tests the class {@link CppLintParser}.
 *
 * @author Ullrich Hafner
 */
public class CppLintParserTest extends ParserTester {
    @Override
    protected String getWarningsFile() {
        return "cpplint.txt";
    }

    /** Parses a file with 1031 warnings. */
    @Test
    public void shouldFindAll1031Warnings() {
        Issues<Issue> issues = new CppLintParser().parse(openFile());

        assertThat(issues).hasSize(1031)
                .hasHighPrioritySize(81)
                .hasNormalPrioritySize(201)
                .hasLowPrioritySize(749);

        assertSoftly(softly -> {
            softly.assertThat(issues.get(0))
                    .hasLineStart(824)
                    .hasLineEnd(824)
                    .hasMessage("Tab found; better to use spaces")
                    .hasFileName("c:/Workspace/Trunk/Project/P1/class.cpp")
                    .hasCategory("whitespace/tab")
                    .hasPriority(Priority.LOW);
        });
    }

    /**
     * Parses a file with CPP Lint warnings in the new format.
     *
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-18290">Issue 18290</a>
     */
    @Test
    public void issue18290() {
        Issues<Issue> warnings = new CppLintParser().parse(openFile("issue18290.txt"));

        assertThat(warnings).hasSize(2);

        assertSoftly(softly -> {
            softly.assertThat(warnings.get(0)).hasLineStart(399)
                    .hasLineEnd(399)
                    .hasMessage("Missing space before {")
                    .hasFileName("/opt/ros/fuerte/stacks/Mule/Mapping/Local_map/src/LocalCostMap.cpp")
                    .hasCategory("whitespace/braces")
                    .hasPriority(Priority.HIGH);
            softly.assertThat(warnings.get(1)).hasLineStart(400)
                    .hasLineEnd(400)
                    .hasMessage("Tab found; better to use spaces")
                    .hasFileName("/opt/ros/fuerte/stacks/Mule/Mapping/Local_map/src/LocalCostMap.cpp")
                    .hasCategory("whitespace/tab")
                    .hasPriority(Priority.LOW);
        });
    }
}
