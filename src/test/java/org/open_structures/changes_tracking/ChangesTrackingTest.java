package org.open_structures.changes_tracking;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open_structures.changes_tracking.Action.*;

public class ChangesTrackingTest {

    private ChangesTracking<Integer> changesTracking;

    @Before
    public void setUp() {
        changesTracking = new ChangesTracking<>();
    }

    @Test
    public void shouldResetTracking() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(2);
        changesTracking.resetTracking();

        // then
        assertThat(changesTracking.getTrackedElements()).isEmpty();
        assertThat(changesTracking.determineAction(1)).isNull();
        assertThat(changesTracking.determineAction(2)).isNull();
    }

    /**
     * It should not record more than one delete in a row. Deleted can't be deleted again. The second delete doesn't change anything.
     */
    @Test
    public void shouldNotRecordDoubleDeletes() {
        // when
        changesTracking.deleted(1);
        changesTracking.deleted(1);

        // then
        assertThat(changesTracking.determineAction(1)).isEqualTo(DELETE);
    }

    /**
     * It should not record more than one addition in a row. What was added can't be added again. The second addition doesn't change anything.
     */
    @Test
    public void shouldNotRecordDoubleAdditions() {
        // when
        changesTracking.added(1);
        changesTracking.added(1);

        // then
        assertThat(changesTracking.determineAction(1)).isEqualTo(ADD);
    }

    /**
     * It should treat deleted added sequence as setJobDetails
     */
    @Test
    public void shouldUpdate() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);

        // when
        assertThat(changesTracking.determineAction(1)).isEqualTo(UPDATE);
    }

    @Test
    public void shouldIgnoreInsertDelete() {
        // when
        changesTracking.added(1);
        changesTracking.deleted(1);

        // then
        assertThat(changesTracking.determineAction(1)).isNull();
        assertThat(changesTracking.getTrackedElements()).hasSize(1).contains(1);
    }

    @Test
    public void shouldConsolidateInsertDeleteInsert() {
        // when
        changesTracking.added(1);
        changesTracking.deleted(1);
        changesTracking.added(1);

        // then
        assertThat(changesTracking.determineAction(1)).isEqualTo(ADD);
    }

    @Test
    public void shouldConsolidateDeleteInsertDelete() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);
        changesTracking.deleted(1);

        // then
        assertThat(changesTracking.determineAction(1)).isEqualTo(DELETE);
    }

    @Test
    public void shouldReturnTrackedElements() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);
        changesTracking.added(2);
        changesTracking.deleted(2);
        changesTracking.added(3);
        changesTracking.deleted(4);

        // then
        assertThat(changesTracking.getTrackedElements()).hasSize(4).contains(1, 2, 3, 4);
    }

    @Test
    public void shouldReturnAllAdded() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);
        changesTracking.added(2);
        changesTracking.deleted(2);
        changesTracking.added(3);
        changesTracking.added(4);

        // then
        assertThat(changesTracking.getAdded()).hasSize(2).contains(3, 4);
    }

    @Test
    public void shouldReturnAllDeleted() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);
        changesTracking.deleted(2);
        changesTracking.added(3);
        changesTracking.deleted(4);

        // then
        assertThat(changesTracking.getDeleted()).hasSize(2).contains(2, 4);
    }

    @Test
    public void shouldReturnAllUpdated() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(1);
        changesTracking.deleted(2);
        changesTracking.added(3);
        changesTracking.deleted(4);
        changesTracking.added(4);

        // then
        assertThat(changesTracking.getUpdated()).hasSize(2).contains(1, 4);
    }

    @Test
    public void shouldDetermineIfElementIsAdded() {
        // when
        changesTracking.added(1);
        changesTracking.deleted(2);
        changesTracking.added(2);

        // then
        assertThat(changesTracking.isAdded(1)).isTrue();
        assertThat(changesTracking.isAdded(2)).isFalse();
    }

    @Test
    public void shouldDetermineIfElementIsDeleted() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(2);
        changesTracking.deleted(2);
        changesTracking.deleted(3);

        // then
        assertThat(changesTracking.isDeleted(1)).isTrue();
        assertThat(changesTracking.isDeleted(2)).isFalse();
        assertThat(changesTracking.isDeleted(3)).isTrue();
    }

    @Test
    public void shouldGetAndRestoreState() {
        // when
        changesTracking.deleted(1);
        changesTracking.added(2);
        ChangesTracking.State<Integer> changesTrackingState = changesTracking.getState();
        changesTracking.added(3);
        changesTracking.deleted(4);
        changesTracking.deleted(2);
        changesTracking.restore(changesTrackingState);

        // then
        assertThat(changesTracking.getTrackedElements()).hasSize(2).contains(1, 2);
        assertThat(changesTracking.getDeleted()).hasSize(1).contains(1);
        assertThat(changesTracking.getAdded()).hasSize(1).contains(2);
        assertThat(changesTracking.getUpdated()).isEmpty();
    }
}
