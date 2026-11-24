package system.com.lostfound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BST {

    public class Node {

        private HistoryEvent event;
        private Node left;
        private Node right;

        Node(HistoryEvent event) {
            this.event = event;
        }

        public HistoryEvent getEvent() {
            return event;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }
    }

    private Node root;

    public Node getRoot() {
        return root;
    }

    public void insert(HistoryEvent event) {
        root = insertNode(root, event);
    }

    private Node insertNode(Node current, HistoryEvent event) {

        if (current == null) {
            return new Node(event);
        }

        if (event.getYear() < current.event.getYear()) {
            current.left = insertNode(current.left, event);
        } else if (event.getYear() > current.event.getYear()) {
            current.right = insertNode(current.right, event);
        }

        return current;
    }

    // Rebuild the BST in a balanced arrangement.
    public void buildBalanced(List<HistoryEvent> eventList) {

        List<HistoryEvent> sorted = new ArrayList<>(eventList);

        sorted.sort(Comparator.comparingInt(HistoryEvent::getYear));

        root = buildBalancedNode(sorted, 0, sorted.size() - 1);
    }

    private Node buildBalancedNode(
            List<HistoryEvent> events,
            int start,
            int end) {

        if (start > end) {
            return null;
        }

        int middle = start + (end - start) / 2;

        Node node = new Node(events.get(middle));

        node.left = buildBalancedNode(
            events,
            start,
            middle - 1
        );

        node.right = buildBalancedNode(
            events,
            middle + 1,
            end
        );

        return node;
    }

    public HistoryEvent search(int year) {

        Node current = root;

        while (current != null) {

            if (year == current.event.getYear()) {
                return current.event;
            }

            if (year < current.event.getYear()) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return null;
    }

    public List<HistoryEvent> getInOrder() {

        List<HistoryEvent> events = new ArrayList<>();

        inOrder(root, events);

        return events;
    }

    private void inOrder(Node current, List<HistoryEvent> events) {

        if (current != null) {

            inOrder(current.left, events);

            events.add(current.event);

            inOrder(current.right, events);
        }
    }
}