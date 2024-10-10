package com.expensetracker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;
import java.io.FileWriter;
import java.io.IOException;

public class ExpenseTrackerGUI extends JFrame {
    private JTextField categoryField;
    private JTextField amountField;
    private JTextField dateField;
    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;

    // Increased font size
    private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 16);

    public ExpenseTrackerGUI() {
        setTitle("Expense Tracker");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Create input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.fill = GridBagConstraints.HORIZONTAL;

        categoryField = new JTextField(10);
        amountField = new JTextField(10);
        dateField = new JTextField(10);
        JButton addButton = new JButton("Add Expense");
        JButton viewButton = new JButton("View All Expenses");
        JButton searchButton = new JButton("Search by Date");
        JButton saveButton = new JButton("Save to File");

        // Set font for input fields and buttons
        categoryField.setFont(DEFAULT_FONT);
        amountField.setFont(DEFAULT_FONT);
        dateField.setFont(DEFAULT_FONT);
        addButton.setFont(DEFAULT_FONT);
        viewButton.setFont(DEFAULT_FONT);
        searchButton.setFont(DEFAULT_FONT);
        saveButton.setFont(DEFAULT_FONT);

        // Adding components to input panel with GridBagLayout
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputPanel.add(new JLabel("Category:"), inputGbc);

        inputGbc.gridx = 1;
        inputPanel.add(categoryField, inputGbc);

        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputPanel.add(new JLabel("Amount:"), inputGbc);

        inputGbc.gridx = 1;
        inputPanel.add(amountField, inputGbc);

        inputGbc.gridx = 0;
        inputGbc.gridy = 2;
        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"), inputGbc);

        inputGbc.gridx = 1;
        inputPanel.add(dateField, inputGbc);

        inputGbc.gridx = 0;
        inputGbc.gridy = 3;
        inputPanel.add(addButton, inputGbc);

        inputGbc.gridx = 1;
        inputPanel.add(viewButton, inputGbc);

        inputGbc.gridx = 0;
        inputGbc.gridy = 4;
        inputPanel.add(searchButton, inputGbc);

        inputGbc.gridx = 1;
        inputPanel.add(saveButton, inputGbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.2;
        add(inputPanel, gbc);

        // Create table model and table
        tableModel = new DefaultTableModel(new String[]{"ID", "Category", "Amount", "Date"}, 0);
        expenseTable = new JTable(tableModel);
        expenseTable.setFont(DEFAULT_FONT);
        expenseTable.setRowHeight(30); // Set row height for better visibility
        add(new JScrollPane(expenseTable), gbc);

        totalLabel = new JLabel("Total Expenses: $0.00");
        totalLabel.setFont(DEFAULT_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 0.1; // Adjust vertical weight
        add(totalLabel, gbc);

        // Button actions
        addButton.addActionListener(e -> addExpense());
        viewButton.addActionListener(e -> viewAllExpenses());
        searchButton.addActionListener(e -> searchByDate());
        saveButton.addActionListener(e -> saveToFile());

        Database.initializeDatabase(); // Initialize the database
    }

    private void addExpense() {
        String category = categoryField.getText();
        String amountStr = amountField.getText();
        String date = dateField.getText();

        if (category.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            String sql = "INSERT INTO expenses (category, amount, date) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(Database.URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, category);
                pstmt.setDouble(2, amount);
                pstmt.setString(3, date);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Expense added successfully!");
                categoryField.setText("");
                amountField.setText("");
                dateField.setText("");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        viewAllExpenses(); // Refresh the table
    }

    private void viewAllExpenses() {
        tableModel.setRowCount(0); // Clear existing data
        double total = 0;

        String sql = "SELECT * FROM expenses";
        try (Connection conn = DriverManager.getConnection(Database.URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                String date = rs.getString("date");
                tableModel.addRow(new Object[]{id, category, amount, date});
                total += amount; // Calculate total
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        totalLabel.setText(String.format("Total Expenses: $%.2f", total)); // Update total label
    }

    private void searchByDate() {
        String date = dateField.getText();
        if (date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a date.");
            return;
        }

        tableModel.setRowCount(0); // Clear existing data
        double total = 0;

        String sql = "SELECT * FROM expenses WHERE date = ?";
        try (Connection conn = DriverManager.getConnection(Database.URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String category = rs.getString("category");
                    double amount = rs.getDouble("amount");
                    tableModel.addRow(new Object[]{id, category, amount, date});
                    total += amount; // Calculate total
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        totalLabel.setText(String.format("Total Expenses: $%.2f", total)); // Update total label
    }

    private void saveToFile() {
        StringBuilder data = new StringBuilder("ID,Category,Amount,Date\n");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                data.append(tableModel.getValueAt(row, col)).append(",");
            }
            data.deleteCharAt(data.length() - 1); // Remove the last comma
            data.append("\n");
        }

        try (FileWriter writer = new FileWriter("expenses.csv")) {
            writer.write(data.toString());
            JOptionPane.showMessageDialog(this, "Expenses saved to expenses.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseTrackerGUI gui = new ExpenseTrackerGUI();
            gui.setVisible(true);
        });
    }
}
