package com.unimet.so.proyecto2.model;

public abstract class FsNode {
    private String name;
    private String owner;
    private boolean publicVisible;
    private DirectoryNode parent;

    protected FsNode(String name, String owner, boolean publicVisible) {
        this.name = name;
        this.owner = owner;
        this.publicVisible = publicVisible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublicVisible() {
        return publicVisible;
    }

    public void setPublicVisible(boolean publicVisible) {
        this.publicVisible = publicVisible;
    }

    public DirectoryNode getParent() {
        return parent;
    }

    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    public String getPath() {
        if (parent == null) {
            return "/";
        }
        String parentPath = parent.getPath();
        if ("/".equals(parentPath)) {
            return parentPath + name;
        }
        return parentPath + "/" + name;
    }

    public abstract boolean isDirectory();

    public abstract FsNodeSnapshot toSnapshot();
}