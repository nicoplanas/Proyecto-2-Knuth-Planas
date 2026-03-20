package com.unimet.so.proyecto2.model;

import com.unimet.so.proyecto2.ds.SimpleLinkedList;

public class DirectoryNode extends FsNode {
    private final SimpleLinkedList<FsNode> children = new SimpleLinkedList<>();

    public DirectoryNode(String name, String owner, boolean publicVisible) {
        super(name, owner, publicVisible);
    }

    public SimpleLinkedList<FsNode> getChildren() {
        return children;
    }

    public void addChild(FsNode child) {
        child.setParent(this);
        children.add(child);
    }

    public boolean removeChild(FsNode child) {
        child.setParent(null);
        return children.removeReference(child);
    }

    public FsNode findChildByName(String name) {
        for (int index = 0; index < children.size(); index++) {
            FsNode child = children.get(index);
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public FsNodeSnapshot toSnapshot() {
        FsNodeSnapshot[] snapshotChildren = new FsNodeSnapshot[children.size()];
        for (int index = 0; index < children.size(); index++) {
            snapshotChildren[index] = children.get(index).toSnapshot();
        }
        return FsNodeSnapshot.directory(getName(), getOwner(), isPublicVisible(), snapshotChildren);
    }
}