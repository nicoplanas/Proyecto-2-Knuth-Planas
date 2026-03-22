package com.unimet.so.proyecto2.engine;

import com.unimet.so.proyecto2.model.Block;

public class Disk {
    private final Block[] blocks;

    public Disk(int blockCount) {
        blocks = new Block[blockCount];
        for (int index = 0; index < blockCount; index++) {
            blocks[index] = new Block(index);
        }
    }

    public int getBlockCount() {
        return blocks.length;
    }

    public void reset() {
        for (Block block : blocks) {
            block.reset();
        }
    }

    public int getFreeCount() {
        int free = 0;
        for (Block block : blocks) {
            if (!block.isAllocated()) {
                free++;
            }
        }
        return free;
    }

    public int[] allocateChained(int size, int preferredStart, String filePath, String owner) {
        if (size <= 0 || size > getFreeCount()) {
            return null;
        }
        int[] chain = new int[size];
        boolean[] reserved = new boolean[blocks.length];
        int start = findStartingBlock(preferredStart, reserved);
        if (start < 0) {
            return null;
        }
        chain[0] = start;
        reserved[start] = true;
        for (int index = 1; index < size; index++) {
            int next = findNextFree(chain[index - 1] + 1, reserved);
            if (next < 0) {
                return null;
            }
            chain[index] = next;
            reserved[next] = true;
        }
        occupyChain(chain, filePath, owner);
        return chain;
    }

    public int[] allocateFromStart(int size, int firstBlock, String filePath, String owner) {
        if (size <= 0 || firstBlock < 0 || firstBlock >= blocks.length || blocks[firstBlock].isAllocated() || size > getFreeCount()) {
            return null;
        }
        int[] chain = new int[size];
        boolean[] reserved = new boolean[blocks.length];
        chain[0] = firstBlock;
        reserved[firstBlock] = true;
        for (int index = 1; index < size; index++) {
            int next = findNextFree(firstBlock + index, reserved);
            if (next < 0) {
                return null;
            }
            chain[index] = next;
            reserved[next] = true;
        }
        occupyChain(chain, filePath, owner);
        return chain;
    }

    public boolean restoreChain(int[] chain, String filePath, String owner) {
        if (chain == null || chain.length == 0) {
            return true;
        }
        for (int blockIndex : chain) {
            if (blockIndex < 0 || blockIndex >= blocks.length || blocks[blockIndex].isAllocated()) {
                return false;
            }
        }
        occupyChain(chain, filePath, owner);
        return true;
    }

    public void freeChain(int[] chain) {
        if (chain == null) {
            return;
        }
        for (int blockIndex : chain) {
            if (blockIndex >= 0 && blockIndex < blocks.length) {
                blocks[blockIndex].reset();
            }
        }
    }

    public Block[] snapshot() {
        Block[] copy = new Block[blocks.length];
        for (int index = 0; index < blocks.length; index++) {
            copy[index] = blocks[index].copy();
        }
        return copy;
    }

    private void occupyChain(int[] chain, String filePath, String owner) {
        for (int index = 0; index < chain.length; index++) {
            Block block = blocks[chain[index]];
            block.setAllocated(true);
            block.setFilePath(filePath);
            block.setOwner(owner);
            block.setNextIndex(index == chain.length - 1 ? -1 : chain[index + 1]);
        }
    }

    private int findStartingBlock(int preferredStart, boolean[] reserved) {
        if (preferredStart >= 0 && preferredStart < blocks.length && !blocks[preferredStart].isAllocated() && !reserved[preferredStart]) {
            return preferredStart;
        }
        return findNextFree(Math.max(preferredStart, 0), reserved);
    }

    private int findNextFree(int start, boolean[] reserved) {
        int normalized = start % blocks.length;
        for (int offset = 0; offset < blocks.length; offset++) {
            int candidate = (normalized + offset) % blocks.length;
            if (!blocks[candidate].isAllocated() && !reserved[candidate]) {
                return candidate;
            }
        }
        return -1;
    }
}