package com.unimet.so.proyecto2.engine;

import com.unimet.so.proyecto2.ds.SimpleLinkedList;
import com.unimet.so.proyecto2.model.ProjectTypes;

public class LockManager {
    private final SimpleLinkedList<LockEntry> entries = new SimpleLinkedList<>();

    public boolean canAcquire(String path, ProjectTypes.LockType type, int pid) {
        LockEntry entry = findEntry(path);
        if (entry == null) {
            return true;
        }
        if (type == ProjectTypes.LockType.SHARED) {
            return entry.exclusiveHolderPid == -1 || entry.exclusiveHolderPid == pid;
        }
        boolean noOtherShared = entry.sharedHolderPids.size() == 0
                || (entry.sharedHolderPids.size() == 1 && entry.sharedHolderPids.get(0) == pid);
        boolean noOtherExclusive = entry.exclusiveHolderPid == -1 || entry.exclusiveHolderPid == pid;
        return noOtherShared && noOtherExclusive;
    }

    public void acquire(String path, ProjectTypes.LockType type, int pid) {
        LockEntry entry = findEntry(path);
        if (entry == null) {
            entry = new LockEntry(path);
            entries.add(entry);
        }
        if (type == ProjectTypes.LockType.SHARED) {
            if (entry.sharedHolderPids.findFirstIndex(value -> value == pid) < 0) {
                entry.sharedHolderPids.add(pid);
            }
        } else {
            entry.exclusiveHolderPid = pid;
        }
    }

    public void release(String path, int pid) {
        LockEntry entry = findEntry(path);
        if (entry == null) {
            return;
        }
        if (entry.exclusiveHolderPid == pid) {
            entry.exclusiveHolderPid = -1;
        }
        int sharedIndex = entry.sharedHolderPids.findFirstIndex(value -> value == pid);
        if (sharedIndex >= 0) {
            entry.sharedHolderPids.removeAt(sharedIndex);
        }
        if (entry.sharedHolderPids.isEmpty() && entry.exclusiveHolderPid == -1) {
            entries.removeReference(entry);
        }
    }

    public void clear() {
        entries.clear();
    }

    public String[][] snapshotTable() {
        String[][] rows = new String[entries.size()][3];
        for (int index = 0; index < entries.size(); index++) {
            LockEntry entry = entries.get(index);
            rows[index][0] = entry.path;
            rows[index][1] = entry.exclusiveHolderPid == -1 ? "SHARED" : "EXCLUSIVE";
            rows[index][2] = buildHolderList(entry);
        }
        return rows;
    }

    private String buildHolderList(LockEntry entry) {
        StringBuilder builder = new StringBuilder();
        if (entry.exclusiveHolderPid != -1) {
            builder.append("P").append(entry.exclusiveHolderPid);
        }
        for (int index = 0; index < entry.sharedHolderPids.size(); index++) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append("P").append(entry.sharedHolderPids.get(index));
        }
        return builder.toString();
    }

    private LockEntry findEntry(String path) {
        int index = entries.findFirstIndex(entry -> entry.path.equals(path));
        return index < 0 ? null : entries.get(index);
    }

    private static final class LockEntry {
        private final String path;
        private final SimpleLinkedList<Integer> sharedHolderPids = new SimpleLinkedList<>();
        private int exclusiveHolderPid = -1;

        private LockEntry(String path) {
            this.path = path;
        }
    }
}