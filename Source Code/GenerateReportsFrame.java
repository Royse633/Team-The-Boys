import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public class GenerateReportsFrame {
    private JPanel contentPanel;
    private MedicalSupplyDAO medicalSupplyDAO;
    private String currentUser = "admin"; // Change this based on logged in user

    public GenerateReportsFrame() {
        medicalSupplyDAO = new MedicalSupplyDAO();
        initializeUI();
    }

    private void initializeUI() {
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);

        JLabel headerLabel = new JLabel("Generate Reports");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Create tabs for different report functions
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Generate New Report
        tabbedPane.addTab("📊 Generate Report", createReportPanel());
        
        // Tab 2: View Report History
        tabbedPane.addTab("📋 Report History", createHistoryPanel());

        contentPanel.add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        
        JPanel createReportPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        createReportPanel.setBorder(BorderFactory.createTitledBorder("Create New Report"));
        createReportPanel.setBackground(Color.WHITE);

        JLabel typeLabel = new JLabel("Report Type:");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                "Select type...",
                "Inventory Summary",
                "Low Stock Report",
                "Expiry Report",
                "Category Summary"
        });

        JLabel dateLabel = new JLabel("Date Range:");
        JComboBox<String> dateCombo = new JComboBox<>(new String[]{
                "Select range...",
                "Last 7 days",
                "Last 30 days",
                "Last 90 days",
                "This Year",
                "All Time"
        });

        JButton generateButton = new JButton("Generate Excel Report");
        generateButton.setBackground(new Color(0, 123, 255));
        generateButton.setForeground(Color.WHITE);
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

        createReportPanel.add(typeLabel);
        createReportPanel.add(typeCombo);
        createReportPanel.add(dateLabel);
        createReportPanel.add(dateCombo);
        createReportPanel.add(new JLabel()); // spacer
        createReportPanel.add(generateButton);

        // Report preview area
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Report Preview"));
        previewPanel.setBackground(Color.WHITE);

        JTextArea previewArea = new JTextArea(15, 50);
        previewArea.setEditable(false);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setText("Report preview will appear here...\n\nSelect report type and click 'Generate Excel Report'.");

        JScrollPane scrollPane = new JScrollPane(previewArea);
        previewPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(createReportPanel, BorderLayout.NORTH);
        panel.add(previewPanel, BorderLayout.CENTER);

        // Button action
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String reportType = (String) typeCombo.getSelectedItem();
                String dateRange = (String) dateCombo.getSelectedItem();

                if ("Select type...".equals(reportType) || "Select range...".equals(dateRange)) {
                    JOptionPane.showMessageDialog(contentPanel,
                            "Please select both report type and date range",
                            "Incomplete Selection",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    // Generate preview text
                    String reportContent = generateReportContent(reportType, dateRange);
                    previewArea.setText(reportContent);

                    // Ask user where to save
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save Excel Report");
                    fileChooser.setSelectedFile(new java.io.File(
                        reportType.toLowerCase().replace(" ", "_") + "_" + 
                        LocalDate.now().toString() + ".csv"));
                    
                    int userSelection = fileChooser.showSaveDialog(contentPanel);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        java.io.File fileToSave = fileChooser.getSelectedFile();
                        try {
                            exportToCSV(reportContent, fileToSave.getAbsolutePath());
                            
                            // Save report record to database
                            boolean saved = medicalSupplyDAO.saveReport(
                                reportType, 
                                dateRange, 
                                currentUser, 
                                fileToSave.getAbsolutePath()
                            );
                            
                            if (saved) {
                                JOptionPane.showMessageDialog(contentPanel,
                                        "✅ Report generated and saved to database!\n\n" +
                                        "File: " + fileToSave.getAbsolutePath() + "\n" +
                                        "Type: " + reportType + "\n" +
                                        "Range: " + dateRange,
                                        "Report Success",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(contentPanel,
                                        "⚠ Report exported but not saved to database log.",
                                        "Warning",
                                        JOptionPane.WARNING_MESSAGE);
                            }
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(contentPanel,
                                    "Error exporting report: " + ex.getMessage(),
                                    "Export Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createTitledBorder("Report History"));
        headerPanel.setBackground(Color.WHITE);
        
        JLabel historyLabel = new JLabel("Previously Generated Reports");
        historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(new Color(108, 117, 125));
        refreshButton.setForeground(Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(refreshButton);
        
        headerPanel.add(historyLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        
        // Create table for report history
        String[] columns = {"ID", "Report Type", "Date", "Generated By", "File Path", "Created"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(30);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add popup menu for actions
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openMenuItem = new JMenuItem("📂 Open File");
        JMenuItem deleteMenuItem = new JMenuItem("🗑️ Delete Record");
        
        popupMenu.add(openMenuItem);
        popupMenu.add(deleteMenuItem);
        
        historyTable.setComponentPopupMenu(popupMenu);
        
        // Load initial data
        loadReportHistory(historyModel);
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Button actions
        refreshButton.addActionListener(e -> loadReportHistory(historyModel));
        
        openMenuItem.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow >= 0) {
                String filePath = (String) historyModel.getValueAt(selectedRow, 4);
                if (filePath != null && !filePath.isEmpty()) {
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel,
                            "Cannot open file: " + ex.getMessage(),
                            "File Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        deleteMenuItem.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow >= 0) {
                int reportId = (int) historyModel.getValueAt(selectedRow, 0);
                String reportType = (String) historyModel.getValueAt(selectedRow, 1);
                
                int confirm = JOptionPane.showConfirmDialog(panel,
                    "Delete report '" + reportType + "'?\n\n" +
                    "Note: This only removes the database record,\n" +
                    "not the actual file.",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = medicalSupplyDAO.deleteReport(reportId);
                    if (success) {
                        JOptionPane.showMessageDialog(panel,
                            "Report record deleted successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadReportHistory(historyModel);
                    } else {
                        JOptionPane.showMessageDialog(panel,
                            "Failed to delete report record.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        return panel;
    }
    
    private void loadReportHistory(DefaultTableModel model) {
        model.setRowCount(0); // Clear existing rows
        
        List<Report> reports = medicalSupplyDAO.getAllReports();
        
        if (reports.isEmpty()) {
            model.addRow(new Object[]{"", "No reports found", "", "", "", ""});
        } else {
            for (Report report : reports) {
                model.addRow(new Object[]{
                    report.getId(),
                    report.getReportType(),
                    report.getReportDate() != null ? report.getReportDate().toString() : "N/A",
                    report.getGeneratedBy(),
                    report.getFilePath(),
                    report.getCreatedAt() != null ? report.getCreatedAt().toString() : "N/A"
                });
            }
        }
    }

    private String generateReportContent(String reportType, String dateRange) {
        StringBuilder report = new StringBuilder();
        report.append("MEDICAL INVENTORY MANAGEMENT SYSTEM\n");
        report.append("====================================\n");
        report.append("Report Type: ").append(reportType).append("\n");
        report.append("Date Range: ").append(dateRange).append("\n");
        report.append("Generated: ").append(LocalDate.now().toString()).append("\n");
        report.append("Generated By: ").append(currentUser).append("\n\n");

        switch (reportType) {
            case "Inventory Summary":
                appendInventorySummary(report);
                break;
            case "Low Stock Report":
                appendLowStockReport(report);
                break;
            case "Expiry Report":
                appendExpiryReport(report);
                break;
            case "Category Summary":
                appendCategorySummary(report);
                break;
        }

        report.append("\n=== END OF REPORT ===\n");
        return report.toString();
    }

    // --- Methods to append data ---
    private void appendInventorySummary(StringBuilder report) {
        List<MedicalSupply> allSupplies = medicalSupplyDAO.getAllSupplies();
        report.append("Inventory Summary\n");
        report.append("Item Name,Category,Quantity,Expiry,Status\n");

        for (MedicalSupply s : allSupplies) {
            String status = s.getQuantity() == 0 ? "OUT OF STOCK" :
                    s.getQuantity() <= s.getMinStockLevel() ? "LOW STOCK" : "IN STOCK";
            report.append(String.format("%s,%s,%d,%s,%s\n",
                    s.getName(), s.getCategory(), s.getQuantity(), 
                    s.getExpiryDate() != null ? s.getExpiryDate().toString() : "N/A", 
                    status));
        }
    }

    private void appendLowStockReport(StringBuilder report) {
        List<MedicalSupply> lowStockItems = medicalSupplyDAO.getLowStockItems();
        report.append("Low Stock Report\n");
        report.append("Item Name,Category,Quantity,Min Stock Level,Urgency\n");

        for (MedicalSupply s : lowStockItems) {
            String urgency = s.getQuantity() == 0 ? "CRITICAL" :
                    s.getQuantity() <= s.getMinStockLevel() / 2 ? "HIGH" : "MEDIUM";
            report.append(String.format("%s,%s,%d,%d,%s\n",
                    s.getName(), s.getCategory(), s.getQuantity(), s.getMinStockLevel(), urgency));
        }
    }

    private void appendExpiryReport(StringBuilder report) {
        List<MedicalSupply> expiringItems = medicalSupplyDAO.getExpiringSoonItems(90);
        report.append("Expiry Report (Next 90 Days)\n");
        report.append("Item Name,Category,Quantity,Expiry,Days Left\n");

        LocalDate now = LocalDate.now();
        for (MedicalSupply s : expiringItems) {
            if (s.getExpiryDate() != null) {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, s.getExpiryDate());
                report.append(String.format("%s,%s,%d,%s,%d\n",
                        s.getName(), s.getCategory(), s.getQuantity(), s.getExpiryDate(), daysLeft));
            }
        }
    }

    private void appendCategorySummary(StringBuilder report) {
        List<MedicalSupply> allSupplies = medicalSupplyDAO.getAllSupplies();
        report.append("Category Summary\n");
        report.append("Category,Item Count,Total Quantity\n");

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        java.util.Map<String, Integer> quantities = new java.util.HashMap<>();

        for (MedicalSupply s : allSupplies) {
            counts.put(s.getCategory(), counts.getOrDefault(s.getCategory(), 0) + 1);
            quantities.put(s.getCategory(), quantities.getOrDefault(s.getCategory(), 0) + s.getQuantity());
        }

        for (String cat : counts.keySet()) {
            report.append(String.format("%s,%d,%d\n", cat, counts.get(cat), quantities.get(cat)));
        }
    }

    // --- Export to CSV ---
    private void exportToCSV(String content, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}