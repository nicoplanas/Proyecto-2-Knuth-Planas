package com.unimet.so.proyecto2.model;

public class Block {
    private final int index;
    private boolean allocated;
    private String filePath;
    private String owner;
    private int nextIndex = -1;

    public Block(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public void reset() {
        allocated = false;
        filePath = null;
        owner = null;
        nextIndex = -1;
    }

    public Block copy() {
        Block copy = new Block(index);
        copy.allocated = allocated;
        copy.filePath = filePath;
        copy.owner = owner;
        copy.nextIndex = nextIndex;
        return copy;
    }
}