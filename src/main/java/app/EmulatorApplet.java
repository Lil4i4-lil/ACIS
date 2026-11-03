package app;

import cpu.CPU;
import cpu.Flag;
import memory.cells.WordCell;
import memory.registers.Register32;
import memory.registers.StatusRegister;
import parsing.IncorrectFormatException;
import parsing.Parser;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Java-апплет эмулятора RISC-процессора.
 * <p>
 * В JDK 8 этот класс можно перевести на {@code javax.swing.JApplet}.
 * Начиная с Java 11 классы апплета удалены из JDK, поэтому здесь используется
 * совместимая оболочка с жизненным циклом апплета ({@code init}/{@code start}/
 * {@code stop}/{@code destroy}) и HTML-страница {@code emulator.html}.
 * На современной JVM апплет также запускается как обычное окно через {@link #main}.
 */
public class EmulatorApplet extends JPanel {
    private static final int APPLET_WIDTH = 821;
    private static final int APPLET_HEIGHT = 600;
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final JRootPane rootPane = new JRootPane();

    private JTextArea codeTextArea;
    private JButton loadIntoMemoryButton;
    private JList<WordCell> memoryListView;
    private JToggleButton supervisorToggleButton;
    private JButton startButton;
    private JButton nextButton;
    private JButton resetButton;
    private JToggleButton trapToggleButton;
    private JList<Register32> intRegsListView;
    private JList<Register32> floatRegsListView;
    private JList<Register32> programCounterListView;
    private JList<StatusRegister> statusListView;

    private DefaultListModel<WordCell> memoryModel;
    private DefaultListModel<Register32> intRegsModel;
    private DefaultListModel<Register32> floatRegsModel;
    private DefaultListModel<Register32> programCounterModel;
    private DefaultListModel<StatusRegister> statusModel;

    private CPU cpu;
    private Parser parser;
    private boolean isProgramEnd;

    public EmulatorApplet() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(APPLET_WIDTH, APPLET_HEIGHT));
        add(rootPane, BorderLayout.CENTER);
    }

    public JRootPane getRootPane() {
        return rootPane;
    }

    public Container getContentPane() {
        return rootPane.getContentPane();
    }

    public String getAppletInfo() {
        return "Модель однокристального RISC-процессора";
    }

    public void init() {
        isProgramEnd = false;
        parser = new Parser();
        cpu = new CPU();

        Container content = getContentPane();
        content.setLayout(new BorderLayout());
        content.add(buildUi(), BorderLayout.CENTER);

        memoryModel = listModel(cpu.memory.asWordList());
        intRegsModel = listModel(cpu.intRegs);
        floatRegsModel = listModel(cpu.floatRegs);
        programCounterModel = new DefaultListModel<>();
        programCounterModel.addElement(cpu.programCounter);
        statusModel = new DefaultListModel<>();
        statusModel.addElement(cpu.statusReg);

        memoryListView.setModel(memoryModel);
        intRegsListView.setModel(intRegsModel);
        floatRegsListView.setModel(floatRegsModel);
        programCounterListView.setModel(programCounterModel);
        statusListView.setModel(statusModel);

        nextButton.setEnabled(false);
        resetButton.setEnabled(false);
        startButton.setEnabled(false);
        trapToggleButton.setSelected(cpu.statusReg.getFlagStatus(Flag.TRAP));
        supervisorToggleButton.setSelected(cpu.statusReg.getFlagStatus(Flag.SUPERVISOR));

        bindActions();
    }

    public void start() {
        // жизненный цикл апплета: страница стала видимой
    }

    public void stop() {
        // жизненный цикл апплета: страница скрыта
    }

    public void destroy() {
        cpu = null;
        parser = null;
    }

    private JPanel buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel left = new JPanel(new BorderLayout(0, 15));
        left.add(buildCodePanel(), BorderLayout.NORTH);
        left.add(buildRegistersPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.add(centeredLabel("Memory"), BorderLayout.NORTH);
        memoryListView = createList();
        memoryListView.setVisibleRowCount(28);
        right.add(new JScrollPane(memoryListView), BorderLayout.CENTER);
        right.setPreferredSize(new Dimension(270, 0));

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        return root;
    }

    private JPanel buildCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(centeredLabel("Code"), BorderLayout.NORTH);

        codeTextArea = new JTextArea(10, 40);
        codeTextArea.setFont(MONO);
        panel.add(new JScrollPane(codeTextArea), BorderLayout.CENTER);
        panel.add(buildButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new GridLayout(1, 0, 12, 0));
        startButton = new JButton("Start");
        loadIntoMemoryButton = new JButton("Load into memory");
        nextButton = new JButton("Next");
        resetButton = new JButton("Reset");
        trapToggleButton = new JToggleButton("Trap");
        supervisorToggleButton = new JToggleButton("Supervisor");

        buttons.add(startButton);
        buttons.add(loadIntoMemoryButton);
        buttons.add(nextButton);
        buttons.add(resetButton);
        buttons.add(trapToggleButton);
        buttons.add(supervisorToggleButton);
        return buttons;
    }

    private JPanel buildRegistersPanel() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.5;

        intRegsListView = createList();
        floatRegsListView = createList();
        programCounterListView = createList();
        statusListView = createList();
        intRegsListView.setVisibleRowCount(8);
        floatRegsListView.setVisibleRowCount(8);
        programCounterListView.setVisibleRowCount(1);
        statusListView.setVisibleRowCount(1);

        c.gridy = 0;
        c.weighty = 0;
        c.gridx = 0;
        grid.add(centeredLabel("Integer registers"), c);
        c.gridx = 1;
        grid.add(centeredLabel("Float registers"), c);

        c.gridy = 1;
        c.weighty = 1;
        c.gridx = 0;
        grid.add(new JScrollPane(intRegsListView), c);
        c.gridx = 1;
        grid.add(new JScrollPane(floatRegsListView), c);

        c.gridy = 2;
        c.weighty = 0;
        c.gridx = 0;
        grid.add(centeredLabel("Program counter"), c);
        c.gridx = 1;
        grid.add(centeredLabel("Status"), c);

        c.gridy = 3;
        c.weighty = 0.15;
        c.gridx = 0;
        grid.add(new JScrollPane(programCounterListView), c);
        c.gridx = 1;
        grid.add(new JScrollPane(statusListView), c);

        c.gridy = 4;
        c.weighty = 0;
        c.gridx = 1;
        grid.add(centeredLabel("–    U    T    I    O    C    S    Z"), c);

        return grid;
    }

    private void bindActions() {
        loadIntoMemoryButton.addActionListener(e -> {
            String[] instructions = codeTextArea.getText().split("\n");
            List<Integer> instructionsData = new ArrayList<>();

            try {
                for (String instruction : instructions) {
                    if (!instruction.isEmpty()) {
                        instructionsData.add(parser.parse(instruction));
                    }
                }

                for (int i = 0; i < instructionsData.size(); ++i) {
                    memoryModel.get(i).setValue(instructionsData.get(i));
                }
            } catch (IncorrectFormatException ex) {
                showAlert(ex.getMessage());
            }

            startButton.setEnabled(!instructionsData.isEmpty());
            refreshAll();
        });

        startButton.addActionListener(e -> {
            try {
                startButton.setEnabled(false);
                loadIntoMemoryButton.setEnabled(false);
                automaticExecution();
            } catch (EmulationAbortException ex) {
                abortEmulation(ex.getMessage());
            }
            refreshAll();
        });

        nextButton.addActionListener(e -> {
            try {
                if (cpu.statusReg.getFlagStatus(Flag.TRAP) && !isProgramEnd) {
                    isProgramEnd = cpu.nextInstruction();
                    resetButton.setEnabled(isProgramEnd);
                    nextButton.setEnabled(!isProgramEnd);
                } else {
                    automaticExecution();
                }
            } catch (EmulationAbortException ex) {
                abortEmulation(ex.getMessage());
            }
            refreshAll();
        });

        resetButton.addActionListener(e -> {
            cpu.clear();
            nextButton.setEnabled(false);
            resetButton.setEnabled(false);
            loadIntoMemoryButton.setEnabled(true);
            trapToggleButton.setSelected(cpu.statusReg.getFlagStatus(Flag.TRAP));
            supervisorToggleButton.setSelected(cpu.statusReg.getFlagStatus(Flag.SUPERVISOR));
            isProgramEnd = false;
            refreshAll();
        });

        trapToggleButton.addActionListener(e -> {
            cpu.statusReg.invertFlagStatus(Flag.TRAP);
            refreshAll();
        });

        supervisorToggleButton.addActionListener(e -> {
            cpu.statusReg.invertFlagStatus(Flag.SUPERVISOR);
            refreshAll();
        });
    }

    private void automaticExecution() throws EmulationAbortException {
        trapToggleButton.setEnabled(false);
        supervisorToggleButton.setEnabled(false);
        nextButton.setEnabled(false);

        while (!cpu.statusReg.getFlagStatus(Flag.TRAP) && !isProgramEnd) {
            isProgramEnd = cpu.nextInstruction();
            resetButton.setEnabled(isProgramEnd);
        }

        nextButton.setEnabled(!isProgramEnd);
        trapToggleButton.setEnabled(true);
        supervisorToggleButton.setEnabled(true);
    }

    private void abortEmulation(String message) {
        showAlert(message);
        cpu.clear();
        nextButton.setEnabled(false);
        resetButton.setEnabled(true);
        isProgramEnd = true;
    }

    private void showAlert(String alertInfo) {
        JOptionPane.showMessageDialog(this, alertInfo, getAppletInfo(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAll() {
        memoryListView.repaint();
        intRegsListView.repaint();
        floatRegsListView.repaint();
        programCounterListView.repaint();
        statusListView.repaint();
    }

    private static JLabel centeredLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static <T> JList<T> createList() {
        JList<T> list = new JList<>();
        list.setFont(MONO);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return list;
    }

    private static <T> DefaultListModel<T> listModel(List<T> items) {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T item : items) {
            model.addElement(item);
        }
        return model;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmulatorApplet applet = new EmulatorApplet();
            applet.init();
            applet.start();

            JFrame frame = new JFrame(applet.getAppletInfo());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(applet);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
