import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
public class SmartTaskSchedular extends JFrame {

    private final String FILE_NAME = "tasks.txt";

    private ArrayList<Task> taskList = new ArrayList<>();
    private HashMap<Integer, Task> taskMap = new HashMap<>();

    private PriorityQueue<Task> priorityQueue = new PriorityQueue<>(
            Comparator.comparingInt((Task t) -> t.priority)
                    .thenComparing(t -> t.deadline)
    );

    private Stack<Task> deletedStack = new Stack<>();
    private Queue<Task> completedQueue = new LinkedList<>();

    private JTextField titleField, categoryField, deadlineField, searchField;
    private JComboBox<Integer> priorityBox;
    private JTable taskTable;
    private DefaultTableModel tableModel;

    private int taskIdCounter = 1;

    public SmartTaskSchedular() {
        setTitle("Smart Task Scheduler System - DSA Project");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createInputPanel();
        createTable();
        createButtonPanel();

        loadTasksFromFile();
        refreshTable(taskList);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveTasksToFile();
                dispose();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void createInputPanel() {
        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Task"));

        titleField = new JTextField();
        categoryField = new JTextField();
        deadlineField = new JTextField();
        priorityBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});

        inputPanel.add(new JLabel("Task Title:"));
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(new JLabel("Priority 1-5:"));
        inputPanel.add(new JLabel("Deadline YYYY-MM-DD:"));
        inputPanel.add(new JLabel("Search by ID:"));

        inputPanel.add(titleField);
        inputPanel.add(categoryField);
        inputPanel.add(priorityBox);
        inputPanel.add(deadlineField);

        searchField = new JTextField();
        inputPanel.add(searchField);

