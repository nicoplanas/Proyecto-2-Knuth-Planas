package com.unimet.so.proyecto2.ds;

public class SimpleQueue<T> {
    private final SimpleLinkedList<T> values = new SimpleLinkedList<>();

    public void enqueue(T value) {
        values.addLast(value);
    }

    public T dequeue() {
        return values.removeFirst();
    }

    public T peek() {
        return values.isEmpty() ? null : values.get(0);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void clear() {
        values.clear();
    }
}