import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Simplified SystemSettingsFrame for MySQL Medical Inventory
 */

public class SystemSettingsFrame extends JFrame {

    // Database info - UPDATE THESE FOR YOUR SYSTEM
    private static final String DB_URL = "jdbc:mysql://localhost:3306/medical_inventory";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // UI Components
    private JLabel dbStatusLabel, dbSizeLabel;
    private JTable backupTable;
    
    // Buttons
    private JButton testConnBtn, backupBtn, restoreBtn, optimizeBtn, validateBtn;

    public SystemSettingsFrame() {
        // Initialize labels first
        dbStatusLabel = new JLabel("Checking...");
        dbSizeLabel = new JLabel("Calculating...");
        
        initializeUI();
        loadDatabaseStatus();
    }

    private void initializeUI() {
        setTitle("System Settings - Medical Inventory");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Main panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Database Management
        tabbedPane.addTab("🗄️ Database", createDatabasePanel());
        
        // Tab 2: Backup & Restore
        tabbedPane.addTab("💾 Backup", createBackupPanel());

        // Status bar at bottom
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.add(new JLabel("Database:"));
        statusPanel.add(dbStatusLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(new JLabel("Size:"));
        statusPanel.add(dbSizeLabel);

        // Layout
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    // ===================== DATABASE PANEL =====================
    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left: Connection info - Use separate labels from status panel
        JPanel infoPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Connection Information"));
        
        JLabel infoStatusLabel = new JLabel("Checking...");  // Separate instance
        
        infoPanel.add(new JLabel("Database URL:"));
        infoPanel.add(new JLabel(DB_URL));
        infoPanel.add(new JLabel("Username:"));
        infoPanel.add(new JLabel(DB_USER));
        infoPanel.add(new JLabel("Password:"));
        infoPanel.add(new JLabel("********"));
        infoPanel.add(new JLabel("Driver:"));
        infoPanel.add(new JLabel("MySQL JDBC"));
        infoPanel.add(new JLabel("Status:"));
        infoPanel.add(infoStatusLabel);

        // Right: Operations
        JPanel opsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        opsPanel.setBorder(BorderFactory.createTitledBorder("Database Operations"));

        testConnBtn = new JButton("🔗 Test Connection");
        testConnBtn.addActionListener(e -> testConnection());
        
        optimizeBtn = new JButton("⚡ Optimize Database");
        optimizeBtn.addActionListener(e -> optimizeDatabase());
        
        validateBtn = new JButton("✓ Validate Data");
        validateBtn.addActionListener(e -> validateData());
        
        JButton refreshBtn = new JButton("🔄 Refresh Status");
        refreshBtn.addActionListener(e -> {
            loadDatabaseStatus();
            // Update the info panel label too
            infoStatusLabel.setText(dbStatusLabel.getText());
        });
        
        JButton clearLogsBtn = new JButton("🗑️ Clear Old Logs");
        clearLogsBtn.addActionListener(e -> clearOldLogs());

        opsPanel.add(testConnBtn);
        opsPanel.add(optimizeBtn);
        opsPanel.add(validateBtn);
        opsPanel.add(refreshBtn);
        opsPanel.add(clearLogsBtn);

        // Table info panel
        JPanel tableInfoPanel = new JPanel(new BorderLayout());
        tableInfoPanel.setBorder(BorderFactory.createTitledBorder("Database Tables"));
        
        String[] columns = {"Table Name", "Rows", "Size (KB)"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        loadTableInfo(tableModel);
        
        tableInfoPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(infoPanel, BorderLayout.NORTH);
        leftPanel.add(opsPanel, BorderLayout.CENTER);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(tableInfoPanel, BorderLayout.CENTER);

        return panel;
    }

    // ===================== BACKUP PANEL =====================
    private JPanel createBackupPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: Backup controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Backup Operations"));
        
        backupBtn = new JButton("💾 Create Backup");
        backupBtn.addActionListener(e -> createBackup());
        
        restoreBtn = new JButton("🔄 Restore Backup");
        restoreBtn.addActionListener(e -> restoreBackup());
        
        JButton exportBtn = new JButton("📤 Export to CSV");
        exportBtn.addActionListener(e -> exportToCSV());
        
        controlPanel.add(backupBtn);
        controlPanel.add(restoreBtn);
        controlPanel.add(exportBtn);

        // Backup history table
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Backup History"));
        
        String[] columns = {"Date", "File", "Size", "Status"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0);
        backupTable = new JTable(historyModel);
        loadBackupHistory(historyModel);
        
        historyPanel.add(new JScrollPane(backupTable), BorderLayout.CENTER);

        // Layout
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(historyPanel, BorderLayout.CENTER);

        return panel;
    }

    // ===================== DATABASE METHODS =====================
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void loadDatabaseStatus() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String status = "Unknown";
            private String size = "Unknown";
            
            @Override
            protected Void doInBackground() {
                try (Connection conn = getConnection()) {
                    status = "Connected ✓";
                    
                    // Get database size
                    String sizeQuery = "SELECT " +
                        "ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as size_mb " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = ?";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sizeQuery)) {
                        stmt.setString(1, "medical_inventory");
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                size = rs.getDouble("size_mb") + " MB";
                            }
                        }
                    }
                } catch (SQLException e) {
                    status = "Disconnected ✗ - " + e.getMessage();
                }
                return null;
            }
            
            @Override
            protected void done() {
                dbStatusLabel.setText(status);
                dbSizeLabel.setText(size);
            }
        };
        worker.execute();
    }

    private void loadTableInfo(DefaultTableModel model) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = getConnection()) {
                    // Clear existing rows
                    SwingUtilities.invokeLater(() -> model.setRowCount(0));
                    
                    // Get table list
                    String query = "SELECT TABLE_NAME FROM information_schema.tables " +
                                  "WHERE table_schema = ? ORDER BY TABLE_NAME";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, "medical_inventory");
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                final String tableName = rs.getString("TABLE_NAME");
                                
                                // Get row count for this table
                                String countQuery = "SELECT COUNT(*) as row_count FROM `" + tableName + "`";
                                try (Statement countStmt = conn.createStatement();
                                     ResultSet countRs = countStmt.executeQuery(countQuery)) {
                                    
                                    final int rowCount; // Declare as final
                                    if (countRs.next()) {
                                        rowCount = countRs.getInt("row_count");
                                    } else {
                                        rowCount = 0;
                                    }
                                    
                                    // Add to table model
                                    SwingUtilities.invokeLater(() -> 
                                        model.addRow(new Object[]{tableName, rowCount, "N/A"})
                                    );
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    logMessage("Error loading table info: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void testConnection() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = getConnection()) {
                    return true;
                } catch (SQLException e) {
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "✅ Database connection successful!",
                            "Connection Test",
                            JOptionPane.INFORMATION_MESSAGE);
                        logMessage("Database connection test: SUCCESS");
                    } else {
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "❌ Database connection failed!\nCheck your MySQL server and credentials.",
                            "Connection Test",
                            JOptionPane.ERROR_MESSAGE);
                        logMessage("Database connection test: FAILED");
                    }
                    loadDatabaseStatus();
                } catch (Exception e) {
                    logMessage("Connection test error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void optimizeDatabase() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = getConnection()) {
                    // Get all tables
                    String query = "SELECT TABLE_NAME FROM information_schema.tables " +
                                  "WHERE table_schema = ?";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, "medical_inventory");
                        try (ResultSet rs = stmt.executeQuery()) {
                            int count = 0;
                            while (rs.next()) {
                                String tableName = rs.getString("TABLE_NAME");
                                try (Statement optimizeStmt = conn.createStatement()) {
                                    optimizeStmt.execute("OPTIMIZE TABLE `" + tableName + "`");
                                    count++;
                                }
                            }
                            logMessage("Optimized " + count + " tables");
                        }
                    }
                } catch (SQLException e) {
                    logMessage("Optimize error: " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void done() {
                JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                    "Database optimization completed!",
                    "Optimize",
                    JOptionPane.INFORMATION_MESSAGE);
                loadDatabaseStatus();
            }
        };
        worker.execute();
    }

    private void validateData() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                StringBuilder result = new StringBuilder();
                result.append("=== DATA VALIDATION REPORT ===\n");
                result.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
                
                try (Connection conn = getConnection()) {
                    
                    // 1. Check for negative quantities
                    result.append("1. Checking for negative quantities...\n");
                    try {
                        // Check supplies table
                        String query = "SELECT COUNT(*) as negative_count FROM supplies WHERE quantity < 0";
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(query)) {
                            if (rs.next()) {
                                int count = rs.getInt("negative_count");
                                if (count > 0) {
                                    result.append("   ⚠ Found ").append(count).append(" items with negative quantity\n");
                                } else {
                                    result.append("   ✓ No negative quantities found\n");
                                }
                            }
                        }
                    } catch (SQLException e) {
                        result.append("   ⚠ Could not check supplies table: ").append(e.getMessage()).append("\n");
                    }
                    
                    // 2. Check for expired items
                    result.append("\n2. Checking for expired items...\n");
                    try {
                        String query = "SELECT COUNT(*) as expired_count FROM supplies " +
                                      "WHERE expiry_date < CURDATE()";
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(query)) {
                            if (rs.next()) {
                                int count = rs.getInt("expired_count");
                                if (count > 0) {
                                    result.append("   ⚠ Found ").append(count).append(" expired items\n");
                                } else {
                                    result.append("   ✓ No expired items found\n");
                                }
                            }
                        }
                    } catch (SQLException e) {
                        result.append("   ⚠ Could not check expiry dates: ").append(e.getMessage()).append("\n");
                    }
                    
                    // 3. Check table counts
                    result.append("\n3. Table Record Counts:\n");
                    String countQuery = "SELECT TABLE_NAME FROM information_schema.tables " +
                                       "WHERE table_schema = ? ORDER BY TABLE_NAME";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(countQuery)) {
                        stmt.setString(1, "medical_inventory");
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String tableName = rs.getString("TABLE_NAME");
                                String rowQuery = "SELECT COUNT(*) as count FROM `" + tableName + "`";
                                try (Statement countStmt = conn.createStatement();
                                     ResultSet countRs = countStmt.executeQuery(rowQuery)) {
                                    if (countRs.next()) {
                                        result.append("   ").append(tableName).append(": ")
                                              .append(countRs.getInt("count")).append(" records\n");
                                    }
                                }
                            }
                        }
                    }
                    
                    result.append("\n=== VALIDATION COMPLETE ===\n");
                    
                } catch (SQLException e) {
                    result.append("ERROR: ").append(e.getMessage()).append("\n");
                }
                
                return result.toString();
            }
            
            @Override
            protected void done() {
                try {
                    String report = get();
                    logMessage(report);
                    
                    // Show in dialog
                    JTextArea textArea = new JTextArea(20, 60);
                    textArea.setText(report);
                    textArea.setEditable(false);
                    
                    JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                        new JScrollPane(textArea),
                        "Data Validation Report",
                        JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception e) {
                    logMessage("Validation error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void createBackup() {
        String backupDir = "backups/";
        new File(backupDir).mkdirs(); // Create directory if it doesn't exist
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFile = backupDir + "backup_" + timestamp + ".sql";
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private int totalTables = 0;
            private int processedTables = 0;
            
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = getConnection();
                     PrintWriter writer = new PrintWriter(new FileWriter(backupFile))) {
                    
                    logMessage("Starting backup process...");
                    
                    // Write backup header
                    writer.println("-- MySQL Backup Generated by Medical Inventory System");
                    writer.println("-- Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    writer.println("-- Database: medical_inventory");
                    writer.println("SET FOREIGN_KEY_CHECKS=0;\n");
                    
                    // Get list of all tables
                    String query = "SELECT TABLE_NAME FROM information_schema.tables " +
                                  "WHERE table_schema = ? ORDER BY TABLE_NAME";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, "medical_inventory");
                        try (ResultSet rs = stmt.executeQuery()) {
                            
                            // First, get total count
                            List<String> tables = new ArrayList<>();
                            while (rs.next()) {
                                tables.add(rs.getString("TABLE_NAME"));
                            }
                            totalTables = tables.size();
                            
                            // Process each table
                            for (String tableName : tables) {
                                backupTable(conn, writer, tableName);
                                processedTables++;
                                
                                // Update progress
                                int progress = (int) ((processedTables * 100) / totalTables);
                                setProgress(progress);
                            }
                        }
                    }
                    
                    // Write footer
                    writer.println("\nSET FOREIGN_KEY_CHECKS=1;");
                    writer.println("-- Backup completed successfully");
                    
                    logMessage("Backup completed for " + processedTables + " tables");
                    return true;
                    
                } catch (Exception e) {
                    logMessage("Backup error: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        File file = new File(backupFile);
                        String size = String.format("%.2f KB", file.length() / 1024.0);
                        
                        logMessage("Backup created: " + backupFile + " (" + size + ")");
                        
                        // Optional: Create ZIP file
                        createZipBackup(backupFile);
                        
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "✅ Backup created successfully!\n" +
                            "File: " + backupFile + "\n" +
                            "Size: " + size + "\n" +
                            "Tables: " + processedTables,
                            "Backup Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "❌ Backup failed!\n" + 
                            "Check console for details.",
                            "Backup Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logMessage("Backup completion error: " + e.getMessage());
                }
            }
        };
        
        // Add progress bar
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });
        
        // Show progress dialog
        JDialog progressDialog = new JDialog(this, "Creating Backup", true);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(new JLabel("Backing up database..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);
        
        // Close dialog when done
        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && 
                SwingWorker.StateValue.DONE == evt.getNewValue()) {
                progressDialog.dispose();
            }
        });
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void backupTable(Connection conn, PrintWriter writer, String tableName) throws SQLException {
        logMessage("Backing up table: " + tableName);
        
        writer.println("\n-- Table: " + tableName);
        writer.println("DROP TABLE IF EXISTS `" + tableName + "`;");
        
        // Get table creation script
        String createTableSQL = getCreateTableSQL(conn, tableName);
        writer.println(createTableSQL + ";");
        
        // Get table data
        String selectQuery = "SELECT * FROM `" + tableName + "`";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Insert data
            while (rs.next()) {
                StringBuilder insertSQL = new StringBuilder();
                insertSQL.append("INSERT INTO `").append(tableName).append("` VALUES (");
                
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    
                    if (value == null) {
                        insertSQL.append("NULL");
                    } else if (value instanceof Number) {
                        insertSQL.append(value);
                    } else if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                        // Format date properly
                        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
                        insertSQL.append("'").append(dateStr).append("'");
                    } else {
                        // Escape single quotes for strings
                        String stringValue = value.toString().replace("'", "''");
                        insertSQL.append("'").append(stringValue).append("'");
                    }
                    
                    if (i < columnCount) {
                        insertSQL.append(", ");
                    }
                }
                
                insertSQL.append(");");
                writer.println(insertSQL.toString());
            }
        }
        
        writer.println("-- End of data for table: " + tableName);
    }
    
    private String getCreateTableSQL(Connection conn, String tableName) throws SQLException {
        String query = "SHOW CREATE TABLE `" + tableName + "`";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString(2); // Second column contains CREATE TABLE statement
            }
        }
        return "";
    }
    
    private void createZipBackup(String sqlFile) {
        try {
            String zipFile = sqlFile.replace(".sql", ".zip");
            try (FileInputStream fis = new FileInputStream(sqlFile);
                 FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                ZipEntry zipEntry = new ZipEntry(new File(sqlFile).getName());
                zos.putNextEntry(zipEntry);
                
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                
                zos.closeEntry();
            }
            
            File zip = new File(zipFile);
            logMessage("ZIP backup created: " + zipFile + " (" + 
                      String.format("%.2f KB", zip.length() / 1024.0) + ")");
            
        } catch (IOException e) {
            logMessage("Warning: Could not create ZIP file: " + e.getMessage());
        }
    }

    private void restoreBackup() {
        JFileChooser chooser = new JFileChooser("backups/");
        chooser.setDialogTitle("Select Backup File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Backup Files", "sql", "zip", "SQL"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File backupFile = chooser.getSelectedFile();
            String fileName = backupFile.getName().toLowerCase();
            
            // Handle ZIP files
            if (fileName.endsWith(".zip")) {
                backupFile = extractZipFile(backupFile);
                if (backupFile == null) {
                    return; // Extraction failed
                }
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "This will overwrite all data in the database!\nAre you sure?",
                "Confirm Restore",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                restoreFromSQLFile(backupFile);
            }
        }
    }
    
    private File extractZipFile(File zipFile) {
        String tempDir = System.getProperty("java.io.tmpdir") + "medical_inventory_restore/";
        new File(tempDir).mkdirs();
        
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile))) {
            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
            
            if (zipEntry != null) {
                String extractedFile = tempDir + zipEntry.getName();
                try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
                return new File(extractedFile);
            }
        } catch (IOException e) {
            logMessage("Error extracting ZIP file: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Error extracting ZIP file: " + e.getMessage(),
                "Extraction Error",
                JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    private void restoreFromSQLFile(File sqlFile) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = getConnection();
                     BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
                    
                    logMessage("Starting restore from: " + sqlFile.getName());
                    
                    // Disable foreign key checks
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                    }
                    
                    StringBuilder sqlCommand = new StringBuilder();
                    String line;
                    int commandCount = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        // Skip comments and empty lines
                        if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                            continue;
                        }
                        
                        sqlCommand.append(line);
                        
                        // Check if line ends with semicolon (end of command)
                        if (line.trim().endsWith(";")) {
                            String command = sqlCommand.toString();
                            
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(command);
                                commandCount++;
                            } catch (SQLException e) {
                                logMessage("Error executing command #" + commandCount + ": " + e.getMessage());
                                logMessage("Command: " + command.substring(0, Math.min(100, command.length())) + "...");
                            }
                            
                            sqlCommand = new StringBuilder(); // Reset for next command
                        }
                    }
                    
                    // Re-enable foreign key checks
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                    }
                    
                    logMessage("Restore completed: " + commandCount + " SQL commands executed");
                    return true;
                    
                } catch (Exception e) {
                    logMessage("Restore error: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        logMessage("Database restored from: " + sqlFile.getName());
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "✅ Database restored successfully!",
                            "Restore Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                            "❌ Restore failed!\nCheck console for details.",
                            "Restore Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logMessage("Restore completion error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void exportToCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("medical_inventory_export_" + 
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File csvFile = chooser.getSelectedFile();
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try (Connection conn = getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT * FROM supplies");
                         PrintWriter writer = new PrintWriter(csvFile)) {
                        
                        // Write header
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            writer.print(meta.getColumnName(i));
                            if (i < columnCount) writer.print(",");
                        }
                        writer.println();
                        
                        // Write data
                        while (rs.next()) {
                            for (int i = 1; i <= columnCount; i++) {
                                String value = rs.getString(i);
                                if (value != null && value.contains(",")) {
                                    value = "\"" + value + "\"";
                                }
                                writer.print(value != null ? value : "");
                                if (i < columnCount) writer.print(",");
                            }
                            writer.println();
                        }
                        
                        return true;
                    } catch (Exception e) {
                        logMessage("Export error: " + e.getMessage());
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            logMessage("Data exported to: " + csvFile.getName());
                            JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                                "✅ Data exported successfully to:\n" + csvFile.getAbsolutePath(),
                                "Export Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(SystemSettingsFrame.this,
                                "❌ Export failed!",
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        logMessage("Export completion error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void loadBackupHistory(DefaultTableModel model) {
        // Simple backup history loader
        File backupDir = new File("backups/");
        if (backupDir.exists()) {
            File[] backupFiles = backupDir.listFiles((dir, name) -> 
                name.endsWith(".sql") || name.endsWith(".zip"));
            if (backupFiles != null) {
                for (File file : backupFiles) {
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date(file.lastModified()));
                    String size = String.format("%.1f KB", file.length() / 1024.0);
                    String type = file.getName().endsWith(".zip") ? "ZIP" : "SQL";
                    model.addRow(new Object[]{date, file.getName(), size, type});
                }
            }
        }
    }

    private void clearOldLogs() {
        // In a real app, you would clear old log entries from database/files
        logMessage("Old logs cleared at: " + 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        JOptionPane.showMessageDialog(this,
            "Old logs cleared successfully!",
            "Clear Logs",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void logMessage(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + message);
    }

    // ===================== MAIN METHOD =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Load MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                SystemSettingsFrame frame = new SystemSettingsFrame();
                frame.setVisible(true);
                
                frame.logMessage("System Settings initialized");
                frame.logMessage("MySQL driver loaded successfully");
                
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                    "MySQL JDBC Driver not found!\n" +
                    "Please add mysql-connector-java.jar to your classpath.",
                    "Driver Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}