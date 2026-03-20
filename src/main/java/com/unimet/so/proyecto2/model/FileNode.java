package com.unimet.so.proyecto2.model;

public class FileNode extends FsNode {
    private int sizeInBlocks;
    private int[] blockChain;

    public FileNode(String name, String owner, boolean publicVisible, int sizeInBlocks, int[] blockChain) {
        super(name, owner, publicVisible);
        this.sizeInBlocks = sizeInBlocks;
        this.blockChain = blockChain == null ? new int[0] : blockChain.clone();
    }

    public int getSizeInBlocks() {
        return sizeInBlocks;
    }

    public void setSizeInBlocks(int sizeInBlocks) {
        this.sizeInBlocks = sizeInBlocks;
    }

    public int[] getBlockChain() {
        return blockChain.clone();
    }

    public void setBlockChain(int[] blockChain) {
        this.blockChain = blockChain == null ? new int[0] : blockChain.clone();
    }

    public int getFirstBlock() {
        return blockChain.length == 0 ? -1 : blockChain[0];
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public FsNodeSnapshot toSnapshot() {
        return FsNodeSnapshot.file(getName(), getOwner(), isPublicVisible(), sizeInBlocks, blockChain);
    }
}