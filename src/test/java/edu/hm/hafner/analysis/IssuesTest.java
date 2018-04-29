package edu.hm.hafner.analysis;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.collections.impl.block.factory.Predicates;
import org.junit.jupiter.api.Test;

import static edu.hm.hafner.analysis.assertj.Assertions.assertThat;
import static edu.hm.hafner.analysis.assertj.SoftAssertions.*;
import edu.hm.hafner.util.SerializableTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Issues}.
 *
 * @author Marcel Binder
 * @author Ullrich Hafner
 */
class IssuesTest extends SerializableTest<Issues> {
    private static final String SERIALIZATION_NAME = "issues.ser";

    private static final Issue HIGH = new IssueBuilder().setMessage("issue-1")
            .setFileName("file-1")
            .setPriority(Priority.HIGH)
            .build();
    private static final Issue NORMAL_1 = new IssueBuilder().setMessage("issue-2")
            .setFileName("file-1")
            .setPriority(Priority.NORMAL)
            .build();
    private static final Issue NORMAL_2 = new IssueBuilder().setMessage("issue-3")
            .setFileName("file-1")
            .setPriority(Priority.NORMAL)
            .build();
    private static final Issue LOW_2_A = new IssueBuilder().setMessage("issue-4")
            .setFileName("file-2")
            .setPriority(Priority.LOW)
            .build();
    private static final Issue LOW_2_B = new IssueBuilder().setMessage("issue-5")
            .setFileName("file-2")
            .setPriority(Priority.LOW)
            .build();
    private static final Issue LOW_FILE_3 = new IssueBuilder().setMessage("issue-6")
            .setFileName("file-3")
            .setPriority(Priority.LOW)
            .build();
    private static final String EXTENDED_VALUE = "Extended";
    private static final String ID = "id";

    @Test
    void shouldGroupIssuesByProperty() {
        Issues issues = new Issues();
        issues.addAll(allIssuesAsList());

        Map<String, Issues> byPriority = issues.groupByProperty("severity");
        assertThat(byPriority).hasSize(3);
        assertThat(byPriority.get(Priority.HIGH.toString())).hasSize(1);
        assertThat(byPriority.get(Priority.NORMAL.toString())).hasSize(2);
        assertThat(byPriority.get(Priority.LOW.toString())).hasSize(3);

        Map<String, Issues> byFile = issues.groupByProperty("fileName");
        assertThat(byFile).hasSize(3);
        assertThat(byFile.get("file-1")).hasSize(3);
        assertThat(byFile.get("file-2")).hasSize(2);
        assertThat(byFile.get("file-3")).hasSize(1);
    }

