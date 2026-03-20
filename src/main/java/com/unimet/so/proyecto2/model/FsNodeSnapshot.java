package com.unimet.so.proyecto2.model;

public class FsNodeSnapshot {
    public ProjectTypes.NodeType nodeType;
    public String name;
    public String owner;
    public boolean publicVisible;
    public int sizeInBlocks;
    public int[] blockChain;
    public FsNodeSnapshot[] children;

    public static FsNodeSnapshot directory(String name, String owner, boolean publicVisible, FsNodeSnapshot[] children) {
        FsNodeSnapshot snapshot = new FsNodeSnapshot();
        snapshot.nodeType = ProjectTypes.NodeType.DIRECTORY;
        snapshot.name = name;
        snapshot.owner = owner;
        snapshot.publicVisible = publicVisible;
        snapshot.children = children;
        snapshot.blockChain = new int[0];
        return snapshot;
    }

    public static FsNodeSnapshot file(String name, String owner, boolean publicVisible, int sizeInBlocks, int[] blockChain) {
        FsNodeSnapshot snapshot = new FsNodeSnapshot();
        snapshot.nodeType = ProjectTypes.NodeType.FILE;
        snapshot.name = name;
        snapshot.owner = owner;
        snapshot.publicVisible = publicVisible;
        snapshot.sizeInBlocks = sizeInBlocks;
        snapshot.blockChain = blockChain == null ? new int[0] : blockChain.clone();
        snapshot.children = new FsNodeSnapshot[0];
        return snapshot;
    }
}