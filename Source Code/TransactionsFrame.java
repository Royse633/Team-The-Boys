import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class TransactionsFrame {
    private JPanel contentPanel;
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    
    public TransactionsFrame() {
        System.out.println("=== TRANSACTIONS FRAME STARTED ===");
        initializeUI();
    }
    
    private void initializeUI() {
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel("Transaction History");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        headerLabel.setForeground(new Color(0, 82, 165));
        
        // Create tabs for different views
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("All Transactions", createAllTransactionsPanel());
        tabbedPane.addTab("Recent Activity", createRecentActivityPanel());
        tabbedPane.addTab("Transaction Summary", createSummaryPanel());
        
        contentPanel.add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createAllTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setBackground(Color.WHITE);
        
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search by supply name or reason...");
        
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh All");
        
        styleButton(searchButton, new Color(0, 123, 255));
        styleButton(refreshButton, new Color(108, 117, 125));
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        
        // Table with optimized column widths
        String[] columns = {"ID", "Date & Time", "Type", "Supply", "Quantity", "Previous", "Current", "User", "Reason"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4 || columnIndex == 5 || columnIndex == 6) {
                    return Integer.class;
                }
                return String.class;
            }
        };
        
        transactionsTable = new JTable(tableModel);
        transactionsTable.setRowHeight(35);
        transactionsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionsTable.setGridColor(new Color(230, 230, 230));
        transactionsTable.setShowGrid(true);
        transactionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionsTable.setSelectionBackground(new Color(220, 240, 255));
        
        // Hide ID column
        transactionsTable.removeColumn(transactionsTable.getColumnModel().getColumn(0));
        
        // Set optimal column widths
        TableColumnModel columnModel = transactionsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(120);  // Date & Time (index 0 after removing ID)
        columnModel.getColumn(1).setPreferredWidth(80);   // Type
        columnModel.getColumn(2).setPreferredWidth(150);  // Supply
        columnModel.getColumn(3).setPreferredWidth(80);   // Quantity
        columnModel.getColumn(4).setPreferredWidth(70);   // Previous
        columnModel.getColumn(5).setPreferredWidth(70);   // Current
        columnModel.getColumn(6).setPreferredWidth(100);  // User
        columnModel.getColumn(7).setPreferredWidth(300);  // Reason (WIDER for better visibility)
        
        // Custom renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        // Center align numeric columns
        for (int i = 0; i <= 5; i++) { // Columns 0-5 (Date to Current)
            transactionsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Special renderer for Reason column with tooltip and word wrap
        transactionsTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea();
                String reason = value != null ? value.toString() : "";
                textArea.setText(reason);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setFont(table.getFont());
                textArea.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                textArea.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                // Set tooltip to show full text
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setBorder(null);
                scrollPane.setToolTipText(reason);
                
                return scrollPane;
            }
        });
        
        // Set row height for reason column
        transactionsTable.setRowHeight(45); // Increased height for better readability
        
        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Action buttons panel - REMOVED Add Test and Export buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        
        // Only keep Clear All button
        JButton clearButton = new JButton("Clear All");
        styleButton(clearButton, new Color(220, 53, 69));
        
        actionPanel.add(clearButton);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Event handlers
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                searchTransactions(searchTerm);
            } else {
                loadAllTransactions();
            }
        });
        
        refreshButton.addActionListener(e -> {
            loadAllTransactions();
        });
        
        clearButton.addActionListener(e -> {
            clearTransactions();
        });
        
        // Load data initially
        loadAllTransactions();
        
        return panel;
    }
    
    private JPanel createRecentActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Recent Activity (Last 50 Transactions)", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 82, 165));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        String[] columns = {"Time", "Activity", "Details"};
        DefaultTableModel recentModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable recentTable = new JTable(recentModel);
        recentTable.setRowHeight(30);
        recentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Load recent activity
        loadRecentActivity(recentModel);
        
        JScrollPane scrollPane = new JScrollPane(recentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JButton refreshRecentButton = new JButton("Refresh");
        styleButton(refreshRecentButton, new Color(0, 123, 255));
        refreshRecentButton.addActionListener(e -> loadRecentActivity(recentModel));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(refreshRecentButton);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Transaction Summary", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        
        // Create summary statistics
        JPanel summaryPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        summaryPanel.setBackground(Color.WHITE);
        
        // Load summary data
        loadTransactionSummary(summaryPanel);
        
        // Add refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshSummaryButton = new JButton("Refresh Summary");
        styleButton(refreshSummaryButton, new Color(0, 123, 255));
        refreshSummaryButton.addActionListener(e -> {
            summaryPanel.removeAll();
            summaryPanel.setLayout(new GridLayout(3, 2, 15, 15));
            loadTransactionSummary(summaryPanel);
            summaryPanel.revalidate();
            summaryPanel.repaint();
        });
        buttonPanel.add(refreshSummaryButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(summaryPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadAllTransactions() {
        System.out.println("Loading all transactions...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/medical_inventory", "root", "");
                    
                    // Query all transactions
                    String sql = "SELECT t.*, s.name as supply_name " +
                                "FROM transactions t " +
                                "LEFT JOIN supplies s ON t.supply_id = s.id " +
                                "ORDER BY t.transaction_date DESC " +
                                "LIMIT 200";
                    
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                    });
                    
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        
                        // Get data
                        int id = rs.getInt("id");
                        Timestamp date = rs.getTimestamp("transaction_date");
                        String type = rs.getString("transaction_type");
                        int supplyId = rs.getInt("supply_id");
                        String supplyName = rs.getString("supply_name");
                        int qtyChanged = rs.getInt("quantity_changed");
                        int prevQty = rs.getInt("previous_quantity");
                        int newQty = rs.getInt("new_quantity");
                        String user = rs.getString("performed_by");
                        String reason = rs.getString("reason");
                        
                        if (supplyName == null) {
                            supplyName = "Supply #" + supplyId;
                        }
                        
                        // Format date
                        String dateStr = "N/A";
                        if (date != null) {
                            dateStr = date.toLocalDateTime().format(
                                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                        }
                        
                        // Format type with color coding
                        String typeText = type;
                        
                        final Object[] row = {
                            id,
                            dateStr,
                            typeText,
                            supplyName,
                            qtyChanged,
                            prevQty,
                            newQty,
                            (user != null && !user.isEmpty()) ? user : "System",
                            reason != null ? reason : "-"
                        };
                        
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(row);
                        });
                    }
                    
                    System.out.println("✅ Loaded " + count + " transactions");
                    
                    if (count == 0) {
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{
                                "-", "No transactions found", 
                                "", "Add or edit supplies to create transactions", 
                                0, 0, 0, "", ""
                            });
                        });
                    }
                    
                    rs.close();
                    stmt.close();
                    conn.close();
                    
                } catch (SQLException ex) {
                    System.err.println("❌ Database error: " + ex.getMessage());
                    
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        tableModel.addRow(new Object[]{
                            "ERROR", ex.getMessage(), 
                            "", "Check database connection", 0, 0, 0, "", ""
                        });
                    });
                }
                return null;
            }
        };
        
        worker.execute();
    }
    
    private void loadRecentActivity(DefaultTableModel model) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/medical_inventory", "root", "");
                    
                    String sql = "SELECT t.transaction_date, t.transaction_type, " +
                                "t.quantity_changed, s.name as supply_name, t.reason " +
                                "FROM transactions t " +
                                "LEFT JOIN supplies s ON t.supply_id = s.id " +
                                "ORDER BY t.transaction_date DESC " +
                                "LIMIT 50";
                    
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                    });
                    
                    while (rs.next()) {
                        Timestamp date = rs.getTimestamp("transaction_date");
                        String type = rs.getString("transaction_type");
                        int qtyChanged = rs.getInt("quantity_changed");
                        String supplyName = rs.getString("supply_name");
                        String reason = rs.getString("reason");
                        
                        String timeStr = "N/A";
                        if (date != null) {
                            timeStr = date.toLocalDateTime().format(
                                DateTimeFormatter.ofPattern("HH:mm:ss"));
                        }
                        
                        String activity = type + " " + qtyChanged + " units of " + 
                                        (supplyName != null ? supplyName : "supply");
                        
                        String details = reason != null ? reason : "No details";
                        
                        final Object[] row = {timeStr, activity, details};
                        
                        SwingUtilities.invokeLater(() -> {
                            model.addRow(row);
                        });
                    }
                    
                    if (model.getRowCount() == 0) {
                        SwingUtilities.invokeLater(() -> {
                            model.addRow(new Object[]{"No recent activity", "", ""});
                        });
                    }
                    
                    rs.close();
                    stmt.close();
                    conn.close();
                    
                } catch (SQLException ex) {
                    System.err.println("Error loading recent activity: " + ex.getMessage());
                }
                return null;
            }
        };
        
        worker.execute();
    }
    
    private void loadTransactionSummary(JPanel summaryPanel) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/medical_inventory", "root", "");
            
            // Get total transactions count
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM transactions");
            rs.next();
            int totalTransactions = rs.getInt("total");
            
            // Get transactions by type
            rs = stmt.executeQuery("SELECT transaction_type, COUNT(*) as count " +
                                  "FROM transactions GROUP BY transaction_type");
            
            int inCount = 0, outCount = 0, adjustCount = 0;
            while (rs.next()) {
                String type = rs.getString("transaction_type");
                int count = rs.getInt("count");
                switch (type) {
                    case "IN": inCount = count; break;
                    case "OUT": outCount = count; break;
                    case "ADJUST": adjustCount = count; break;
                }
            }
            
            // Get today's transactions
            rs = stmt.executeQuery("SELECT COUNT(*) as today FROM transactions " +
                                  "WHERE DATE(transaction_date) = CURDATE()");
            rs.next();
            int todayCount = rs.getInt("today");
            
            // Get most active user
            rs = stmt.executeQuery("SELECT performed_by, COUNT(*) as count " +
                                  "FROM transactions WHERE performed_by IS NOT NULL " +
                                  "GROUP BY performed_by ORDER BY count DESC LIMIT 1");
            String topUser = "None";
            int userCount = 0;
            if (rs.next()) {
                topUser = rs.getString("performed_by");
                userCount = rs.getInt("count");
            }
            
            conn.close();
            
            // Create stat boxes
            summaryPanel.add(createStatBox("Total Transactions", 
                String.valueOf(totalTransactions), new Color(0, 82, 165)));
            summaryPanel.add(createStatBox("Today's Transactions", 
                String.valueOf(todayCount), new Color(40, 167, 69)));
            summaryPanel.add(createStatBox("Stock-In Transactions", 
                String.valueOf(inCount), new Color(0, 123, 255)));
            summaryPanel.add(createStatBox("Stock-Out Transactions", 
                String.valueOf(outCount), new Color(220, 53, 69)));
            summaryPanel.add(createStatBox("Adjustment Transactions", 
                String.valueOf(adjustCount), new Color(255, 193, 7)));
            summaryPanel.add(createStatBox("Most Active User", 
                topUser + " (" + userCount + ")", new Color(111, 66, 193)));
            
        } catch (SQLException ex) {
            summaryPanel.add(new JLabel("Error loading summary: " + ex.getMessage()));
        }
    }
    
    private JPanel createStatBox(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(15, 10, 15, 10)
        ));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(100, 100, 100));
        
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(color);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void searchTransactions(String searchTerm) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/medical_inventory", "root", "");
                    
                    String sql = "SELECT t.*, s.name as supply_name " +
                                "FROM transactions t " +
                                "LEFT JOIN supplies s ON t.supply_id = s.id " +
                                "WHERE s.name LIKE ? OR t.reason LIKE ? OR t.performed_by LIKE ? " +
                                "ORDER BY t.transaction_date DESC " +
                                "LIMIT 100";
                    
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    String likeTerm = "%" + searchTerm + "%";
                    pstmt.setString(1, likeTerm);
                    pstmt.setString(2, likeTerm);
                    pstmt.setString(3, likeTerm);
                    
                    ResultSet rs = pstmt.executeQuery();
                    
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                    });
                    
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        
                        Timestamp date = rs.getTimestamp("transaction_date");
                        String type = rs.getString("transaction_type");
                        String supplyName = rs.getString("supply_name");
                        int qtyChanged = rs.getInt("quantity_changed");
                        int prevQty = rs.getInt("previous_quantity");
                        int newQty = rs.getInt("new_quantity");
                        String user = rs.getString("performed_by");
                        String reason = rs.getString("reason");
                        
                        String dateStr = "N/A";
                        if (date != null) {
                            dateStr = date.toLocalDateTime().format(
                                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                        }
                        
                        final Object[] row = {
                            rs.getInt("id"),
                            dateStr,
                            type,
                            supplyName != null ? supplyName : "Supply #" + rs.getInt("supply_id"),
                            qtyChanged,
                            prevQty,
                            newQty,
                            user != null ? user : "System",
                            reason != null ? reason : "-"
                        };
                        
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(row);
                        });
                    }
                    
                    System.out.println("Search found " + count + " transactions for: " + searchTerm);
                    
                    if (count == 0) {
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{
                                "-", "No results for: " + searchTerm, 
                                "", "", 0, 0, 0, "", ""
                            });
                        });
                    }
                    
                    rs.close();
                    pstmt.close();
                    conn.close();
                    
                } catch (SQLException ex) {
                    System.err.println("Error searching transactions: " + ex.getMessage());
                }
                return null;
            }
        };
        
        worker.execute();
    }
    
    private void clearTransactions() {
        int confirm = JOptionPane.showConfirmDialog(contentPanel,
            "<html><div style='width:400px;'>" +
            "<h3 style='color: #dc3545;'>⚠️ Delete ALL Transaction Records?</h3>" +
            "<p>This will permanently remove all transaction history from the database.</p>" +
            "<p><b>This action cannot be undone!</b></p>" +
            "<p>Are you sure you want to continue?</p>" +
            "</div></html>",
            "Confirm Clear All Transactions",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/medical_inventory", "root", "");
                
                Statement stmt = conn.createStatement();
                int count = stmt.executeUpdate("DELETE FROM transactions");
                
                JOptionPane.showMessageDialog(contentPanel,
                    "Deleted " + count + " transaction records",
                    "Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh all tabs
                loadAllTransactions();
                
                conn.close();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(contentPanel,
                    "Error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void styleButton(JButton button, Color backgroundColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(backgroundColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
    }
    
    public JPanel getContentPanel() {
        return contentPanel;
    }
}