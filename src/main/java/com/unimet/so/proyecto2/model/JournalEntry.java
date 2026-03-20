package com.unimet.so.proyecto2.model;

public class JournalEntry {
    private final int id;
    private final ProjectTypes.OperationType operationType;
    private final String targetPath;
    private final long createdAt;
    private final String description;
    private ProjectTypes.JournalStatus status;
    private FsNodeSnapshot snapshot;

    public JournalEntry(int id, ProjectTypes.OperationType operationType, String targetPath, String description) {
        this.id = id;
        this.operationType = operationType;
        this.targetPath = targetPath;
        this.createdAt = System.currentTimeMillis();
        this.description = description;
        this.status = ProjectTypes.JournalStatus.PENDING;
    }

    public int getId() {
        return id;
    }

    public ProjectTypes.OperationType getOperationType() {
        return operationType;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public ProjectTypes.JournalStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectTypes.JournalStatus status) {
        this.status = status;
    }

    public FsNodeSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(FsNodeSnapshot snapshot) {
        this.snapshot = snapshot;
    }
}