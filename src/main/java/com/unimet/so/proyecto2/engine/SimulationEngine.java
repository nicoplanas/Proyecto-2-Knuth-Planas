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
        ensureDirectory("/home", "system", true);
        ensureDirectory("/system", "system", true);
        logEvent("Simulador reiniciado");
        notifyChange();
    }

    public synchronized void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public synchronized void setUserMode(ProjectTypes.UserMode userMode) {
        this.userMode = userMode;
        logEvent("Modo cambiado a " + userMode);
        notifyChange();
    }

    public synchronized ProjectTypes.UserMode getUserMode() {
        return userMode;
    }

    public synchronized void setSchedulerPolicy(ProjectTypes.SchedulerPolicy schedulerPolicy) {
        requireAdminControl();
        this.schedulerPolicy = schedulerPolicy;
        logEvent("Planificador cambiado a " + schedulerPolicy);
        notifyChange();
    }

    public synchronized ProjectTypes.SchedulerPolicy getSchedulerPolicy() {
        return schedulerPolicy;
    }

    public synchronized void setHeadDirection(ProjectTypes.HeadDirection headDirection) {
        requireAdminControl();
        this.headDirection = headDirection;
        logEvent("Direccion del cabezal: " + headDirection);
        notifyChange();
    }

    public synchronized ProjectTypes.HeadDirection getHeadDirection() {
        return headDirection;
    }

    public synchronized void setHeadPosition(int headPosition) {
        requireAdminControl();
        if (headPosition < 0 || headPosition >= disk.getBlockCount()) {
            throw new IllegalArgumentException("Cabezal fuera de rango");
        }
        this.headPosition = headPosition;
        logEvent("Cabezal colocado en " + headPosition);
        notifyChange();
    }

    public synchronized int getHeadPosition() {
        return headPosition;
    }

    public synchronized int getBlockCount() {
        return disk.getBlockCount();
    }

    public synchronized void armSimulatedFailure() {
        requireAdminControl();
        simulateFailureOnNextCritical = true;
        logEvent("Se armara un fallo en la siguiente operacion critica");
        notifyChange();
    }

    public synchronized boolean isCrashed() {
        return crashed;
    }

    public synchronized void startAutoProcessing() {
        requireAdminControl();
        if (autoRunning) {
            return;
        }
        autoRunning = true;
        autoThread = new Thread(() -> {
            while (autoRunning) {
                try {
                    dispatchNextProcess();
                    Thread.sleep(500L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "scheduler-dispatcher");
        autoThread.setDaemon(true);
        autoThread.start();
        logEvent("Procesamiento automatico activado");
        notifyChange();
    }

    public synchronized void stopAutoProcessing() {
        if (userMode != null) {
            requireAdminControl();
        }
        autoRunning = false;
        if (autoThread != null) {
            autoThread.interrupt();
            autoThread = null;
        }
    }

    public synchronized boolean isAutoRunning() {
        return autoRunning;
    }

    public synchronized void recoverPendingJournal() {
        requireAdminControl();
        for (int index = 0; index < journalEntries.size(); index++) {
            JournalEntry entry = journalEntries.get(index);
            if (entry.getStatus() == ProjectTypes.JournalStatus.PENDING) {
                undoJournalEntry(entry);
                entry.setStatus(ProjectTypes.JournalStatus.UNDONE);
                logEvent("UNDO aplicado para " + entry.getTargetPath());
            }
        }
        crashed = false;
        simulateFailureOnNextCritical = false;
        notifyChange();
    }

    public synchronized void enqueueCreateDirectory(String parentPath, String name, String owner, boolean publicVisible) {
        requireWritableMode();
        String normalizedName = normalizeName(name);
        DirectoryNode parent = findDirectory(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("Directorio padre invalido");
        }
        if (parent.findChildByName(normalizedName) != null) {
            throw new IllegalArgumentException("Ya existe un elemento con ese nombre");
        }
        String path = buildChildPath(parentPath, normalizedName);
        enqueueProcess(new IoProcess(nextPid++, ProjectTypes.OperationType.CREATE_DIRECTORY, path, parentPath, owner, 0, publicVisible, headPosition, null));
    }

    public synchronized void enqueueCreateFile(String parentPath, String name, int blocks, String owner, boolean publicVisible, int preferredStart) {
        requireWritableMode();
        if (blocks <= 0) {
            throw new IllegalArgumentException("El tamano del archivo debe ser mayor a 0 bloques");
        }
        String normalizedName = normalizeName(name);
        DirectoryNode parent = findDirectory(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("Directorio padre invalido");
        }
        if (parent.findChildByName(normalizedName) != null) {
            throw new IllegalArgumentException("Ya existe un elemento con ese nombre");
        }
        String path = buildChildPath(parentPath, normalizedName);
        enqueueProcess(new IoProcess(nextPid++, ProjectTypes.OperationType.CREATE_FILE, path, parentPath, owner, blocks, publicVisible, preferredStart, null));
    }

    public synchronized void enqueueRead(String targetPath, String owner) {
        FsNode node = findNode(targetPath);
        if (node == null) {
            throw new IllegalArgumentException("El archivo o directorio no existe");
        }
        if (!canRead(node, owner)) {
            throw new IllegalArgumentException("No tiene permisos de lectura sobre este recurso");
        }
        enqueueProcess(new IoProcess(nextPid++, ProjectTypes.OperationType.READ, targetPath, targetPath, owner, 0, node.isPublicVisible(), resolveTargetPosition(targetPath), null));
    }

    public synchronized void enqueueRename(String targetPath, String newName, String owner) {
        requireWritableMode();
        FsNode node = findNode(targetPath);
        if (node == null || "/".equals(targetPath)) {
            throw new IllegalArgumentException("No se puede renombrar el recurso solicitado");
        }
        enqueueProcess(new IoProcess(nextPid++, ProjectTypes.OperationType.UPDATE_NAME, targetPath, targetPath, owner, 0, node.isPublicVisible(), resolveTargetPosition(targetPath), normalizeName(newName)));
    }

    public synchronized void enqueueDelete(String targetPath, String owner) {
        requireWritableMode();
        FsNode node = findNode(targetPath);
        if (node == null || "/".equals(targetPath)) {
            throw new IllegalArgumentException("No se puede eliminar el recurso solicitado");
        }
        enqueueProcess(new IoProcess(nextPid++, ProjectTypes.OperationType.DELETE, targetPath, targetPath, owner, 0, node.isPublicVisible(), resolveTargetPosition(targetPath), null));
    }

    public synchronized boolean dispatchNextProcess() {
        requireAdminControl();
        if (crashed || readyProcesses.isEmpty()) {
            notifyChange();
            return false;
        }
        retryBlockedProcesses();
        int nextIndex = selectReadyIndex();
        if (nextIndex < 0) {
            notifyChange();
            return false;
        }
        IoProcess process = readyProcesses.removeAt(nextIndex);
        if (!lockManager.canAcquire(process.getResourcePath(), process.getRequiredLock(), process.getPid())) {
            process.setState(ProjectTypes.ProcessState.BLOCKED);
            blockedProcesses.add(process);
            logEvent("P" + process.getPid() + " bloqueado por lock sobre " + process.getResourcePath());
            notifyChange();
            return false;
        }
        lockManager.acquire(process.getResourcePath(), process.getRequiredLock(), process.getPid());
        process.setState(ProjectTypes.ProcessState.EXECUTING);
        headPosition = process.getTargetPosition();
        activeProcesses.add(process);
        logEvent("P" + process.getPid() + " ejecutando " + process.getOperationType() + " en " + process.getTargetPath());
        notifyChange();

        Thread worker = new Thread(() -> executeProcess(process), "io-process-" + process.getPid());
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    public synchronized FsNodeSnapshot getTreeSnapshot() {
        return root.toSnapshot();
    }

    public synchronized FsNodeSnapshot getTreeSnapshotFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return root.toSnapshot();
        }
        FsNodeSnapshot snapshot = filterNodeSnapshot(root, owner, true);
        return snapshot == null ? FsNodeSnapshot.directory("", "system", true, new FsNodeSnapshot[0]) : snapshot;
    }

    public synchronized Block[] getBlockSnapshot() {
        return disk.snapshot();
    }

    public synchronized Block[] getBlockSnapshotFor(String owner) {
        Block[] copy = disk.snapshot();
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return copy;
        }
        for (int index = 0; index < copy.length; index++) {
            Block block = copy[index];
            if (!block.isAllocated() || block.getFilePath() == null) {
                continue;
            }
            FsNode node = findNode(block.getFilePath());
            if (node != null && !canRead(node, owner)) {
                block.setFilePath("/privado");
                block.setOwner("privado");
                block.setNextIndex(-1);
            }
        }
        return copy;
    }

    public synchronized String[][] getFatRows() {
        SimpleLinkedList<FileNode> files = new SimpleLinkedList<>();
        collectFiles(root, files);
        String[][] rows = new String[files.size()][5];
        for (int index = 0; index < files.size(); index++) {
            FileNode file = files.get(index);
            rows[index][0] = file.getPath();
            rows[index][1] = String.valueOf(file.getSizeInBlocks());
            rows[index][2] = String.valueOf(file.getFirstBlock());
            rows[index][3] = file.getOwner();
            rows[index][4] = colorForPath(file.getPath());
        }
        return rows;
    }

    public synchronized String[][] getFatRowsFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return getFatRows();
        }
        SimpleLinkedList<FileNode> files = new SimpleLinkedList<>();
        collectFiles(root, files);
        SimpleLinkedList<FileNode> visibleFiles = new SimpleLinkedList<>();
        for (int index = 0; index < files.size(); index++) {
            FileNode file = files.get(index);
            if (canRead(file, owner)) {
                visibleFiles.add(file);
            }
        }
        String[][] rows = new String[visibleFiles.size()][5];
        for (int index = 0; index < visibleFiles.size(); index++) {
            FileNode file = visibleFiles.get(index);
            rows[index][0] = file.getPath();
            rows[index][1] = String.valueOf(file.getSizeInBlocks());
            rows[index][2] = String.valueOf(file.getFirstBlock());
            rows[index][3] = file.getOwner();
            rows[index][4] = colorForPath(file.getPath());
        }
        return rows;
    }

    public synchronized String[][] getProcessRows() {
        int total = readyProcesses.size() + blockedProcesses.size() + activeProcesses.size() + completedProcesses.size();
        String[][] rows = new String[total][6];
        int index = 0;
        index = fillProcessRows(rows, index, activeProcesses);
        index = fillProcessRows(rows, index, readyProcesses);
        index = fillProcessRows(rows, index, blockedProcesses);
        fillProcessRows(rows, index, completedProcesses);
        return rows;
    }

    public synchronized String[][] getProcessRowsFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return getProcessRows();
        }
        int total = countOwnedProcesses(activeProcesses, owner)
                + countOwnedProcesses(readyProcesses, owner)
                + countOwnedProcesses(blockedProcesses, owner)
                + countOwnedProcesses(completedProcesses, owner);
        String[][] rows = new String[total][6];
        int index = 0;
        index = fillOwnedProcessRows(rows, index, activeProcesses, owner);
        index = fillOwnedProcessRows(rows, index, readyProcesses, owner);
        index = fillOwnedProcessRows(rows, index, blockedProcesses, owner);
        fillOwnedProcessRows(rows, index, completedProcesses, owner);
        return rows;
    }

    public synchronized String[][] getLockRows() {
        return lockManager.snapshotTable();
    }

    public synchronized String[][] getLockRowsFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return getLockRows();
        }
        return new String[0][3];
    }

    public synchronized String[][] getJournalRows() {
        String[][] rows = new String[journalEntries.size()][5];
        for (int index = 0; index < journalEntries.size(); index++) {
            JournalEntry entry = journalEntries.get(index);
            rows[index][0] = String.valueOf(entry.getId());
            rows[index][1] = entry.getOperationType().name();
            rows[index][2] = entry.getTargetPath();
            rows[index][3] = entry.getStatus().name();
            rows[index][4] = TIME_FORMAT.format(Instant.ofEpochMilli(entry.getCreatedAt()));
        }
        return rows;
    }

    public synchronized String[][] getJournalRowsFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return getJournalRows();
        }
        return new String[0][5];
    }

    public synchronized String[] getEventLogLines() {
        return eventLog.toArray(String[]::new);
    }

    public synchronized String[] getEventLogLinesFor(String owner) {
        if (userMode == ProjectTypes.UserMode.ADMIN) {
            return getEventLogLines();
        }
        return new String[]{"Vista de log limitada en modo usuario"};
    }

    public synchronized String describeNode(String path) {
        FsNode node = findNode(path);
        if (node == null) {
            return "Seleccione un archivo o directorio";
        }
        if (node instanceof FileNode fileNode) {
            return "Archivo: " + fileNode.getPath() + " | Dueno: " + fileNode.getOwner() + " | Bloques: " + fileNode.getSizeInBlocks() + " | Primero: " + fileNode.getFirstBlock();
        }
        return "Directorio: " + node.getPath() + " | Dueno: " + node.getOwner() + " | Hijos: " + ((DirectoryNode) node).getChildren().size();
    }

    public synchronized String describeNode(String path, String owner) {
        FsNode node = findNode(path);
        if (node == null) {
            return "Seleccione un archivo o directorio";
        }
        if (userMode == ProjectTypes.UserMode.USER && !canRead(node, owner)) {
            return "No tiene permisos para ver este recurso";
        }
        return describeNode(path);
    }

    public synchronized SimulatorState exportState() {
        requireAdminControl();
        SimulatorState state = new SimulatorState();
        state.blockCount = disk.getBlockCount();
        state.headPosition = headPosition;
        state.schedulerPolicy = schedulerPolicy.name();
        state.headDirection = headDirection.name();
        state.userMode = userMode.name();
        state.nextPid = nextPid;
        state.nextJournalId = nextJournalId;
        state.rootSnapshot = root.toSnapshot();
        SimulatorState.JournalEntryState[] entries = new SimulatorState.JournalEntryState[journalEntries.size()];
        for (int index = 0; index < journalEntries.size(); index++) {
            JournalEntry entry = journalEntries.get(index);
            SimulatorState.JournalEntryState stateEntry = new SimulatorState.JournalEntryState();
            stateEntry.id = entry.getId();
            stateEntry.operationType = entry.getOperationType().name();
            stateEntry.targetPath = entry.getTargetPath();
            stateEntry.createdAt = entry.getCreatedAt();
            stateEntry.description = entry.getDescription();
            stateEntry.status = entry.getStatus().name();
            stateEntry.snapshot = entry.getSnapshot();
            entries[index] = stateEntry;
        }
        state.journalEntries = entries;
        return state;
    }

    public synchronized void importState(SimulatorState state) {
        requireAdminControl();
        reset();
        disk.reset();
        root = restoreDirectoryTree(state.rootSnapshot, null);
        rebuildDiskFromTree(root);
        headPosition = state.headPosition;
        schedulerPolicy = ProjectTypes.SchedulerPolicy.valueOf(state.schedulerPolicy);
        headDirection = ProjectTypes.HeadDirection.valueOf(state.headDirection);
        userMode = ProjectTypes.UserMode.valueOf(state.userMode);
        nextPid = state.nextPid;
        nextJournalId = state.nextJournalId;
        journalEntries.clear();
        if (state.journalEntries != null) {
            for (SimulatorState.JournalEntryState entryState : state.journalEntries) {
                JournalEntry entry = new JournalEntry(entryState.id, ProjectTypes.OperationType.valueOf(entryState.operationType), entryState.targetPath, entryState.description);
                entry.setStatus(ProjectTypes.JournalStatus.valueOf(entryState.status));
                entry.setSnapshot(entryState.snapshot);
                journalEntries.add(entry);
            }
        }
        logEvent("Estado cargado desde JSON");
        notifyChange();
    }

    public synchronized void loadScenario(TestScenario scenario) {
        requireAdminControl();
        reset();
        setHeadPosition(scenario.initialHead);
        DirectoryNode systemDirectory = ensureDirectory("/system", "system", true);
        for (TestScenario.FileSeed seed : scenario.systemFiles) {
            String filePath = buildChildPath(systemDirectory.getPath(), seed.name);
            int[] chain = disk.allocateFromStart(seed.blocks, seed.position, filePath, "system");
            if (chain != null) {
                FileNode fileNode = new FileNode(seed.name, "system", true, seed.blocks, chain);
                systemDirectory.addChild(fileNode);
            }
        }
        for (TestScenario.Request request : scenario.requests) {
            String path = findFileByFirstBlock(request.pos);
            if (path == null) {
                continue;
            }
            ProjectTypes.OperationType operationType = switch (request.op == null ? "READ" : request.op.trim().toUpperCase()) {
                case "UPDATE" -> ProjectTypes.OperationType.UPDATE_NAME;
                case "DELETE" -> ProjectTypes.OperationType.DELETE;
                default -> ProjectTypes.OperationType.READ;
            };
            String newName = operationType == ProjectTypes.OperationType.UPDATE_NAME ? "renamed_" + request.pos + ".dat" : null;
            enqueueProcess(new IoProcess(nextPid++, operationType, path, path, "system", 0, true, request.pos, newName));
        }
        logEvent("Escenario " + scenario.testId + " cargado");
        notifyChange();
    }

    private void executeProcess(IoProcess process) {
        try {
            Thread.sleep(800L);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            try {
                applyProcess(process);
                if (process.getState() != ProjectTypes.ProcessState.FAILED) {
                    process.setState(ProjectTypes.ProcessState.TERMINATED);
                    logEvent("P" + process.getPid() + " finalizado");
                }
            } catch (RuntimeException runtimeException) {
                process.setState(ProjectTypes.ProcessState.FAILED);
                logEvent("P" + process.getPid() + " fallo: " + runtimeException.getMessage());
            } finally {
                activeProcesses.removeReference(process);
                completedProcesses.add(process);
                lockManager.release(process.getResourcePath(), process.getPid());
                retryBlockedProcesses();
                notifyChange();
            }
        }
    }

    private void applyProcess(IoProcess process) {
        switch (process.getOperationType()) {
            case CREATE_DIRECTORY -> applyCreateDirectory(process);
            case CREATE_FILE -> applyCreateFile(process);
            case READ -> applyRead(process);
            case UPDATE_NAME -> applyRename(process);
            case DELETE -> applyDelete(process);
        }
    }

    private void applyCreateDirectory(IoProcess process) {
        DirectoryNode parent = findDirectory(process.getResourcePath());
        if (parent == null) {
            throw new IllegalArgumentException("Directorio padre no encontrado");
        }
        String name = nameFromPath(process.getTargetPath());
        if (parent.findChildByName(name) != null) {
            throw new IllegalArgumentException("Ya existe el directorio");
        }
        JournalEntry entry = beginJournal(process, "CREATE_DIRECTORY");
        DirectoryNode node = new DirectoryNode(name, process.getOwner(), process.isPublicVisible());
        parent.addChild(node);
        entry.setSnapshot(node.toSnapshot());
        failIfArmed(process, entry);
        commitJournal(entry);
    }

    private void applyCreateFile(IoProcess process) {
        DirectoryNode parent = findDirectory(process.getResourcePath());
        if (parent == null) {
            throw new IllegalArgumentException("Directorio padre no encontrado");
        }
        String name = nameFromPath(process.getTargetPath());
        if (parent.findChildByName(name) != null) {
            throw new IllegalArgumentException("Ya existe el archivo");
        }
        int[] chain = disk.allocateChained(process.getRequestedBlocks(), process.getTargetPosition(), process.getTargetPath(), process.getOwner());
        if (chain == null) {
            throw new IllegalArgumentException("No hay espacio suficiente en disco");
        }
        JournalEntry entry = beginJournal(process, "CREATE_FILE");
        FileNode node = new FileNode(name, process.getOwner(), process.isPublicVisible(), process.getRequestedBlocks(), chain);
        parent.addChild(node);
        entry.setSnapshot(node.toSnapshot());
        failIfArmed(process, entry);
        commitJournal(entry);
    }

    private void applyRead(IoProcess process) {
        FsNode node = findNode(process.getTargetPath());
        if (node == null) {
            throw new IllegalArgumentException("Recurso no encontrado");
        }
        logEvent("Lectura completada sobre " + node.getPath());
    }

    private void applyRename(IoProcess process) {
        FsNode node = findNode(process.getTargetPath());
        if (node == null) {
            throw new IllegalArgumentException("Recurso no encontrado");
        }
        DirectoryNode parent = node.getParent();
        if (parent != null && parent.findChildByName(process.getNewName()) != null) {
            throw new IllegalArgumentException("Ya existe un hermano con ese nombre");
        }
        String oldPath = node.getPath();
        node.setName(process.getNewName());
        if (node instanceof FileNode fileNode) {
            refreshDiskOwnershipForPath(fileNode.getBlockChain(), oldPath, fileNode.getPath(), fileNode.getOwner());
        }
        logEvent("Recurso renombrado a " + node.getPath());
    }

    private void applyDelete(IoProcess process) {
        FsNode node = findNode(process.getTargetPath());
        if (node == null || node.getParent() == null) {
            throw new IllegalArgumentException("No se puede eliminar el recurso");
        }
        JournalEntry entry = beginJournal(process, "DELETE");
        entry.setSnapshot(node.toSnapshot());
        removeNode(node);
        failIfArmed(process, entry);
        commitJournal(entry);
    }

    private void retryBlockedProcesses() {
        int index = 0;
        while (index < blockedProcesses.size()) {
            IoProcess process = blockedProcesses.get(index);
            if (lockManager.canAcquire(process.getResourcePath(), process.getRequiredLock(), process.getPid())) {
                blockedProcesses.removeAt(index);
                process.setState(ProjectTypes.ProcessState.READY);
                readyProcesses.add(process);
                logEvent("P" + process.getPid() + " vuelve a listo");
                continue;
            }
            index++;
        }
    }

    private int selectReadyIndex() {
        if (readyProcesses.isEmpty()) {
            return -1;
        }
        return switch (schedulerPolicy) {
            case FIFO -> 0;
            case SSTF -> selectSstfIndex();
            case SCAN -> selectScanIndex(false);
            case C_SCAN -> selectScanIndex(true);
        };
    }

    private int selectSstfIndex() {
        int selectedIndex = 0;
        int minDistance = Integer.MAX_VALUE;
        for (int index = 0; index < readyProcesses.size(); index++) {
            int distance = Math.abs(readyProcesses.get(index).getTargetPosition() - headPosition);
            if (distance < minDistance) {
                minDistance = distance;
                selectedIndex = index;
            }
        }
        return selectedIndex;
    }

    private int selectScanIndex(boolean circular) {
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < readyProcesses.size(); index++) {
            IoProcess process = readyProcesses.get(index);
            int distance = process.getTargetPosition() - headPosition;
            boolean sameDirection = headDirection == ProjectTypes.HeadDirection.UP ? distance >= 0 : distance <= 0;
            if (!sameDirection) {
                continue;
            }
            int absoluteDistance = Math.abs(distance);
            if (absoluteDistance < bestDistance) {
                bestDistance = absoluteDistance;
                bestIndex = index;
            }
        }
        if (bestIndex >= 0) {
            return bestIndex;
        }
        if (!circular) {
            headDirection = headDirection == ProjectTypes.HeadDirection.UP ? ProjectTypes.HeadDirection.DOWN : ProjectTypes.HeadDirection.UP;
            return selectScanIndex(true);
        }
        int wrapIndex = 0;
        int candidatePosition = headDirection == ProjectTypes.HeadDirection.UP ? Integer.MAX_VALUE : -1;
        for (int index = 0; index < readyProcesses.size(); index++) {
            int position = readyProcesses.get(index).getTargetPosition();
            if (headDirection == ProjectTypes.HeadDirection.UP && position < candidatePosition) {
                candidatePosition = position;
                wrapIndex = index;
            }
            if (headDirection == ProjectTypes.HeadDirection.DOWN && position > candidatePosition) {
                candidatePosition = position;
                wrapIndex = index;
            }
        }
        return wrapIndex;
    }

    private int fillProcessRows(String[][] rows, int startIndex, SimpleLinkedList<IoProcess> processes) {
        int index = startIndex;
        for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
            IoProcess process = processes.get(processIndex);
            rows[index][0] = String.valueOf(process.getPid());
            rows[index][1] = process.getOperationType().name();
            rows[index][2] = process.getTargetPath();
            rows[index][3] = String.valueOf(process.getTargetPosition());
            rows[index][4] = process.getState().name();
            rows[index][5] = process.getOwner();
            index++;
        }
        return index;
    }

    private int countOwnedProcesses(SimpleLinkedList<IoProcess> processes, String owner) {
        int count = 0;
        for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
            IoProcess process = processes.get(processIndex);
            if (matchesOwner(process.getOwner(), owner)) {
                count++;
            }
        }
        return count;
    }

    private int fillOwnedProcessRows(String[][] rows, int startIndex, SimpleLinkedList<IoProcess> processes, String owner) {
        int index = startIndex;
        for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
            IoProcess process = processes.get(processIndex);
            if (!matchesOwner(process.getOwner(), owner)) {
                continue;
            }
            rows[index][0] = String.valueOf(process.getPid());
            rows[index][1] = process.getOperationType().name();
            rows[index][2] = process.getTargetPath();
            rows[index][3] = String.valueOf(process.getTargetPosition());
            rows[index][4] = process.getState().name();
            rows[index][5] = process.getOwner();
            index++;
        }
        return index;
    }

    private void enqueueProcess(IoProcess process) {
        if (crashed) {
            throw new IllegalStateException("El sistema esta detenido por un fallo pendiente de recuperar");
        }
        process.setState(ProjectTypes.ProcessState.READY);
        readyProcesses.add(process);
        logEvent("P" + process.getPid() + " encolado: " + process.getOperationType() + " -> " + process.getTargetPath());
        notifyChange();
    }

    private JournalEntry beginJournal(IoProcess process, String description) {
        JournalEntry entry = new JournalEntry(nextJournalId++, process.getOperationType(), process.getTargetPath(), description);
        journalEntries.add(entry);
        logEvent("Journal PENDIENTE para " + process.getTargetPath());
        return entry;
    }

    private void commitJournal(JournalEntry entry) {
        entry.setStatus(ProjectTypes.JournalStatus.COMMITTED);
        logEvent("Journal COMMIT para " + entry.getTargetPath());
    }

    private void failIfArmed(IoProcess process, JournalEntry entry) {
        if (!simulateFailureOnNextCritical) {
            return;
        }
        simulateFailureOnNextCritical = false;
        crashed = true;
        process.setState(ProjectTypes.ProcessState.FAILED);
        logEvent("Fallo simulado antes de commit para " + entry.getTargetPath());
    }

    private void undoJournalEntry(JournalEntry entry) {
        if (entry.getOperationType() == ProjectTypes.OperationType.CREATE_FILE || entry.getOperationType() == ProjectTypes.OperationType.CREATE_DIRECTORY) {
            FsNode currentNode = findNode(entry.getTargetPath());
            if (currentNode != null) {
                removeNode(currentNode);
            }
            return;
        }
        if (entry.getOperationType() == ProjectTypes.OperationType.DELETE && entry.getSnapshot() != null) {
            String parentPath = parentPath(entry.getTargetPath());
            DirectoryNode parent = findDirectory(parentPath);
            if (parent != null) {
                restoreSnapshot(entry.getSnapshot(), parent);
            }
        }
    }

    private void removeNode(FsNode node) {
        if (node instanceof FileNode fileNode) {
            disk.freeChain(fileNode.getBlockChain());
        } else {
            DirectoryNode directoryNode = (DirectoryNode) node;
            while (directoryNode.getChildren().size() > 0) {
                removeNode(directoryNode.getChildren().get(0));
            }
        }
        DirectoryNode parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node);
        }
    }

    private void restoreSnapshot(FsNodeSnapshot snapshot, DirectoryNode parent) {
        if (snapshot.nodeType == ProjectTypes.NodeType.FILE) {
            FileNode fileNode = new FileNode(snapshot.name, snapshot.owner, snapshot.publicVisible, snapshot.sizeInBlocks, snapshot.blockChain);
            parent.addChild(fileNode);
            disk.restoreChain(snapshot.blockChain, fileNode.getPath(), fileNode.getOwner());
            return;
        }
        DirectoryNode directoryNode = new DirectoryNode(snapshot.name, snapshot.owner, snapshot.publicVisible);
        parent.addChild(directoryNode);
        if (snapshot.children != null) {
            for (FsNodeSnapshot child : snapshot.children) {
                restoreSnapshot(child, directoryNode);
            }
        }
    }

    private void rebuildDiskFromTree(DirectoryNode directoryNode) {
        for (int index = 0; index < directoryNode.getChildren().size(); index++) {
            FsNode child = directoryNode.getChildren().get(index);
            if (child instanceof FileNode fileNode) {
                disk.restoreChain(fileNode.getBlockChain(), fileNode.getPath(), fileNode.getOwner());
            } else {
                rebuildDiskFromTree((DirectoryNode) child);
            }
        }
    }

    private DirectoryNode restoreDirectoryTree(FsNodeSnapshot snapshot, DirectoryNode parent) {
        DirectoryNode current = new DirectoryNode(snapshot.name, snapshot.owner, snapshot.publicVisible);
        current.setParent(parent);
        if (snapshot.children != null) {
            for (FsNodeSnapshot child : snapshot.children) {
                if (child.nodeType == ProjectTypes.NodeType.FILE) {
                    current.addChild(new FileNode(child.name, child.owner, child.publicVisible, child.sizeInBlocks, child.blockChain));
                } else {
                    current.addChild(restoreDirectoryTree(child, current));
                }
            }
        }
        return current;
    }

    private void collectFiles(DirectoryNode directoryNode, SimpleLinkedList<FileNode> files) {
        for (int index = 0; index < directoryNode.getChildren().size(); index++) {
            FsNode child = directoryNode.getChildren().get(index);
            if (child instanceof FileNode fileNode) {
                files.add(fileNode);
            } else {
                collectFiles((DirectoryNode) child, files);
            }
        }
    }

    private void refreshDiskOwnershipForPath(int[] chain, String previousPath, String newPath, String owner) {
        Block[] blocks = disk.snapshot();
        for (int blockIndex : chain) {
            if (blockIndex >= 0 && blockIndex < blocks.length && previousPath.equals(blocks[blockIndex].getFilePath())) {
                blocks[blockIndex].setFilePath(newPath);
                blocks[blockIndex].setOwner(owner);
            }
        }
        disk.freeChain(chain);
        disk.restoreChain(chain, newPath, owner);
    }

    private FsNodeSnapshot filterNodeSnapshot(FsNode node, String owner, boolean forceInclude) {
        if (node instanceof FileNode fileNode) {
            if (!forceInclude && !canRead(fileNode, owner)) {
                return null;
            }
            return FsNodeSnapshot.file(fileNode.getName(), fileNode.getOwner(), fileNode.isPublicVisible(), fileNode.getSizeInBlocks(), fileNode.getBlockChain());
        }

        DirectoryNode directoryNode = (DirectoryNode) node;
        SimpleLinkedList<FsNodeSnapshot> visibleChildren = new SimpleLinkedList<>();
        for (int index = 0; index < directoryNode.getChildren().size(); index++) {
            FsNode child = directoryNode.getChildren().get(index);
            FsNodeSnapshot childSnapshot = filterNodeSnapshot(child, owner, false);
            if (childSnapshot != null) {
                visibleChildren.add(childSnapshot);
            }
        }

        boolean visibleDirectory = forceInclude || canRead(directoryNode, owner) || visibleChildren.size() > 0;
        if (!visibleDirectory) {
            return null;
        }

        return FsNodeSnapshot.directory(
                directoryNode.getName(),
                directoryNode.getOwner(),
                directoryNode.isPublicVisible(),
                visibleChildren.toArray(FsNodeSnapshot[]::new)
        );
    }

    private DirectoryNode ensureDirectory(String path, String owner, boolean publicVisible) {
        if ("/".equals(path)) {
            return root;
        }
        String[] segments = tokenize(path);
        DirectoryNode current = root;
        for (String segment : segments) {
            FsNode child = current.findChildByName(segment);
            if (child == null) {
                DirectoryNode directoryNode = new DirectoryNode(segment, owner, publicVisible);
                current.addChild(directoryNode);
                current = directoryNode;
            } else if (child instanceof DirectoryNode directoryNode) {
                current = directoryNode;
            } else {
                throw new IllegalStateException("La ruta contiene un archivo en lugar de directorio");
            }
        }
        return current;
    }

    private FsNode findNode(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return root;
        }
        String[] segments = tokenize(path);
        FsNode current = root;
        for (String segment : segments) {
            if (!(current instanceof DirectoryNode directoryNode)) {
                return null;
            }
            current = directoryNode.findChildByName(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private DirectoryNode findDirectory(String path) {
        FsNode node = findNode(path);
        return node instanceof DirectoryNode directoryNode ? directoryNode : null;
    }

    private boolean canRead(FsNode node, String owner) {
        return userMode == ProjectTypes.UserMode.ADMIN
                || node.isPublicVisible()
                || matchesOwner(node.getOwner(), owner);
    }

    private boolean matchesOwner(String ownerA, String ownerB) {
        return ownerA != null && ownerB != null && ownerA.equalsIgnoreCase(ownerB);
    }

    private int resolveTargetPosition(String path) {
        FsNode node = findNode(path);
        if (node instanceof FileNode fileNode && fileNode.getFirstBlock() >= 0) {
            return fileNode.getFirstBlock();
        }
        return Math.floorMod(path.hashCode(), disk.getBlockCount());
    }

    private String findFileByFirstBlock(int firstBlock) {
        SimpleLinkedList<FileNode> files = new SimpleLinkedList<>();
        collectFiles(root, files);
        for (int index = 0; index < files.size(); index++) {
            FileNode file = files.get(index);
            if (file.getFirstBlock() == firstBlock) {
                return file.getPath();
            }
        }
        return null;
    }

    private void requireWritableMode() {
        requireAdminControl();
        if (crashed) {
            throw new IllegalStateException("Recupere el journal pendiente antes de continuar");
        }
    }

    private void requireAdminControl() {
        if (userMode != ProjectTypes.UserMode.ADMIN) {
            throw new IllegalArgumentException("Solo el administrador puede realizar esta operacion");
        }
    }

    private String[] tokenize(String path) {
        String trimmed = path == null ? "" : path.trim();
        if (trimmed.isEmpty() || "/".equals(trimmed)) {
            return new String[0];
        }
        String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        return normalized.split("/");
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank() || name.contains("/")) {
            throw new IllegalArgumentException("Nombre invalido");
        }
        return name.trim();
    }

    private String buildChildPath(String parentPath, String childName) {
        return "/".equals(parentPath) ? "/" + childName : parentPath + "/" + childName;
    }

    private String parentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash <= 0 ? "/" : path.substring(0, lastSlash);
    }

    private String nameFromPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash < 0 ? path : path.substring(lastSlash + 1);
    }

    private void logEvent(String message) {
        eventLog.addLast(TIME_FORMAT.format(Instant.now()) + " | " + message);
        while (eventLog.size() > 250) {
            eventLog.removeFirst();
        }
    }

    private String colorForPath(String path) {
        int color = Math.floorMod(path.hashCode(), 0xFFFFFF);
        return String.format("#%06X", color);
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }
}