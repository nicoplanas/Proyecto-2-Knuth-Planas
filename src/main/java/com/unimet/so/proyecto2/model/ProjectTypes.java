package com.unimet.so.proyecto2.model;

public final class ProjectTypes {
    private ProjectTypes() {
    }

    public enum UserMode {
        ADMIN,
        USER
    }

    public enum OperationType {
        CREATE_FILE,
        CREATE_DIRECTORY,
        READ,
        UPDATE_NAME,
        DELETE
    }

    public enum ProcessState {
        NEW,
        READY,
        EXECUTING,
        BLOCKED,
        TERMINATED,
        FAILED
    }

    public enum LockType {
        SHARED,
        EXCLUSIVE
    }

    public enum SchedulerPolicy {
        FIFO,
        SSTF,
        SCAN,
        C_SCAN
    }

    public enum HeadDirection {
        UP,
        DOWN
    }

    public enum JournalStatus {
        PENDING,
        COMMITTED,
        UNDONE
    }

    public enum NodeType {
        DIRECTORY,
        FILE
    }
}