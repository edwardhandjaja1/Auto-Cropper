package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A min priority queue of distinct elements of type `KeyType` associated with (extrinsic) integer
 * priorities, implemented using a binary heap paired with a hash table.
 */
public class HeapMinQueue<KeyType> implements MinQueue<KeyType> {

    /**
     * Pairs an element `key` with its associated priority `priority`.
     */
    private record Entry<KeyType>(KeyType key, int priority) {
        // Note: This is equivalent to declaring a static nested class with fields `key` and
        //  `priority` and a corresponding constructor and observers, overriding `equals()` and
        //  `hashCode()` to depend on the fields, and overriding `toString()` to print their values.
        // https://docs.oracle.com/en/java/javase/17/language/records.html
    }

    /**
     * Associates each element in the queue with its index in `heap`.  Satisfies
     * `heap.get(index.get(e)).key().equals(e)` if `e` is an element in the queue. Only maps
     * elements that are in the queue (`index.size() == heap.size()`).
     */
    private final Map<KeyType, Integer> index;

    /**
     * Sequence representing a min-heap of element-priority pairs.  Satisfies
     * `heap.get(i).priority() >= heap.get((i-1)/2).priority()` for all `i` in `[1..heap.size()]`.
     */
    private final ArrayList<Entry<KeyType>> heap;

    /**
     * Assert that our class invariant is satisfied.  Returns true if it is (or if assertions are
     * disabled).
     */
    private boolean checkInvariant() {
        for (int i = 1; i < heap.size(); ++i) {
            int p = (i - 1) / 2;
            assert heap.get(i).priority() >= heap.get(p).priority();
            assert index.get(heap.get(i).key()) == i;
        }
        assert index.size() == heap.size();
        return true;
    }

    /**
     * Create an empty queue.
     */
    public HeapMinQueue() {
        index = new HashMap<>();
        heap = new ArrayList<>();
        assert checkInvariant();
    }

    /**
     * Return whether this queue contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Return the number of elements contained in this queue.
     */
    @Override
    public int size() {
        return heap.size();
    }

    /**
     * Return an element associated with the smallest priority in this queue.  This is the same
     * element that would be removed by a call to `remove()` (assuming no mutations in between).
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType get() {
        // Propagate exception from `List::getFirst()` if empty.
        return heap.getFirst().key();
    }

    /**
     * Return the minimum priority associated with an element in this queue.  Throws
     * NoSuchElementException if this queue is empty.
     */
    @Override
    public int minPriority() {
        return heap.getFirst().priority();
    }

    /**
     * If `key` is already contained in this queue, change its associated priority to `priority`.
     * Otherwise, add it to this queue with that priority.
     */
    @Override
    public void addOrUpdate(KeyType key, int priority) {
        if (!index.containsKey(key)) {
            add(key, priority);
        } else {
            update(key, priority);
        }
    }

    /**
     * Remove and return the element associated with the smallest priority in this queue.  If
     * multiple elements are tied for the smallest priority, an arbitrary one will be removed.
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType remove() {
        // TODO A6.3c: Implement this method as specified
        if (heap.isEmpty()) {
            throw new NoSuchElementException("Heap is empty.");
        }
        KeyType minElement = heap.get(0).key;
        // Move the last element to the root and remove the last element
        Entry<KeyType> lastElement = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, lastElement);
            index.put(lastElement.key, 0);
            bubbleDown(0);  // Adjust the position to maintain heap property
        }
        index.remove(minElement);  // Remove the minimum element from the index map
        return minElement;
    }

    /**
     * Remove all elements from this queue (making it empty).
     */
    @Override
    public void clear() {
        index.clear();
        heap.clear();
        assert checkInvariant();
    }

    /**
     * Swap the Entries at indices `i` and `j` in `heap`, updating `index` accordingly.  Requires `0
     * <= i,j < heap.size()`.
     */
    private void swap(int i, int j) {
        assert i >= 0 && i < heap.size();
        assert j >= 0 && j < heap.size();

        // TODO A6.3a: Implement this method as specified
        // Swap the elements in the heap
        Entry<KeyType> temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);

        // Update the hash map to reflect the new indices
        index.put(heap.get(i).key, i);
        index.put(heap.get(j).key, j);
    }

    /**
     * Add element `key` to this queue, associated with priority `priority`.  Requires `key` is not
     * contained in this queue.
     */
    private void add(KeyType key, int priority) {
        assert !index.containsKey(key);

        Entry<KeyType> newEntry = new Entry<>(key, priority);
        heap.add(newEntry);  // Add at the end of the heap
        index.put(key, heap.size() - 1);  // Store the index of the new entry
        bubbleUp(heap.size() - 1);  // Adjust the position to maintain heap property

        assert checkInvariant();
    }

    /**
     * Change the priority associated with element `key` to `priority`.  Requires that `key` is
     * contained in this queue.
     */
    private void update(KeyType key, int priority) {
        assert index.containsKey(key);

        // TODO A6.3e: Implement this method as specified
        if (!index.containsKey(key)) {
            throw new NoSuchElementException("Element not found in the heap.");
        }
        int idx = index.get(key);
        int currentPriority = heap.get(idx).priority;
        heap.set(idx, new Entry<>(key, priority)); // Update the priority

        // Adjust the heap depending on the priority change
        if (priority < currentPriority) {
            bubbleUp(idx);
        } else if (priority > currentPriority) {
            bubbleDown(idx);
        }

        assert checkInvariant();
    }

    // TODO A6.3b: Implement private helper methods for bubbling entries up and down in the heap.
    //  Their interfaces are up to you, but you must write precise specifications.\
    private void bubbleUp(int index) {
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            if (heap.get(index).priority < heap.get(parentIndex).priority) {
                swap(index, parentIndex);
                index = parentIndex;
            } else {
                break;
            }
        }
    }

    private void bubbleDown(int index) {
        int childIndex = 2 * index + 1; // Left child index
        while (childIndex < heap.size()) {
            // Find the smallest child
            int rightChildIndex = childIndex + 1;
            if (rightChildIndex < heap.size() && heap.get(rightChildIndex).priority < heap.get(childIndex).priority) {
                childIndex = rightChildIndex;
            }

            // If the smaller child is less than the parent, swap them
            if (heap.get(childIndex).priority < heap.get(index).priority) {
                swap(index, childIndex);
                index = childIndex;
                childIndex = 2 * index + 1;
            } else {
                break;
            }
        }
    }
}
