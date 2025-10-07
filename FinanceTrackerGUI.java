import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.io.*;

public class FinanceTrackerGUI extends JFrame {

    private DefaultTableModel tableModel;
    private JLabel totalIncomeLabel, totalExpenseLabel, balanceLabel;

    private final String DB_URL = "jdbc:sqlite:finance.db";

    public FinanceTrackerGUI() {
        setTitle("Personal Finance Tracker with SQLite");
        setSize(750, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Form panel with Add and Export buttons
        JPanel formPanel = new JPanel();
        String[] types = {"Income", "Expense"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        JTextField categoryField = new JTextField(10);
        JTextField amountField = new JTextField(7);
        JButton addButton = new JButton("Add");
        JButton exportButton = new JButton("Export to CSV");

        formPanel.add(new JLabel("Type:"));
        formPanel.add(typeBox);
        formPanel.add(new JLabel("Category:"));
        formPanel.add(categoryField);
        formPanel.add(new JLabel("Amount:"));
        formPanel.add(amountField);
        formPanel.add(addButton);
        formPanel.add(exportButton);

        // Table
        String[] columns = {"Type", "Category", "Amount"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);

        // Totals panel
        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalIncomeLabel = new JLabel("Total Income: 0.00");
        totalExpenseLabel = new JLabel("Total Expense: 0.00");
        balanceLabel = new JLabel("Balance: 0.00");
        totalsPanel.add(totalIncomeLabel);
        totalsPanel.add(Box.createHorizontalStrut(20));
        totalsPanel.add(totalExpenseLabel);
        totalsPanel.add(Box.createHorizontalStrut(20));
        totalsPanel.add(balanceLabel);

        add(formPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(totalsPanel, BorderLayout.SOUTH);

        // Initialize DB and load data
        initDatabase();
        loadTransactionsFromDB();
        updateTotals();

        // Add Button Action
        addButton.addActionListener(e -> {
            try {
                String type = (String) typeBox.getSelectedItem();
                String category = categoryField.getText().trim();
                if (category.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a category.");
                    return;
                }
                double amount = Double.parseDouble(amountField.getText());

                addTransactionToDB(type, category, amount);
                loadTransactionsFromDB(); // reload table data
                updateTotals();

                categoryField.setText("");
                amountField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
            }
        });

        // Export Button Action
        exportButton.addActionListener(e -> exportToCSV());

        setVisible(true);
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "type TEXT NOT NULL," +
                    "category TEXT NOT NULL," +
                    "amount REAL NOT NULL" +
                    ")";
            stmt.execute(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error initializing database: " + e.getMessage());
        }
    }

    private void loadTransactionsFromDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT type, category, amount FROM transactions");
            tableModel.setRowCount(0); // clear existing rows

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("type"));
                row.add(rs.getString("category"));
                row.add(rs.getDouble("amount"));
                tableModel.addRow(row);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading transactions: " + e.getMessage());
        }
    }

    private void addTransactionToDB(String type, String category, double amount) {
        String sql = "INSERT INTO transactions (type, category, amount) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, category);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding transaction: " + e.getMessage());
        }
    }

    private void updateTotals() {
        double totalIncome = 0;
        double totalExpense = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String type = (String) tableModel.getValueAt(i, 0);
            double amount = (Double) tableModel.getValueAt(i, 2);
            if ("Income".equalsIgnoreCase(type)) {
                totalIncome += amount;
            } else if ("Expense".equalsIgnoreCase(type)) {
                totalExpense += amount;
            }
        }
        double balance = totalIncome - totalExpense;

        totalIncomeLabel.setText(String.format("Total Income: %.2f", totalIncome));
        totalExpenseLabel.setText(String.format("Total Expense: %.2f", totalExpense));
        balanceLabel.setText(String.format("Balance: %.2f", balance));
    }

    private void exportToCSV() {
        String fileName = "expenses.csv";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type, category, amount FROM transactions");
             BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            writer.write("Type,Category,Amount");
            writer.newLine();

            while (rs.next()) {
                String type = rs.getString("type");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                writer.write(String.format("%s,%s,%.2f", type, category, amount));
                writer.newLine();
            }

            JOptionPane.showMessageDialog(this, "Exported transactions to " + fileName);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage());
        }
    }
}
