import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ViewInventoryFrame {
    private JPanel contentPanel;
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private MedicalSupplyDAO medicalSupplyDAO;
    private List<MedicalSupply> currentSupplies;
    
    public ViewInventoryFrame() {
        medicalSupplyDAO = new MedicalSupplyDAO();
        initializeUI();
        checkExpiryAlerts(); // Add this line
    }
    
    private void initializeUI() {
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel("View Inventory");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Create tabs for different views
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("All Inventory", createAllInventoryPanel());
        tabbedPane.addTab("Low Stock Alerts", createLowStockPanel());
        tabbedPane.addTab("Expiry Management", createExpiringPanel()); // Renamed tab
        
        contentPanel.add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
    }
    
    // Add this new method
    private void checkExpiryAlerts() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Check for expired items
                    List<MedicalSupply> expiredItems = medicalSupplyDAO.getExpiredItems();
                    
                    // Check for items expiring soon (next 7 days)
                    List<MedicalSupply> expiringSoon = medicalSupplyDAO.getExpiringInDays(7);
                    
                    // Show alerts if needed
                    if (!expiredItems.isEmpty() || !expiringSoon.isEmpty()) {
                        showExpiryAlert(expiredItems.size(), expiringSoon.size());
                    }
                } catch (Exception e) {
                    System.err.println("Error checking expiry alerts: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }
    
    // Add this method to show expiry alerts
    private void showExpiryAlert(int expiredCount, int expiringSoonCount) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder message = new StringBuilder();
            message.append("⚠️ EXPIRY ALERTS ⚠️\n\n");
            
            if (expiredCount > 0) {
                message.append("❌ ").append(expiredCount).append(" item(s) have EXPIRED!\n");
                message.append("   Please check the 'Expiring Soon' tab and dispose of expired items.\n\n");
            }
            
            if (expiringSoonCount > 0) {
                message.append("⚠ ").append(expiringSoonCount).append(" item(s) expiring in the next 7 days!\n");
                message.append("   Consider using these items first or reordering.\n\n");
            }
            
            message.append("Click OK to continue to inventory management.");
            
            JOptionPane.showMessageDialog(contentPanel,
                message.toString(),
                "Expiry Alert",
                JOptionPane.WARNING_MESSAGE);
        });
    }
    
    private JPanel createAllInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search inventory...");
        
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh All");
        
        searchButton.setBackground(new Color(0, 123, 255));
        refreshButton.setBackground(new Color(108, 117, 125));
        searchButton.setForeground(Color.WHITE);
        refreshButton.setForeground(Color.WHITE);
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        
        // Table
        String[] columns = {"ID", "Item Name", "Category", "Quantity", "Min Stock", "Status", "Expiry Date", "Days Left", "Location", "Supplier"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        inventoryTable = new JTable(tableModel);
        inventoryTable.setRowHeight(30);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Hide ID column by column index, not by name
        inventoryTable.removeColumn(inventoryTable.getColumnModel().getColumn(0));
        
        loadAllInventoryData();
        
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);
        
        JButton addQuantityButton = new JButton("Add Quantity");
        JButton removeQuantityButton = new JButton("Remove Quantity");
        
        addQuantityButton.setBackground(new Color(40, 167, 69));
        removeQuantityButton.setBackground(new Color(220, 53, 69));
        addQuantityButton.setForeground(Color.WHITE);
        removeQuantityButton.setForeground(Color.WHITE);
        
        actionPanel.add(addQuantityButton);
        actionPanel.add(removeQuantityButton);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Event handlers
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                searchInventory(searchTerm);
            } else {
                loadAllInventoryData();
            }
        });
        
        refreshButton.addActionListener(e -> {
            loadAllInventoryData();
        });
        
        addQuantityButton.addActionListener(e -> {
            showQuantityDialog(true);
        });
        
        removeQuantityButton.addActionListener(e -> {
            showQuantityDialog(false);
        });
        
        return panel;
    }
    
    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Low Stock Items - Needs Reordering", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(220, 53, 69));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        String[] columns = {"Item Name", "Category", "Current Qty", "Min Required", "Urgency", "Location"};
        DefaultTableModel lowStockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable lowStockTable = new JTable(lowStockModel);
        lowStockTable.setRowHeight(30);
        lowStockTable.setBackground(new Color(255, 240, 240));
        lowStockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add refresh button for low stock tab
        JPanel lowStockHeaderPanel = new JPanel(new BorderLayout());
        lowStockHeaderPanel.add(titleLabel, BorderLayout.CENTER);
        
        JButton refreshLowStockButton = new JButton("Refresh");
        refreshLowStockButton.setBackground(new Color(0, 123, 255));
        refreshLowStockButton.setForeground(Color.WHITE);
        refreshLowStockButton.addActionListener(e -> loadLowStockData(lowStockModel));
        
        lowStockHeaderPanel.add(refreshLowStockButton, BorderLayout.EAST);
        
        JScrollPane scrollPane = new JScrollPane(lowStockTable);
        
        panel.add(lowStockHeaderPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load data initially
        loadLowStockData(lowStockModel);
        
        return panel;
    }
    
    // Update the createExpiringPanel() method to include expired items
    private JPanel createExpiringPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create a tabbed pane within the expiring panel
        JTabbedPane innerTabbedPane = new JTabbedPane();
        
        // Tab 1: Expiring Soon (next 30 days)
        innerTabbedPane.addTab("Expiring Soon (30 Days)", createExpiringSoonPanel());
        
        // Tab 2: Already Expired
        innerTabbedPane.addTab("Expired Items", createExpiredItemsPanel());
        
        // Tab 3: Expiry Summary
        innerTabbedPane.addTab("Expiry Summary", createExpirySummaryPanel());
        
        panel.add(innerTabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Create panel for expiring soon items
    private JPanel createExpiringSoonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Items Expiring Soon (Next 30 Days)", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(255, 193, 7)); // Yellow for warning
        
        String[] columns = {"Item Name", "Category", "Quantity", "Expiry Date", "Days Left", "Location"};
        DefaultTableModel expiringModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable expiringTable = new JTable(expiringModel);
        expiringTable.setRowHeight(30);
        expiringTable.setBackground(new Color(255, 250, 240));
        
        // Load expiring data
        loadExpiringSoonData(expiringModel);
        
        // Add refresh button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadExpiringSoonData(expiringModel));
        headerPanel.add(refreshButton, BorderLayout.EAST);
        
        JScrollPane scrollPane = new JScrollPane(expiringTable);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Create panel for expired items
    private JPanel createExpiredItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Expired Items - Needs Disposal", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(220, 53, 69)); // Red for danger
        
        String[] columns = {"Item Name", "Category", "Quantity", "Expired Date", "Days Expired", "Location"};
        DefaultTableModel expiredModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable expiredTable = new JTable(expiredModel);
        expiredTable.setRowHeight(30);
        expiredTable.setBackground(new Color(255, 240, 240));
        
        // Load expired data
        loadExpiredData(expiredModel);
        
        // Add action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton disposeAllButton = new JButton("Dispose All Expired");
        JButton refreshButton = new JButton("Refresh");
        
        disposeAllButton.setBackground(new Color(220, 53, 69));
        disposeAllButton.setForeground(Color.WHITE);
        disposeAllButton.addActionListener(e -> disposeAllExpiredItems());
        
        refreshButton.addActionListener(e -> loadExpiredData(expiredModel));
        
        buttonPanel.add(disposeAllButton);
        buttonPanel.add(refreshButton);
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        JScrollPane scrollPane = new JScrollPane(expiredTable);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Create panel for expiry summary
    private JPanel createExpirySummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Expiry Summary", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        // Create summary statistics
        JPanel summaryPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        summaryPanel.setBackground(Color.WHITE);
        
        // Get data
        List<MedicalSupply> expiredItems = medicalSupplyDAO.getExpiredItems();
        List<MedicalSupply> expiring7Days = medicalSupplyDAO.getExpiringInDays(7);
        List<MedicalSupply> expiring30Days = medicalSupplyDAO.getExpiringSoonItems(30);
        List<MedicalSupply> allSupplies = medicalSupplyDAO.getAllSupplies();
        
        int totalItems = allSupplies.size();
        int itemsWithExpiry = 0;
        for (MedicalSupply supply : allSupplies) {
            if (supply.getExpiryDate() != null) {
                itemsWithExpiry++;
            }
        }
        
        // Add statistics
        summaryPanel.add(createStatBox("Total Items", String.valueOf(totalItems), Color.BLUE));
        summaryPanel.add(createStatBox("Items with Expiry", itemsWithExpiry + " (" + 
            (totalItems > 0 ? (itemsWithExpiry * 100 / totalItems) : 0) + "%)", Color.BLUE));
        
        summaryPanel.add(createStatBox("Already Expired", String.valueOf(expiredItems.size()), Color.RED));
        summaryPanel.add(createStatBox("Expiring in 7 Days", String.valueOf(expiring7Days.size()), Color.ORANGE));
        
        summaryPanel.add(createStatBox("Expiring in 30 Days", String.valueOf(expiring30Days.size()), Color.YELLOW));
        summaryPanel.add(createStatBox("No Expiry Date", String.valueOf(totalItems - itemsWithExpiry), Color.GRAY));
        
        // Add recommendation
        JTextArea recommendationArea = new JTextArea();
        recommendationArea.setEditable(false);
        recommendationArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        recommendationArea.setLineWrap(true);
        recommendationArea.setWrapStyleWord(true);
        recommendationArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("📋 RECOMMENDATIONS:\n\n");
        
        if (expiredItems.size() > 0) {
            recommendations.append("• Dispose of ").append(expiredItems.size()).append(" expired items immediately\n");
        }
        if (expiring7Days.size() > 0) {
            recommendations.append("• Use ").append(expiring7Days.size()).append(" items expiring this week first\n");
        }
        if (expiring30Days.size() > expiring7Days.size()) {
            recommendations.append("• Plan for ").append(expiring30Days.size() - expiring7Days.size())
                           .append(" items expiring this month\n");
        }
        if (itemsWithExpiry < totalItems) {
            recommendations.append("• Set expiry dates for ").append(totalItems - itemsWithExpiry)
                           .append(" items without expiry\n");
        }
        
        recommendationArea.setText(recommendations.toString());
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(summaryPanel, BorderLayout.CENTER);
        panel.add(new JScrollPane(recommendationArea), BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Helper method to create stat boxes
    private JPanel createStatBox(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(color, 2));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(color);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadAllInventoryData() {
        try {
            tableModel.setRowCount(0);
            currentSupplies = medicalSupplyDAO.getAllSupplies();
            
            if (currentSupplies == null || currentSupplies.isEmpty()) {
                System.out.println("No supplies found in database.");
                return;
            }
            
            System.out.println("Loading " + currentSupplies.size() + " supplies...");
            
            LocalDate today = LocalDate.now();
            
            for (MedicalSupply supply : currentSupplies) {
                String status = getStockStatus(supply);
                String expiryDateStr = (supply.getExpiryDate() != null) ? 
                    supply.getExpiryDate().toString() : "No Expiry";
                
                long daysLeft = 9999;
                if (supply.getExpiryDate() != null) {
                    daysLeft = ChronoUnit.DAYS.between(today, supply.getExpiryDate());
                }
                
                Object[] row = {
                    supply.getId(),
                    supply.getName(),
                    supply.getCategory(),
                    supply.getQuantity(),
                    supply.getMinStockLevel(),
                    status,
                    expiryDateStr,
                    daysLeft == 9999 ? "No Expiry" : (daysLeft >= 0 ? daysLeft + " days" : "Expired " + (-daysLeft) + " days ago"),
                    supply.getLocation(),
                    supply.getSupplier() != null ? supply.getSupplier() : "N/A"
                };
                tableModel.addRow(row);
            }
            
            System.out.println("Successfully loaded " + currentSupplies.size() + " items.");
        } catch (Exception e) {
            System.err.println("Error loading inventory data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(contentPanel,
                "Error loading inventory data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadLowStockData(DefaultTableModel model) {
        try {
            model.setRowCount(0);
            List<MedicalSupply> lowStockItems = medicalSupplyDAO.getLowStockItems();
            
            if (lowStockItems == null) {
                System.err.println("getLowStockItems() returned null");
                return;
            }
            
            System.out.println("Loading " + lowStockItems.size() + " low stock items...");
            
            for (MedicalSupply supply : lowStockItems) {
                String urgency;
                if (supply.getQuantity() == 0) {
                    urgency = "CRITICAL";
                } else if (supply.getQuantity() <= supply.getMinStockLevel() / 2) {
                    urgency = "HIGH";
                } else {
                    urgency = "MEDIUM";
                }
                
                Object[] row = {
                    supply.getName(),
                    supply.getCategory(),
                    supply.getQuantity(),
                    supply.getMinStockLevel(),
                    urgency,
                    supply.getLocation()
                };
                model.addRow(row);
            }
            
            if (lowStockItems.isEmpty()) {
                Object[] row = {"No low stock items found", "", "", "", "", ""};
                model.addRow(row);
                System.out.println("No low stock items found.");
            } else {
                System.out.println("Successfully loaded " + lowStockItems.size() + " low stock items.");
            }
        } catch (Exception e) {
            System.err.println("Error loading low stock data: " + e.getMessage());
            e.printStackTrace();
            Object[] row = {"Error loading data: " + e.getMessage(), "", "", "", "", ""};
            model.addRow(row);
        }
    }
    
    // Load expiring soon data
    private void loadExpiringSoonData(DefaultTableModel model) {
        try {
            model.setRowCount(0);
            List<MedicalSupply> expiringItems = medicalSupplyDAO.getExpiringSoonItems(30);
            LocalDate today = LocalDate.now();
            
            for (MedicalSupply supply : expiringItems) {
                if (supply.getExpiryDate() == null) continue;
                
                long daysLeft = ChronoUnit.DAYS.between(today, supply.getExpiryDate());
                
                if (daysLeft >= 0) {
                    Object[] row = {
                        supply.getName(),
                        supply.getCategory(),
                        supply.getQuantity(),
                        supply.getExpiryDate().toString(),
                        daysLeft + " days",
                        supply.getLocation()
                    };
                    model.addRow(row);
                }
            }
            
            if (model.getRowCount() == 0) {
                Object[] row = {"No items expiring in the next 30 days", "", "", "", "", ""};
                model.addRow(row);
                System.out.println("No items expiring in the next 30 days.");
            } else {
                System.out.println("Successfully loaded " + model.getRowCount() + " expiring items.");
            }
        } catch (Exception e) {
            System.err.println("Error loading expiring soon data: " + e.getMessage());
        }
    }
    
    // Load expired data
    private void loadExpiredData(DefaultTableModel model) {
        try {
            model.setRowCount(0);
            List<MedicalSupply> expiredItems = medicalSupplyDAO.getExpiredItems();
            LocalDate today = LocalDate.now();
            
            for (MedicalSupply supply : expiredItems) {
                if (supply.getExpiryDate() == null) continue;
                
                long daysExpired = ChronoUnit.DAYS.between(supply.getExpiryDate(), today);
                
                Object[] row = {
                    supply.getName(),
                    supply.getCategory(),
                    supply.getQuantity(),
                    supply.getExpiryDate().toString(),
                    daysExpired + " days",
                    supply.getLocation()
                };
                model.addRow(row);
            }
            
            if (model.getRowCount() == 0) {
                Object[] row = {"No expired items found", "", "", "", "", ""};
                model.addRow(row);
                System.out.println("No expired items found.");
            } else {
                System.out.println("Successfully loaded " + model.getRowCount() + " expired items.");
            }
        } catch (Exception e) {
            System.err.println("Error loading expired data: " + e.getMessage());
        }
    }
    
    private void searchInventory(String searchTerm) {
        try {
            tableModel.setRowCount(0);
            currentSupplies = medicalSupplyDAO.searchSupplies(searchTerm);
            
            if (currentSupplies == null || currentSupplies.isEmpty()) {
                System.out.println("No search results for: " + searchTerm);
                return;
            }
            
            LocalDate today = LocalDate.now();
            
            for (MedicalSupply supply : currentSupplies) {
                String status = getStockStatus(supply);
                String expiryDateStr = (supply.getExpiryDate() != null) ? 
                    supply.getExpiryDate().toString() : "No Expiry";
                
                long daysLeft = 9999;
                if (supply.getExpiryDate() != null) {
                    daysLeft = ChronoUnit.DAYS.between(today, supply.getExpiryDate());
                }
                
                Object[] row = {
                    supply.getId(),
                    supply.getName(),
                    supply.getCategory(),
                    supply.getQuantity(),
                    supply.getMinStockLevel(),
                    status,
                    expiryDateStr,
                    daysLeft == 9999 ? "No Expiry" : (daysLeft >= 0 ? daysLeft + " days" : "Expired " + (-daysLeft) + " days ago"),
                    supply.getLocation(),
                    supply.getSupplier() != null ? supply.getSupplier() : "N/A"
                };
                tableModel.addRow(row);
            }
            
            System.out.println("Search found " + currentSupplies.size() + " items for: " + searchTerm);
        } catch (Exception e) {
            System.err.println("Error searching inventory: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(contentPanel,
                "Error searching inventory: " + e.getMessage(),
                "Search Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String getStockStatus(MedicalSupply supply) {
        if (supply.getQuantity() == 0) {
            return "OUT OF STOCK";
        } else if (supply.getQuantity() <= supply.getMinStockLevel()) {
            return "LOW STOCK";
        } else {
            return "IN STOCK";
        }
    }
    
    private void showQuantityDialog(boolean isAdd) {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(contentPanel,
                "Please select an item from the table first.",
                "No Item Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Convert view row index to model row index
            int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
            
            if (modelRow < currentSupplies.size()) {
                MedicalSupply selectedSupply = currentSupplies.get(modelRow);
                QuantityDialog dialog = new QuantityDialog(selectedSupply, isAdd);
                dialog.setVisible(true);
                if (dialog.isSuccess()) {
                    loadAllInventoryData(); // Refresh the table
                }
            }
        } catch (Exception e) {
            System.err.println("Error showing quantity dialog: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(contentPanel,
                "Error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Dispose all expired items
    private void disposeAllExpiredItems() {
        int confirm = JOptionPane.showConfirmDialog(contentPanel,
            "This will dispose of ALL expired items by setting their quantity to 0.\n" +
            "This action cannot be undone!\n\n" +
            "Do you want to continue?",
            "Confirm Disposal",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<MedicalSupply> expiredItems = medicalSupplyDAO.getExpiredItems();
                int disposedCount = 0;
                
                for (MedicalSupply item : expiredItems) {
                    boolean success = medicalSupplyDAO.disposeExpiredItem(
                        item.getId(), 
                        "Auto-disposed due to expiry"
                    );
                    if (success) disposedCount++;
                }
                
                JOptionPane.showMessageDialog(contentPanel,
                    "Disposed " + disposedCount + " expired items.",
                    "Disposal Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh all tabs
                loadAllInventoryData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(contentPanel,
                    "Error disposing items: " + e.getMessage(),
                    "Disposal Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public JPanel getContentPanel() {
        return contentPanel;
    }
}