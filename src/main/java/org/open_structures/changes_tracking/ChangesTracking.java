package org.open_structures.changes_tracking;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.open_structures.memento.Memento;
import org.open_structures.memento.Restorable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.requireNonNull;
import static org.open_structures.changes_tracking.Action.*;

public class ChangesTracking<T> implements Restorable<ChangesTracking.State<T>> {

    public enum ChangeOperation {
        INSERT, DELETE
    }

    public static class State<T> implements Memento {
        private final Map<T, List<ChangeOperation>> changes;

        public State(Map<T, List<ChangeOperation>> changes) {
            this.changes = requireNonNull(changes);
        }
    }

    private final Map<T, List<ChangeOperation>> changes = newHashMap();

    public Set<T> getAdded() {
        return getTrackedElementsByAction(ADD);
    }

    public Set<T> getDeleted() {
        return getTrackedElementsByAction(DELETE);
    }

    public Set<T> getUpdated() {
        return getTrackedElementsByAction(UPDATE);
    }

    public boolean isAdded(T element) {
        return ADD == determineAction(element);
    }

    public boolean isDeleted(T element) {
        return DELETE == determineAction(element);
    }

    private boolean isUpdated(T element) {
        return UPDATE == determineAction(element);
    }

    @Override
    public State<T> getState() {
        ImmutableMap.Builder<T, List<ChangeOperation>> builder = ImmutableMap.builder();
        changes.forEach((k, v) -> builder.put(k, ImmutableList.copyOf(v)));
        return new State<>(builder.build());
    }

    @Override
    public void restore(State<T> state) {
        checkNotNull(state);

        changes.clear();
        changes.putAll(state.changes);
    }


    public Set<T> getTrackedElements() {
        return changes.keySet();
    }

    public void added(T key) {
        checkNotNull(key);
        checkState(!isUpdated(key));

        if (!changes.containsKey(key)) {
            changes.put(key, new LinkedList<>());
        }
        if (!isLastChangeInsert(key)) {
            changes.get(key).add(ChangeOperation.INSERT);
        }

    }

    public void deleted(T key) {
        checkNotNull(key);

        if (!changes.containsKey(key)) {
            changes.put(key, new LinkedList<>());
        }
        if (!isLastChangeDelete(key)) {
            changes.get(key).add(ChangeOperation.DELETE);
        }
    }

    public void resetTracking() {
        changes.clear();
    }

    private Set<T> getTrackedElementsByAction(Action action) {
        Set<T> result = newHashSet();
        for (T element : getTrackedElements()) {
            Action elementAction = determineAction(element);
            if (action == elementAction) {
                result.add(element);
            }
        }

        return result;
    }

    private boolean isLastChangeDelete(T element) {
        return isLastChange(element, ChangeOperation.DELETE);
    }

    private boolean isLastChangeInsert(T element) {
        return isLastChange(element, ChangeOperation.INSERT);
    }

    private boolean isLastChange(T element, ChangeOperation changeOperation) {
        return changes.containsKey(element) && !changes.get(element).isEmpty() && getLast(changes.get(element)) == changeOperation;
    }

    private ChangeOperation getLast(List<ChangeOperation> changeOperations) {
        return changeOperations.get(changeOperations.size() - 1);
    }

    public Action determineAction(T element) {
        checkNotNull(element);
        if (changes.containsKey(element) && !changes.get(element).isEmpty()) {
            return determineAction(changes.get(element));
        } else {
            return null;
        }
    }

    private Action determineAction(List<ChangeOperation> changeOperations) {
        ObjectStateContext objectStateContext = new ObjectStateContext();
        for (ChangeOperation operation : changeOperations) {
            objectStateContext.transition(operation);
        }
        return objectStateContext.getCurrentAction();
    }

    private interface ObjectState {
        Action getAction();

        void transition(ChangeOperation operation);
    }

    private static class ObjectStateContext {
        private ObjectState currentState = new InitialState(this);

        public Action getCurrentAction() {
            return currentState.getAction();
        }

        public void transition(ChangeOperation operation) {
            currentState.transition(operation);
        }
    }

    private record CreatedState(ObjectStateContext objectStateContext) implements ObjectState {
            private CreatedState(ObjectStateContext objectStateContext) {
                this.objectStateContext = requireNonNull(objectStateContext);
            }

            @Override
            public Action getAction() {
                return ADD;
            }

            @Override
            public void transition(ChangeOperation operation) {
                switch (operation) {
                    case DELETE -> objectStateContext.currentState = new NonExistingState(objectStateContext);
                    case INSERT ->
                            throw new IllegalArgumentException("Can't transition from Add action to another Add action. Adding the same thing twice in a row makes no sense");
                    default -> throw new UnsupportedOperationException("operation " + operation + " is not supported");
                }
            }
        }

    private record DeletedState(ObjectStateContext objectStateContext) implements ObjectState {
            private DeletedState(ObjectStateContext objectStateContext) {
                this.objectStateContext = requireNonNull(objectStateContext);
            }

            @Override
            public Action getAction() {
                return DELETE;
            }

            @Override
            public void transition(ChangeOperation operation) {
                switch (operation) {
                    case INSERT -> objectStateContext.currentState = new UpdatedState(objectStateContext);
                    case DELETE -> throw new IllegalStateException("Delete operation can't follow Delete action");
                    default -> throw new UnsupportedOperationException("operation " + operation + " is not supported");
                }
            }
        }

    private record UpdatedState(ObjectStateContext objectStateContext) implements ObjectState {
            private UpdatedState(ObjectStateContext objectStateContext) {
                this.objectStateContext = requireNonNull(objectStateContext);
            }

            @Override
            public Action getAction() {
                return UPDATE;
            }

            @Override
            public void transition(ChangeOperation operation) {
                switch (operation) {
                    case INSERT -> throw new IllegalStateException("Insert operation can't follow Update action");
                    case DELETE ->
                            objectStateContext.currentState = new DeletedState(objectStateContext); // ... deleted -> added -> deleted
                    default -> throw new UnsupportedOperationException("operation " + operation + " is not supported");
                }
            }
        }

    private record NonExistingState(ObjectStateContext objectStateContext) implements ObjectState {
            private NonExistingState(ObjectStateContext objectStateContext) {
                this.objectStateContext = requireNonNull(objectStateContext);
            }

            @Override
            public Action getAction() {
                return null;
            }

            @Override
            public void transition(ChangeOperation operation) {
                switch (operation) {
                    case INSERT -> objectStateContext.currentState = new CreatedState(objectStateContext);
                    case DELETE ->
                            throw new IllegalStateException("Delete operation can't be applied to non existing state");
                    default -> throw new UnsupportedOperationException("operation " + operation + " is not supported");
                }
            }
        }

    private record InitialState(ObjectStateContext objectStateContext) implements ObjectState {
            private InitialState(ObjectStateContext objectStateContext) {
                this.objectStateContext = requireNonNull(objectStateContext);
            }

            @Override
            public Action getAction() {
                return null;
            }

            @Override
            public void transition(ChangeOperation operation) {
                switch (operation) {
                    case INSERT -> objectStateContext.currentState = new CreatedState(objectStateContext);
                    case DELETE -> objectStateContext.currentState = new DeletedState(objectStateContext);
                    default -> throw new UnsupportedOperationException("operation " + operation + " is not supported");
                }
            }
        }
}
