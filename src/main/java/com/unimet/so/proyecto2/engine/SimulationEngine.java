package com.unimet.so.proyecto2.engine;

import com.unimet.so.proyecto2.ds.SimpleLinkedList;
import com.unimet.so.proyecto2.model.Block;
import com.unimet.so.proyecto2.model.DirectoryNode;
import com.unimet.so.proyecto2.model.FileNode;
import com.unimet.so.proyecto2.model.FsNode;
import com.unimet.so.proyecto2.model.FsNodeSnapshot;
import com.unimet.so.proyecto2.model.IoProcess;
import com.unimet.so.proyecto2.model.JournalEntry;
import com.unimet.so.proyecto2.model.ProjectTypes;
import com.unimet.so.proyecto2.persistence.SimulatorState;
import com.unimet.so.proyecto2.persistence.TestScenario;
import java.time.Instant;
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

    private ProjectTypes.UserMode userMode;
    private ProjectTypes.SchedulerPolicy schedulerPolicy;
    private ProjectTypes.HeadDirection headDirection;
    private int headPosition;
    private int nextPid;
    private int nextJournalId;
    private boolean simulateFailureOnNextCritical;
    private boolean crashed;
    private volatile boolean autoRunning;
    private Thread autoThread;
    private Runnable onChange;

    public SimulationEngine() {
        disk = new Disk(DEFAULT_BLOCK_COUNT);
        lockManager = new LockManager();
        readyProcesses = new SimpleLinkedList<>();
        blockedProcesses = new SimpleLinkedList<>();
        activeProcesses = new SimpleLinkedList<>();
        completedProcesses = new SimpleLinkedList<>();
        journalEntries = new SimpleLinkedList<>();
        eventLog = new SimpleLinkedList<>();
        reset();
    }

    public synchronized void reset() {
        stopAutoProcessing();
        root = new DirectoryNode("", "system", true);
        disk.reset();
        lockManager.clear();
        readyProcesses.clear();
        blockedProcesses.clear();
        activeProcesses.clear();
        completedProcesses.clear();
        journalEntries.clear();
        eventLog.clear();
        userMode = ProjectTypes.UserMode.ADMIN;
        schedulerPolicy = ProjectTypes.SchedulerPolicy.FIFO;
        headDirection = ProjectTypes.HeadDirection.UP;
        headPosition = 50;
        nextPid = 1;
        nextJournalId = 1;
        simulateFailureOnNextCritical = false;
        crashed = false;
    }

    public synchronized void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }


    public synchronized ProjectTypes.UserMode getUserMode() {
        return userMode;
    }



    public synchronized ProjectTypes.SchedulerPolicy getSchedulerPolicy() {
        return schedulerPolicy;
    }


    public synchronized ProjectTypes.HeadDirection getHeadDirection() {
        return headDirection;
    }

    public synchronized void stopAutoProcessing() {
        if (userMode != null) {
            userMode = null;
        }
        autoRunning = false;
        if (autoThread != null) {
            autoThread.interrupt();
            autoThread = null;
        }
    }

    public synchronized int getHeadPosition() {
        return headPosition;
    }

    public synchronized int getBlockCount() {
        return disk.getBlockCount();
    }

    public synchronized boolean isCrashed() {
        return crashed;
    }
}