        add(inputPanel, BorderLayout.NORTH);
    }

    private void createTable() {
        String[] columns = {"ID", "Title", "Category", "Priority", "Deadline", "Status"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(taskTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Operations"));

        JButton addButton = new JButton("Add Task");
        JButton searchButton = new JButton("Search Task");
        JButton deleteButton = new JButton("Delete Task");
        JButton undoButton = new JButton("Undo Delete");
        JButton completeButton = new JButton("Mark Completed");
        JButton urgentButton = new JButton("Next Urgent Task");
        JButton sortButton = new JButton("Sort by Deadline");
        JButton showAllButton = new JButton("Show All");

        buttonPanel.add(addButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(urgentButton);
        buttonPanel.add(sortButton);
        buttonPanel.add(showAllButton);

        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addTask());
        searchButton.addActionListener(e -> searchTask());
        deleteButton.addActionListener(e -> deleteTask());
        undoButton.addActionListener(e -> undoDelete());
        completeButton.addActionListener(e -> markCompleted());
        urgentButton.addActionListener(e -> showNextUrgentTask());
        sortButton.addActionListener(e -> sortByDeadline());
        showAllButton.addActionListener(e -> refreshTable(taskList));
    }

    private void addTask() {
        String title = titleField.getText().trim();
        String category = categoryField.getText().trim();
        String deadlineText = deadlineField.getText().trim();
        int priority = (int) priorityBox.getSelectedItem();

        if (title.isEmpty() || category.isEmpty() || deadlineText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try {
            LocalDate deadline = LocalDate.parse(deadlineText);

            Task task = new Task(taskIdCounter, title, category, priority, deadline);

            taskList.add(task);
            taskMap.put(task.id, task);
            priorityQueue.add(task);

            taskIdCounter++;

            saveTasksToFile();
            clearFields();
            refreshTable(taskList);

            JOptionPane.showMessageDialog(this, "Task added and saved successfully.");

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
        }
    }

    private void searchTask() {
        try {
            int id = Integer.parseInt(searchField.getText().trim());

            Task task = taskMap.get(id);

            if (task == null) {
                JOptionPane.showMessageDialog(this, "Task not found.");
                return;
            }

            ArrayList<Task> result = new ArrayList<>();
            result.add(task);
            refreshTable(result);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid task ID.");
        }
    }

    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a task from table.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        Task task = taskMap.get(id);

        if (task != null) {
            taskList.remove(task);
            taskMap.remove(id);
            priorityQueue.remove(task);
            completedQueue.remove(task);
            deletedStack.push(task);

            saveTasksToFile();
            refreshTable(taskList);

            JOptionPane.showMessageDialog(this, "Task deleted and file updated successfully.");
        }
    }

    private void undoDelete() {
        if (deletedStack.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No deleted task available.");
            return;
        }

        Task task = deletedStack.pop();

        taskList.add(task);
        taskMap.put(task.id, task);

        if (task.status.equals("Pending")) {
            priorityQueue.add(task);
        } else if (task.status.equals("Completed")) {
            completedQueue.add(task);
        }

        saveTasksToFile();
        refreshTable(taskList);

        JOptionPane.showMessageDialog(this, "Deleted task restored and file updated.");
    }

    private void markCompleted() {
        int selectedRow = taskTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a task from table.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        Task task = taskMap.get(id);

        if (task != null) {
            if (task.status.equals("Completed")) {
                JOptionPane.showMessageDialog(this, "This task is already completed.");
                return;
            }

            task.status = "Completed";
            priorityQueue.remove(task);
            completedQueue.add(task);

            saveTasksToFile();
            refreshTable(taskList);

            JOptionPane.showMessageDialog(this, "Task marked as completed and saved.");
        }
    }

    private void showNextUrgentTask() {
        while (!priorityQueue.isEmpty() && priorityQueue.peek().status.equals("Completed")) {
            priorityQueue.poll();
        }

        if (priorityQueue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No pending urgent task available.");
            return;
        }

        Task urgent = priorityQueue.peek();

        JOptionPane.showMessageDialog(this,
                "Next Urgent Task:\n\n" +
                        "ID: " + urgent.id + "\n" +
                        "Title: " + urgent.title + "\n" +
                        "Category: " + urgent.category + "\n" +
                        "Priority: " + urgent.priority + "\n" +
                        "Deadline: " + urgent.deadline
        );
    }

    private void sortByDeadline() {
        ArrayList<Task> sortedTasks = new ArrayList<>(taskList);

        sortedTasks.sort(Comparator.comparing(t -> t.deadline));

        refreshTable(sortedTasks);
    }

    private void saveTasksToFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME));

            for (Task task : taskList) {
                writer.println(
                        task.id + "," +
                                cleanText(task.title) + "," +
                                cleanText(task.category) + "," +
                                task.priority + "," +
                                task.deadline + "," +
                                task.status
                );
            }

            writer.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving tasks to file.");
        }
    }

    private void loadTasksFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return;
        }

        int maxId = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                if (data.length == 6) {
                    int id = Integer.parseInt(data[0]);
                    String title = data[1];
                    String category = data[2];
                    int priority = Integer.parseInt(data[3]);
                    LocalDate deadline = LocalDate.parse(data[4]);
                    String status = data[5];

                    Task task = new Task(id, title, category, priority, deadline);
                    task.status = status;

                    taskList.add(task);
                    taskMap.put(task.id, task);

                    if (task.status.equals("Pending")) {
                        priorityQueue.add(task);
                    } else if (task.status.equals("Completed")) {
                        completedQueue.add(task);
                    }

                    if (id > maxId) {
                        maxId = id;
                    }
                }
            }

            reader.close();
            taskIdCounter = maxId + 1;

        } catch (IOException | NumberFormatException | DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Error loading tasks from file.");
        }
    }

    private String cleanText(String text) {
        return text.replace(",", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void refreshTable(ArrayList<Task> tasks) {
        tableModel.setRowCount(0);

        for (Task task : tasks) {
            tableModel.addRow(new Object[]{
                    task.id,
                    task.title,
                    task.category,
                    task.priority,
                    task.deadline,
                    task.status
            });
        }
    }

    private void clearFields() {
        titleField.setText("");
        categoryField.setText("");
        deadlineField.setText("");
        searchField.setText("");
        priorityBox.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        new SmartTaskSchedular();
    }
}