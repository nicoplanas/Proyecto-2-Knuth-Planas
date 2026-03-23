package com.unimet.so.proyecto2.engine;

import com.unimet.so.proyecto2.ds.SimpleLinkedList;
import com.unimet.so.proyecto2.model.DirectoryNode;
import com.unimet.so.proyecto2.model.IoProcess;
import com.unimet.so.proyecto2.model.JournalEntry;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SimulationEngine {
    private static final int DEFAULT_BLOCK_COUNT = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
   
    private DirectoryNode root;
    private final Disk disk;
    private final LockManager lockManager;
    private final SimpleLinkedList<IoProcess> readyProcesses;
    private final SimpleLinkedList<IoProcess> blockedProcesses;
    private final SimpleLinkedList<IoProcess> activeProcesses;
    private final SimpleLinkedList<IoProcess> completedProcesses;
    private final SimpleLinkedList<JournalEntry> journalEntries;
    private final SimpleLinkedList<String> eventLog;

    public SimulationEngine() {
        disk = new Disk(DEFAULT_BLOCK_COUNT);
        lockManager = new LockManager();
        readyProcesses = new SimpleLinkedList<>();
        blockedProcesses = new SimpleLinkedList<>();
        activeProcesses = new SimpleLinkedList<>();
        completedProcesses = new SimpleLinkedList<>();
        journalEntries = new SimpleLinkedList<>();
        eventLog = new SimpleLinkedList<>();
    }
}
