package com.unimet.so.proyecto2.ui;

import com.unimet.so.proyecto2.engine.SimulationEngine;
import com.unimet.so.proyecto2.model.Block;
import com.unimet.so.proyecto2.model.FsNodeSnapshot;
import com.unimet.so.proyecto2.model.ProjectTypes;
import com.unimet.so.proyecto2.persistence.SimulatorStateRepository;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class MainFrame extends JFrame {
    private static final Color APP_BG = new Color(17, 25, 38);
    private static final Color CARD_BG = new Color(24, 34, 48);
    private static final Color INPUT_BG = new Color(28, 40, 58);
    private static final Color BORDER_COLOR = new Color(50, 71, 97);
    private static final Color TEXT_COLOR = new Color(231, 239, 247);
    private static final Color MUTED_TEXT = new Color(166, 184, 208);
    private static final Color BUTTON_BG = new Color(36, 55, 79);
    private static final Color BUTTON_HOVER = new Color(48, 75, 110);
    private static final Color ACCENT = new Color(70, 120, 196);
    private static final Color SPINNER_VALUE_BG = new Color(225, 233, 243);
    private static final Color SPINNER_VALUE_TEXT = new Color(22, 31, 44);
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private final SimulationEngine engine;
    private final SimulatorStateRepository repository;

    private final JTree tree;
    private final DiskPanel diskPanel;
    private final JTable fatTable;
    private final JTable processTable;
    private final JTable lockTable;
    private final JTable journalTable;
    private final JTextArea eventLogArea;

    private final JTextField pathField;
    private final JTextField nameField;
    private final JTextField renameField;
    private final JTextField ownerField;
    private final JCheckBox publicCheckBox;
    private final JSpinner sizeSpinner;
    private final JSpinner headSpinner;
    private final JSpinner preferredBlockSpinner;
    private final JComboBox<ProjectTypes.UserMode> modeCombo;
    private final JComboBox<ProjectTypes.SchedulerPolicy> policyCombo;
    private final JComboBox<ProjectTypes.HeadDirection> directionCombo;
    private final JLabel selectionLabel;
    private final JLabel statusLabel;

    public MainFrame(SimulationEngine engine, SimulatorStateRepository repository) {
        super("Proyecto 2 - Simulador de Sistema de Archivos Concurrente");
        this.engine = engine;
        this.repository = repository;

        tree = new JTree();
        diskPanel = new DiskPanel();
        fatTable = createTable();
        processTable = createTable();
        lockTable = createTable();
        journalTable = createTable();
        eventLogArea = new JTextArea();

        pathField = new JTextField("/home", 20);
        nameField = new JTextField(12);
        renameField = new JTextField(12);
        ownerField = new JTextField("admin", 10);
        publicCheckBox = new JCheckBox("Publico", true);
        sizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 200, 1));
        headSpinner = new JSpinner(new SpinnerNumberModel(50, 0, engine.getBlockCount() - 1, 1));
        preferredBlockSpinner = new JSpinner(new SpinnerNumberModel(50, 0, engine.getBlockCount() - 1, 1));
        modeCombo = new JComboBox<>(ProjectTypes.UserMode.values());
        policyCombo = new JComboBox<>(ProjectTypes.SchedulerPolicy.values());
        directionCombo = new JComboBox<>(ProjectTypes.HeadDirection.values());
        selectionLabel = new JLabel("Seleccione un archivo o directorio");
        statusLabel = new JLabel();

        buildUi();
        bindEvents();
        refreshAll();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(APP_BG);
        setJMenuBar(buildMenuBar());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        topPanel.setOpaque(false);
        topPanel.add(buildControlsPanel(), BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        topPanel.add(statusLabel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setBorder(createCardBorder("Sistema de Archivos"));
        leftPanel.setBackground(CARD_BG);
        leftPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        leftPanel.add(selectionLabel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
        centerPanel.setBorder(createCardBorder("Disk Visualizacion"));
        centerPanel.setBackground(CARD_BG);
        centerPanel.add(diskPanel, BorderLayout.CENTER);

        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.setBackground(CARD_BG);
        rightTabs.setForeground(TEXT_COLOR);
        rightTabs.addTab("Tabla de Asignacion", wrapTable("Tabla de Asignacion", fatTable));
        rightTabs.addTab("Procesos", wrapTable("Procesos", processTable));
        rightTabs.addTab("Locks", wrapTable("Locks", lockTable));
        rightTabs.addTab("Journal", wrapTable("Journal", journalTable));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        splitPane.setResizeWeight(0.28);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setContinuousLayout(true);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane, rightTabs);
        mainSplit.setResizeWeight(0.67);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);

        eventLogArea.setEditable(false);
        eventLogArea.setRows(8);
        add(wrapComponent("Log del Sistema", new JScrollPane(eventLogArea)), BorderLayout.SOUTH);

        applyTheme();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int initialWidth = Math.min(1500, Math.max(1100, screen.width - 80));
        int initialHeight = Math.min(900, Math.max(700, screen.height - 120));
        setPreferredSize(new Dimension(initialWidth, initialHeight));
        setMinimumSize(new Dimension(Math.min(950, initialWidth), Math.min(650, initialHeight)));

        pack();
        setSize(new Dimension(Math.min(getWidth(), screen.width - 40), Math.min(getHeight(), screen.height - 60)));
        setLocationRelativeTo(null);
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 6, 6));
        panel.setBorder(createCardBorder("Controles"));
        panel.setBackground(CARD_BG);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(CARD_BG);
        row1.add(new JLabel("Ruta"));
        row1.add(pathField);
        row1.add(new JLabel("Nombre"));
        row1.add(nameField);
        row1.add(new JLabel("Renombrar a"));
        row1.add(renameField);
        row1.add(new JLabel("Dueno"));
        row1.add(ownerField);
        row1.add(new JLabel("Bloques"));
        row1.add(sizeSpinner);
        row1.add(new JLabel("Bloque preferido"));
        row1.add(preferredBlockSpinner);
        row1.add(publicCheckBox);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(CARD_BG);
        JButton createDirButton = createActionButton("Crear Directorio");
        createDirButton.addActionListener(event -> runAction(() -> engine.enqueueCreateDirectory(pathField.getText().trim(), nameField.getText().trim(), ownerField.getText().trim(), publicCheckBox.isSelected())));
        JButton createFileButton = createActionButton("Crear Archivo");
        createFileButton.addActionListener(event -> runAction(() -> engine.enqueueCreateFile(pathField.getText().trim(), nameField.getText().trim(), (Integer) sizeSpinner.getValue(), ownerField.getText().trim(), publicCheckBox.isSelected(), (Integer) preferredBlockSpinner.getValue())));
        JButton readButton = createActionButton("Leer");
        readButton.addActionListener(event -> runAction(() -> engine.enqueueRead(pathField.getText().trim(), ownerField.getText().trim())));
        JButton renameButton = createActionButton("Renombrar");
        renameButton.addActionListener(event -> runAction(() -> engine.enqueueRename(pathField.getText().trim(), renameField.getText().trim(), ownerField.getText().trim())));
        JButton deleteButton = createActionButton("Eliminar");
        deleteButton.addActionListener(event -> runAction(() -> engine.enqueueDelete(pathField.getText().trim(), ownerField.getText().trim())));
        JButton statsButton = createActionButton("Estadisticas");
        statsButton.addActionListener(event -> showStatistics());
        JButton stepButton = createActionButton("Procesar Siguiente");
        stepButton.addActionListener(event -> runAction(engine::dispatchNextProcess));
        JButton autoButton = createActionButton("Auto On/Off");
        autoButton.addActionListener(event -> runAction(() -> {
            if (engine.isAutoRunning()) {
                engine.stopAutoProcessing();
            } else {
                engine.startAutoProcessing();
            }
        }));
        JButton failButton = createActionButton("Simular Fallo");
        failButton.addActionListener(event -> runAction(engine::armSimulatedFailure));
        JButton recoverButton = createActionButton("Recuperar Journal");
        recoverButton.addActionListener(event -> runAction(engine::recoverPendingJournal));

        row2.add(createDirButton);
        row2.add(createFileButton);
        row2.add(readButton);
        row2.add(renameButton);
        row2.add(deleteButton);
        row2.add(statsButton);
        row2.add(stepButton);
        row2.add(autoButton);
        row2.add(failButton);
        row2.add(recoverButton);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.setBackground(CARD_BG);
        row3.add(new JLabel("Modo"));
        row3.add(modeCombo);
        row3.add(new JLabel("Planificador"));
        row3.add(policyCombo);
        row3.add(new JLabel("Direccion"));
        row3.add(directionCombo);
        row3.add(new JLabel("Cabezal"));
        row3.add(headSpinner);

        JButton applyConfigButton = createActionButton("Aplicar Config");
        applyConfigButton.addActionListener(event -> runAction(() -> {
            ProjectTypes.UserMode selectedMode = (ProjectTypes.UserMode) modeCombo.getSelectedItem();
            if (selectedMode == ProjectTypes.UserMode.ADMIN) {
                engine.setUserMode(ProjectTypes.UserMode.ADMIN);
                engine.setSchedulerPolicy((ProjectTypes.SchedulerPolicy) policyCombo.getSelectedItem());
                engine.setHeadDirection((ProjectTypes.HeadDirection) directionCombo.getSelectedItem());
                engine.setHeadPosition((Integer) headSpinner.getValue());
                return;
            }
            engine.setUserMode(ProjectTypes.UserMode.USER);
        }));

        JButton saveButton = createActionButton("Guardar JSON");
        saveButton.addActionListener(event -> saveState());
        JButton loadButton = createActionButton("Cargar JSON");
        loadButton.addActionListener(event -> loadState());
        JButton sampleButton = createActionButton("Escenario P1");
        sampleButton.addActionListener(event -> loadBundledScenario());
        JButton resetButton = createActionButton("Reset");
        resetButton.addActionListener(event -> runAction(engine::reset));

        row3.add(applyConfigButton);
        row3.add(saveButton);
        row3.add(loadButton);
        row3.add(sampleButton);
        row3.add(resetButton);

        panel.add(row1);
        panel.add(row2);
        panel.add(row3);
        return panel;
    }

    private void bindEvents() {
        tree.addTreeSelectionListener(this::onTreeSelection);
        engine.setOnChange(() -> SwingUtilities.invokeLater(this::refreshAll));
    }

    private void onTreeSelection(TreeSelectionEvent event) {
        TreePath selectionPath = event.getPath();
        if (selectionPath == null) {
            return;
        }
        Object[] nodes = selectionPath.getPath();
        StringBuilder builder = new StringBuilder();
        for (int index = 1; index < nodes.length; index++) {
            String part = nodes[index].toString();
            builder.append('/').append(part);
        }
        String path = builder.isEmpty() ? "/" : builder.toString();
        pathField.setText(path);
        selectionLabel.setText(engine.describeNode(path, currentOwner()));
    }

    private void refreshAll() {
        refreshTree();
        refreshTables();
        refreshDisk();
        refreshLog();
        refreshStatus();
    }

    private void refreshTree() {
        FsNodeSnapshot snapshot = engine.getTreeSnapshotFor(currentOwner());
        DefaultMutableTreeNode rootNode = buildTreeNode(snapshot, true);
        tree.setModel(new DefaultTreeModel(rootNode));
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }
    }

    private DefaultMutableTreeNode buildTreeNode(FsNodeSnapshot snapshot, boolean isRoot) {
        String label = isRoot ? "/" : snapshot.name;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        if (snapshot.children != null) {
            for (FsNodeSnapshot child : snapshot.children) {
                node.add(buildTreeNode(child, false));
            }
        }
        return node;
    }

    private void refreshTables() {
        String owner = currentOwner();
        setTableData(fatTable, new String[]{"Archivo", "Bloques", "Primer Bloque", "Dueno", "Color"}, engine.getFatRowsFor(owner));
        setTableData(processTable, new String[]{"PID", "Operacion", "Ruta", "Pos", "Estado", "Dueno"}, engine.getProcessRowsFor(owner));
        setTableData(lockTable, new String[]{"Ruta", "Modo", "Holders"}, engine.getLockRowsFor(owner));
        setTableData(journalTable, new String[]{"ID", "Operacion", "Ruta", "Estado", "Hora"}, engine.getJournalRowsFor(owner));
    }

    private void refreshDisk() {
        diskPanel.setBlocks(engine.getBlockSnapshotFor(currentOwner()));
    }

    private void refreshLog() {
        StringBuilder builder = new StringBuilder();
        for (String line : engine.getEventLogLinesFor(currentOwner())) {
            builder.append(line).append('\n');
        }
        eventLogArea.setText(builder.toString());
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }

    private void refreshStatus() {
        Block[] blocks = engine.getBlockSnapshotFor(currentOwner());
        int freeBlocks = 0;
        for (Block block : blocks) {
            if (!block.isAllocated()) {
                freeBlocks++;
            }
        }
        modeCombo.setSelectedItem(engine.getUserMode());
        policyCombo.setSelectedItem(engine.getSchedulerPolicy());
        directionCombo.setSelectedItem(engine.getHeadDirection());
        headSpinner.setValue(engine.getHeadPosition());
        statusLabel.setText("Modo=" + engine.getUserMode() + " | Politica=" + engine.getSchedulerPolicy() + " | Cabezal=" + engine.getHeadPosition() + " | Libres=" + freeBlocks + "/" + blocks.length + " | Crash=" + (engine.isCrashed() ? "SI" : "NO") + " | Auto=" + (engine.isAutoRunning() ? "ON" : "OFF"));
    }

    private JTable createTable() {
        JTable table = new JTable();
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        return table;
    }

    private JPanel wrapTable(String title, JTable table) {
        return wrapComponent(title, new JScrollPane(table));
    }

    private JPanel wrapComponent(String title, JScrollPane component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createCardBorder(title));
        panel.setBackground(CARD_BG);
        component.setBorder(BorderFactory.createEmptyBorder());
        component.getViewport().setBackground(CARD_BG);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(CARD_BG);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        JMenu archivoMenu = new JMenu("Archivo");
        archivoMenu.setForeground(TEXT_COLOR);
        archivoMenu.add(createMenuItem("Guardar JSON", this::saveState));
        archivoMenu.add(createMenuItem("Cargar JSON", this::loadState));
        archivoMenu.add(createMenuItem("Reset", () -> runAction(engine::reset)));
        archivoMenu.add(createMenuItem("Salir", this::dispose));

        JMenu reportesMenu = new JMenu("Reportes");
        reportesMenu.setForeground(TEXT_COLOR);
        reportesMenu.add(createMenuItem("Estadisticas", this::showStatistics));
        reportesMenu.add(createMenuItem("Recuperar Journal", () -> runAction(engine::recoverPendingJournal)));

        menuBar.add(archivoMenu);
        menuBar.add(reportesMenu);
        return menuBar;
    }

    private JMenuItem createMenuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> action.run());
        return item;
    }

    private JButton createActionButton(String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setBackground(BUTTON_BG);
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        button.addChangeListener(event -> {
            if (button.getModel().isRollover()) {
                button.setBackground(BUTTON_HOVER);
            } else {
                button.setBackground(BUTTON_BG);
            }
        });
        return button;
    }

    private javax.swing.border.Border createCardBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(8, 8, 8, 8),
                        title,
                        0,
                        0,
                        UI_FONT.deriveFont(Font.BOLD),
                        TEXT_COLOR
                )
        );
    }

    private void applyTheme() {
        applyFontRecursively(getContentPane());

        selectionLabel.setForeground(MUTED_TEXT);
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(CARD_BG);

        styleTextInput(pathField);
        styleTextInput(nameField);
        styleTextInput(renameField);
        styleTextInput(ownerField);

        styleSpinner(sizeSpinner, 72);
        styleSpinner(headSpinner, 72);
        styleSpinner(preferredBlockSpinner, 72);

        styleCombo(modeCombo, 120);
        styleCombo(policyCombo, 140);
        styleCombo(directionCombo, 120);

        publicCheckBox.setOpaque(false);
        publicCheckBox.setForeground(TEXT_COLOR);

        styleTree();
        styleTable(fatTable);
        styleTable(processTable);
        styleTable(lockTable);
        styleTable(journalTable);

        eventLogArea.setBackground(new Color(12, 18, 29));
        eventLogArea.setForeground(new Color(190, 223, 250));
        eventLogArea.setCaretColor(TEXT_COLOR);
        eventLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        eventLogArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void applyFontRecursively(Component component) {
        component.setFont(UI_FONT);
        if (component instanceof JLabel label) {
            label.setForeground(TEXT_COLOR);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyFontRecursively(child);
            }
        }
    }

    private void styleTextInput(JTextField field) {
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 28));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
    }

    private void styleSpinner(JSpinner spinner, int width) {
        spinner.setUI(new BasicSpinnerUI());
        spinner.setPreferredSize(new Dimension(width, 28));
        spinner.setOpaque(true);
        spinner.setBackground(INPUT_BG);
        spinner.setForeground(TEXT_COLOR);
        spinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.setOpaque(true);
        editor.setBackground(SPINNER_VALUE_BG);
        editor.getTextField().setBackground(SPINNER_VALUE_BG);
        editor.getTextField().setForeground(SPINNER_VALUE_TEXT);
        editor.getTextField().setCaretColor(SPINNER_VALUE_TEXT);
        editor.getTextField().setDisabledTextColor(SPINNER_VALUE_TEXT);
        editor.getTextField().setSelectionColor(ACCENT);
        editor.getTextField().setSelectedTextColor(Color.WHITE);
        editor.getTextField().setOpaque(true);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));

        for (Component child : spinner.getComponents()) {
            styleSpinnerChild(child);
        }
    }

    private void styleSpinnerChild(Component component) {
        if (component instanceof JButton button) {
            button.setBackground(new Color(43, 62, 88));
            button.setForeground(TEXT_COLOR);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleSpinnerChild(child);
            }
        }
    }

    private void styleCombo(JComboBox<?> combo, int width) {
        combo.setPreferredSize(new Dimension(width, 28));
        combo.setBackground(INPUT_BG);
        combo.setForeground(TEXT_COLOR);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component cell = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    cell.setBackground(ACCENT);
                    cell.setForeground(Color.WHITE);
                } else {
                    cell.setBackground(index < 0 ? INPUT_BG : CARD_BG);
                    cell.setForeground(TEXT_COLOR);
                }
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return cell;
            }
        });
    }

    private void styleTree() {
        tree.setBackground(new Color(20, 31, 45));
        tree.setForeground(TEXT_COLOR);
        tree.setRowHeight(22);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(20, 31, 45));
        renderer.setTextNonSelectionColor(TEXT_COLOR);
        renderer.setBackgroundSelectionColor(ACCENT);
        renderer.setTextSelectionColor(Color.WHITE);
        tree.setCellRenderer(renderer);
    }

    private void styleTable(JTable table) {
        table.setBackground(new Color(20, 31, 45));
        table.setForeground(TEXT_COLOR);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setBackground(new Color(30, 42, 58));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setOpaque(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setOpaque(true);
        headerRenderer.setBackground(new Color(30, 42, 58));
        headerRenderer.setForeground(TEXT_COLOR);
        headerRenderer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR));
        headerRenderer.setHorizontalAlignment(JLabel.LEFT);
        table.getTableHeader().setDefaultRenderer(headerRenderer);
    }

    private void showStatistics() {
        Block[] blocks = engine.getBlockSnapshotFor(currentOwner());
        int freeBlocks = 0;
        for (Block block : blocks) {
            if (!block.isAllocated()) {
                freeBlocks++;
            }
        }

        String message = "Bloques totales: " + blocks.length
                + "\nBloques libres: " + freeBlocks
                + "\nBloques usados: " + (blocks.length - freeBlocks)
                + "\nModo: " + engine.getUserMode()
                + "\nPlanificador: " + engine.getSchedulerPolicy()
                + "\nCabezal: " + engine.getHeadPosition();
        JOptionPane.showMessageDialog(this, message, "Estadisticas", JOptionPane.INFORMATION_MESSAGE);
    }

    private String currentOwner() {
        String owner = ownerField.getText() == null ? "" : ownerField.getText().trim();
        return owner.isEmpty() ? "user" : owner;
    }

    private void setTableData(JTable table, String[] columns, String[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        styleTable(table);
    }

    private void saveState() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar estado");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            runAction(() -> repository.saveState(chooser.getSelectedFile().toPath(), engine.exportState()));
        }
    }

    private void loadState() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Cargar estado");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            runAction(() -> engine.importState(repository.loadState(chooser.getSelectedFile().toPath())));
        }
    }

    private void loadBundledScenario() {
        Path samplePath = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "samples", "escenario_p1.json");
        runAction(() -> engine.loadScenario(repository.loadScenario(samplePath)));
    }

    private void runAction(Action action) {
        try {
            action.run();
            refreshAll();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Operacion invalida", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }
}