    /**
     * Ensures that each method that creates a copy of another issue instance also copies the corresponding properties.
     */
    @Test
    void shouldProvideNoWritingIterator() {
        Issues issues = new Issues();
        issues.add(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);
        Iterator<Issue> iterator = issues.iterator();
        iterator.next();
        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Ensures that each method that creates a copy of another issue instance also copies the corresponding properties.
     */
    @Test
    void shouldCopyProperties() {
        Issues expected = new Issues();
        expected.add(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);
        expected.setOrigin(ID);
        expected.logInfo("Hello");
        expected.logInfo("World!");
        expected.logError("Boom!");

        Issues copy = expected.copy();
        assertThat(copy).isEqualTo(expected);
        assertThatAllIssuesHaveBeenAdded(copy);

        Issues issues = new Issues();
        issues.addAll(expected);
        assertThat(issues).isEqualTo(expected);
        assertThatAllIssuesHaveBeenAdded(issues);

        Issues empty = expected.copyEmptyInstance();
        assertThat(empty).isEmpty();
        assertThat(empty).hasOrigin(expected.getOrigin());
        assertThat(empty.getErrorMessages()).isEqualTo(expected.getErrorMessages());
        assertThat(empty.getInfoMessages()).isEqualTo(expected.getInfoMessages());
        assertThat(empty.getDuplicatesSize()).isEqualTo(expected.getDuplicatesSize());

        Issues filtered = expected.filter(Predicates.alwaysTrue());
        assertThat(filtered).isEqualTo(expected);
        assertThatAllIssuesHaveBeenAdded(filtered);
    }

    /** Verifies some additional variants of the {@link Issues#addAll(Issues, Issues[])}. */
    @Test
    void shouldVerifyPathInteriorCoverageOfAddAll() {
        Issues first = new Issues();
        first.add(HIGH);
        Issues second = new Issues();
        second.add(NORMAL_1, NORMAL_2);
        Issues third = new Issues();
        third.add(LOW_2_A, LOW_2_B, LOW_FILE_3);

        Issues issues = new Issues();
        issues.addAll(first);
        assertThat((Iterable<Issue>) issues).containsExactly(HIGH);
        issues.addAll(second, third);
        assertThatAllIssuesHaveBeenAdded(issues);

        Issues altogether = new Issues();
        altogether.addAll(first, second, third);
        assertThatAllIssuesHaveBeenAdded(issues);
    }

    /** Verifies that the ID of the first set of issues remains if other IDs are added. */
    @Test
    void shouldVerifyOriginAndReferenceOfFirstRemains() {
        Issues first = new Issues();
        first.setOrigin(ID);
        first.setReference(ID);
        first.add(HIGH);
        Issues second = new Issues();
        String otherId = "otherId";
        second.setOrigin(otherId);
        second.setReference(otherId);
        second.add(NORMAL_1, NORMAL_2);
        Issues third = new Issues();
        String idOfThird = "yetAnotherId";
        third.setOrigin(idOfThird);
        third.setReference(idOfThird);
        third.add(LOW_2_A, LOW_2_B, LOW_FILE_3);

        Issues issues = new Issues();
        issues.addAll(first);
        assertThat((Iterable<Issue>) issues).containsExactly(HIGH);
        assertThat(issues).hasOrigin(ID);
        assertThat(issues).hasReference(ID);

        issues.addAll(second, third);
        assertThatAllIssuesHaveBeenAdded(issues);
        assertThat(issues).hasOrigin(ID);
        assertThat(issues).hasReference(ID);

        Issues altogether = new Issues();
        altogether.addAll(first, second, third);
        assertThatAllIssuesHaveBeenAdded(issues);
        assertThat(issues).hasOrigin(ID);
        assertThat(issues).hasReference(ID);

        Issues copy = third.copyEmptyInstance();
        copy.addAll(first, second);
        assertThat(copy).hasOrigin(idOfThird);
        assertThat(copy).hasReference(idOfThird);
    }

    @Test
    void shouldSetAndGetOriginAndReference() {
        Issues issues = new Issues();
        assertThat(issues).hasOrigin(Issues.DEFAULT_ID);
        assertThat(issues).hasReference(Issues.DEFAULT_ID);

        issues.setOrigin(ID);
        assertThat(issues).hasOrigin(ID);
        assertThat(issues).hasReference(Issues.DEFAULT_ID);

        issues.setReference(ID);
        assertThat(issues).hasOrigin(ID);
        assertThat(issues).hasReference(ID);

        //noinspection ConstantConditions
        assertThatThrownBy(() -> issues.setOrigin(null)).isInstanceOf(NullPointerException.class);
        //noinspection ConstantConditions
        assertThatThrownBy(() -> issues.setReference(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldBeEmptyWhenCreated() {
        Issues issues = new Issues();

        assertThat(issues).isEmpty();
        assertThat(issues.isNotEmpty()).isFalse();
        assertThat(issues).hasSize(0);
        assertThat(issues.size()).isEqualTo(0);
        assertThat(issues).hasPriorities(0, 0, 0);
    }

    @Test
    void shouldAddMultipleIssuesOneByOne() {
        Issues issues = new Issues();

        issues.add(HIGH);
        issues.add(NORMAL_1);
        issues.add(NORMAL_2);
        issues.add(LOW_2_A);
        issues.add(LOW_2_B);
        issues.add(LOW_FILE_3);

        assertThatAllIssuesHaveBeenAdded(issues);
    }

    @Test
    void shouldAddMultipleIssuesAsCollection() {
        Issues issues = new Issues();
        List<Issue> issueList = allIssuesAsList();

        issues.addAll(issueList);

        assertThatAllIssuesHaveBeenAdded(issues);
    }

    @Test
    void shouldIterateOverAllElementsInCorrectOrder() {
        Issues issues = new Issues();

        issues.add(HIGH);
        issues.add(NORMAL_1, NORMAL_2);
        issues.add(LOW_2_A, LOW_2_B, LOW_FILE_3);
        Iterator<Issue> iterator = issues.iterator();
        assertThat(iterator.next()).isSameAs(HIGH);
        assertThat(iterator.next()).isSameAs(NORMAL_1);
        assertThat(iterator.next()).isSameAs(NORMAL_2);
        assertThat(iterator.next()).isSameAs(LOW_2_A);
        assertThat(iterator.next()).isSameAs(LOW_2_B);
        assertThat(iterator.next()).isSameAs(LOW_FILE_3);
    }

    @Test
    void shouldSkipAddedElements() {
        Issues issues = new Issues(allIssuesAsList());

        Issues fromEmpty = new Issues();

        fromEmpty.addAll(issues);
        assertThatAllIssuesHaveBeenAdded(fromEmpty);
        fromEmpty.addAll(issues);
        assertThat(fromEmpty).hasSize(6)
                .hasDuplicatesSize(6)
                .hasPriorities(1, 2, 3);

        Issues left = new Issues(asList(HIGH, NORMAL_1, NORMAL_2));
        Issues right = new Issues(asList(LOW_2_A, LOW_2_B, LOW_FILE_3));

        Issues everything = new Issues();
        everything.addAll(left, right);
        assertThat(everything).hasSize(6);
    }

    @Test
    void shouldAddMultipleIssuesToNonEmpty() {
        Issues issues = new Issues();
        issues.add(HIGH);

        issues.addAll(asList(NORMAL_1, NORMAL_2));
        issues.addAll(asList(LOW_2_A, LOW_2_B, LOW_FILE_3));

        assertThatAllIssuesHaveBeenAdded(issues);
    }

    private void assertThatAllIssuesHaveBeenAdded(final Issues issues) {
        assertSoftly(softly -> {
            softly.assertThat(issues)
                    .hasSize(6)
                    .hasDuplicatesSize(0)
                    .hasPriorities(1, 2, 3);
            softly.assertThat(issues.getFiles())
                    .containsExactly("file-1", "file-2", "file-3");
            softly.assertThat(issues.getFiles())
                    .containsExactly("file-1", "file-2", "file-3");
            softly.assertThat((Iterable<Issue>) issues)
                    .containsExactly(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);
            softly.assertThat(issues.isNotEmpty()).isTrue();
            softly.assertThat(issues.size()).isEqualTo(6);

            softly.assertThat(issues.getPropertyCount(Issue::getFileName)).containsEntry("file-1", 3);
            softly.assertThat(issues.getPropertyCount(Issue::getFileName)).containsEntry("file-2", 2);
            softly.assertThat(issues.getPropertyCount(Issue::getFileName)).containsEntry("file-3", 1);
        });
    }

    @Test
    void shouldSkipDuplicates() {
        Issues issues = new Issues();
        issues.add(HIGH);
        assertThat(issues).hasSize(1).hasDuplicatesSize(0);
        issues.add(HIGH);
        assertThat(issues).hasSize(1).hasDuplicatesSize(1);
        issues.addAll(asList(HIGH, LOW_2_A));
        assertThat(issues).hasSize(2).hasDuplicatesSize(2);
        issues.addAll(asList(NORMAL_1, NORMAL_2));
        assertThat(issues).hasSize(4).hasDuplicatesSize(2);

        assertThat(issues.iterator()).containsExactly(HIGH, LOW_2_A, NORMAL_1, NORMAL_2);
        assertThat(issues).hasPriorities(1, 2, 1);
        assertThat(issues.getFiles()).containsExactly("file-1", "file-2");
    }

    @Test
    void shouldRemoveById() {
        shouldRemoveOneIssue(HIGH, NORMAL_1, NORMAL_2);
        shouldRemoveOneIssue(NORMAL_1, HIGH, NORMAL_2);
        shouldRemoveOneIssue(NORMAL_1, NORMAL_2, HIGH);
    }

    private void shouldRemoveOneIssue(final Issue... initialElements) {
        Issues issues = new Issues();
        issues.addAll(asList(initialElements));

        assertThat(issues.remove(HIGH.getId())).isEqualTo(HIGH);

        assertThat((Iterable<Issue>) issues).containsExactly(NORMAL_1, NORMAL_2);
    }

    @Test
    void shouldThrowExceptionWhenRemovingWithWrongKey() {
        Issues issues = new Issues();

        UUID id = HIGH.getId();
        assertThatThrownBy(() -> issues.remove(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void shouldFindIfOnlyOneIssue() {
        Issues issues = new Issues();
        issues.addAll(Collections.singletonList(HIGH));

        Issue found = issues.findById(HIGH.getId());

        assertThat(found).isSameAs(HIGH);
    }

    @Test
    void shouldFindWithinMultipleIssues() {
        shouldFindIssue(HIGH, NORMAL_1, NORMAL_2);
        shouldFindIssue(NORMAL_1, HIGH, NORMAL_2);
        shouldFindIssue(NORMAL_1, NORMAL_2, HIGH);
    }

    private void shouldFindIssue(final Issue... elements) {
        Issues issues = new Issues();
        issues.addAll(asList(elements));

        Issue found = issues.findById(HIGH.getId());

        assertThat(found).isSameAs(HIGH);
    }

    @Test
    void shouldThrowExceptionWhenSearchingWithWrongKey() {
        shouldFindNothing(HIGH);
        shouldFindNothing(HIGH, NORMAL_1);
    }

    private void shouldFindNothing(final Issue... elements) {
        Issues issues = new Issues();
        issues.addAll(asList(elements));

        UUID id = NORMAL_2.getId();
        assertThatThrownBy(() -> issues.findById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void shouldReturnEmptyListIfPropertyDoesNotMatch() {
        Issues issues = new Issues();
        issues.addAll(asList(HIGH, NORMAL_1, NORMAL_2));

        Set<Issue> found = issues.findByProperty(issue -> Objects.equals(issue.getSeverity(), Priority.LOW));

        assertThat(found).isEmpty();
    }

    @Test
    void testFindByPropertyResultImmutable() {
        Issues issues = new Issues();
        issues.addAll(asList(HIGH, NORMAL_1, NORMAL_2));
        Set<Issue> found = issues.findByProperty(issue -> Objects.equals(issue.getSeverity(), Severity.WARNING_HIGH));

        assertThat(found).hasSize(1);
        assertThat(found).containsExactly(HIGH);
    }

    @Test
    @SuppressFBWarnings
    void shouldReturnIndexedValue() {
        Issues issues = new Issues();
        issues.addAll(asList(HIGH, NORMAL_1, NORMAL_2));

        assertThat(issues.get(0)).isSameAs(HIGH);
        assertThat(issues.get(1)).isSameAs(NORMAL_1);
        assertThat(issues.get(2)).isSameAs(NORMAL_2);
        assertThatThrownBy(() -> issues.get(-1))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("-1");
        assertThatThrownBy(() -> issues.get(3))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("3");
    }

    @Test
    void shouldReturnFiles() {
        Issues issues = new Issues();
        issues.addAll(allIssuesAsList());

        assertThat(issues.getFiles()).contains("file-1", "file-1", "file-3");
    }

    private List<Issue> allIssuesAsList() {
        return asList(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);
    }

    @Test
    void shouldReturnSizeInToString() {
        Issues issues = new Issues();
        issues.addAll(asList(HIGH, NORMAL_1, NORMAL_2));

        assertThat(issues.toString()).contains("3");
    }

    @Test
    void shouldReturnProperties() {
        Issues issues = new Issues();
        issues.addAll(allIssuesAsList());

        Set<String> properties = issues.getProperties(Issue::getMessage);

        assertThat(properties)
                .contains(HIGH.getMessage())
                .contains(NORMAL_1.getMessage())
                .contains(NORMAL_2.getMessage());
    }

    @Test
    void testCopy() {
        Issues original = new Issues();
        original.addAll(asList(HIGH, NORMAL_1, NORMAL_2));

        Issues copy = original.copy();

        assertThat(copy).isNotSameAs(original);
        assertThat(copy.iterator()).containsExactly(HIGH, NORMAL_1, NORMAL_2);

        copy.add(LOW_2_A);
        assertThat(original.iterator()).containsExactly(HIGH, NORMAL_1, NORMAL_2);
        assertThat(copy.iterator()).containsExactly(HIGH, NORMAL_1, NORMAL_2, LOW_2_A);
    }

    @Test
    void shouldFilterByProperty() {
        assertFilterFor(IssueBuilder::setPackageName, Issues::getPackages, "packageName");
        assertFilterFor(IssueBuilder::setModuleName, Issues::getModules, "moduleName");
        assertFilterFor(IssueBuilder::setOrigin, Issues::getTools, "toolName");
        assertFilterFor(IssueBuilder::setCategory, Issues::getCategories, "category");
        assertFilterFor(IssueBuilder::setType, Issues::getTypes, "type");
    }

    private void assertFilterFor(final BiFunction<IssueBuilder, String, IssueBuilder> builderSetter,
            final Function<Issues, Set<String>> propertyGetter, final String propertyName) {
        Issues issues = new Issues();

        IssueBuilder builder = new IssueBuilder();

        for (int i = 1; i < 4; i++) {
            for (int j = i; j < 4; j++) {
                Issue build = builderSetter.apply(builder, "name " + i).setMessage(i + " " + j).build();
                issues.add(build);
            }
        }
        assertThat(issues).hasSize(6);

        Set<String> properties = propertyGetter.apply(issues);

        assertThat(properties).as("Wrong values for property " + propertyName)
                .containsExactlyInAnyOrder("name 1", "name 2", "name 3");
    }

    @Test
    void shouldStoreAndRetrieveLogAndErrorMessagesInCorrectOrder() {
        Issues issues = new Issues();

        assertThat(issues.getInfoMessages()).hasSize(0);
        assertThat(issues.getErrorMessages()).hasSize(0);

        issues.logInfo("%d: %s %s", 1, "Hello", "World");
        issues.logInfo("%d: %s %s", 2, "Hello", "World");

        assertThat(issues.getInfoMessages()).hasSize(2);
        assertThat(issues.getInfoMessages()).containsExactly("1: Hello World", "2: Hello World");

        issues.logError("%d: %s %s", 1, "Hello", "World");
        issues.logError("%d: %s %s", 2, "Hello", "World");

        assertThat(issues.getInfoMessages()).hasSize(2);
        assertThat(issues.getInfoMessages()).containsExactly("1: Hello World", "2: Hello World");
        assertThat(issues.getErrorMessages()).hasSize(2);
        assertThat(issues.getErrorMessages()).containsExactly("1: Hello World", "2: Hello World");
    }

    @Override
    protected Issues createSerializable() {
        Issues issues = new Issues();
        issues.add(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);
        return issues;
    }

    /**
     * Verifies that saved serialized format (from a previous release) still can be resolved with the current
     * implementation of {@link Issue}.
     */
    @Test
    void shouldReadIssueFromOldSerialization() {
        byte[] restored = readAllBytes(SERIALIZATION_NAME);

        assertThatSerializableCanBeRestoredFrom(restored);
    }

    /** Verifies that equals checks all properties. */
    @Test
    void shouldBeNotEqualsAPropertyChanges() {
        Issues issues = new Issues();
        issues.add(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);

        Issues other = new Issues();
        other.addAll(issues);
        other.add(HIGH, NORMAL_1, NORMAL_2, LOW_2_A, LOW_2_B, LOW_FILE_3);

        assertThat(issues).isNotEqualTo(other); // there should be duplicates
        assertThat(issues).hasDuplicatesSize(0);
        assertThat(other).hasDuplicatesSize(6);
    }

    /**
     * Serializes an issues to a file. Use this method in case the issue properties have been changed and the
     * readResolve method has been adapted accordingly so that the old serialization still can be read.
     *
     * @param args
     *         not used
     *
     * @throws IOException
     *         if the file could not be written
     */
    public static void main(final String... args) throws IOException {
        new IssuesTest().createSerializationFile();
    }
}