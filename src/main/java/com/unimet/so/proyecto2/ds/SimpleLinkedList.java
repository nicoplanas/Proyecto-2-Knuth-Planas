package com.unimet.so.proyecto2.ds;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class SimpleLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void add(T value) {
        addLast(value);
    }

    public void addFirst(T value) {
        Node<T> node = new Node<>(value);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            node.next = head;
            head = node;
        }
        size++;
    }

    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    public T get(int index) {
        validateIndex(index);
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.value;
    }

    public T removeFirst() {
        if (head == null) {
            return null;
        }
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return value;
    }

    public T removeAt(int index) {
        validateIndex(index);
        if (index == 0) {
            return removeFirst();
        }
        Node<T> previous = head;
        for (int i = 0; i < index - 1; i++) {
            previous = previous.next;
        }
        Node<T> removed = previous.next;
        previous.next = removed.next;
        if (removed == tail) {
            tail = previous;
        }
        size--;
        return removed.value;
    }

    public boolean removeReference(T value) {
        if (head == null) {
            return false;
        }
        if (head.value == value) {
            removeFirst();
            return true;
        }
        Node<T> previous = head;
        Node<T> current = head.next;
        while (current != null) {
            if (current.value == value) {
                previous.next = current.next;
                if (current == tail) {
                    tail = previous;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int findFirstIndex(Predicate<T> predicate) {
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            if (predicate.test(current.value)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    public void forEach(Consumer<T> consumer) {
        Node<T> current = head;
        while (current != null) {
            consumer.accept(current.value);
            current = current.next;
        }
    }

    public T[] toArray(IntFunction<T[]> generator) {
        T[] array = generator.apply(size);
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            array[index++] = current.value;
            current = current.next;
        }
        return array;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Indice invalido: " + index);
        }
    }

    private static final class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }
}