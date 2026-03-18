package com.unimet.so.proyecto2.model;

public class IoProcess {
    private final int pid;
    private final ProjectTypes.OperationType operationType;
    private final String targetPath;
    private final String resourcePath;
    private final String owner;
    private final int requestedBlocks;
    private final boolean publicVisible;
    private final int targetPosition;
    private String newName;
    private ProjectTypes.ProcessState state;

    public IoProcess(
            int pid,
            ProjectTypes.OperationType operationType,
            String targetPath,
            String resourcePath,
            String owner,
            int requestedBlocks,
            boolean publicVisible,
            int targetPosition,
            String newName
    ) {
        this.pid = pid;
        this.operationType = operationType;
        this.targetPath = targetPath;
        this.resourcePath = resourcePath;
        this.owner = owner;
        this.requestedBlocks = requestedBlocks;
        this.publicVisible = publicVisible;
        this.targetPosition = targetPosition;
        this.newName = newName;
        this.state = ProjectTypes.ProcessState.NEW;
    }

    public int getPid() {
        return pid;
    }

    public ProjectTypes.OperationType getOperationType() {
        return operationType;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getOwner() {
        return owner;
    }

    public int getRequestedBlocks() {
        return requestedBlocks;
    }

    public boolean isPublicVisible() {
        return publicVisible;
    }

    public int getTargetPosition() {
        return targetPosition;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public ProjectTypes.ProcessState getState() {
        return state;
    }

    public void setState(ProjectTypes.ProcessState state) {
        this.state = state;
    }

    public ProjectTypes.LockType getRequiredLock() {
        if (operationType == ProjectTypes.OperationType.READ) {
            return ProjectTypes.LockType.SHARED;
        }
        return ProjectTypes.LockType.EXCLUSIVE;
    }

    public boolean isCriticalOperation() {
        return operationType == ProjectTypes.OperationType.CREATE_FILE
                || operationType == ProjectTypes.OperationType.CREATE_DIRECTORY
                || operationType == ProjectTypes.OperationType.DELETE;
    }
}