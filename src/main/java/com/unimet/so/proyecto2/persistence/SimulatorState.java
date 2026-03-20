package com.unimet.so.proyecto2.persistence;

import com.unimet.so.proyecto2.model.FsNodeSnapshot;

public class SimulatorState {
    public int blockCount;
    public int headPosition;
    public String schedulerPolicy;
    public String headDirection;
    public String userMode;
    public int nextPid;
    public int nextJournalId;
    public FsNodeSnapshot rootSnapshot;
    public JournalEntryState[] journalEntries;

    public static class JournalEntryState {
        public int id;
        public String operationType;
        public String targetPath;
        public long createdAt;
        public String description;
        public String status;
        public FsNodeSnapshot snapshot;
    }
}