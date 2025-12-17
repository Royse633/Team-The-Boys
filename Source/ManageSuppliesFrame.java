import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ManageSuppliesFrame {
    private JPanel contentPanel;
    private JTable suppliesTable;
    private DefaultTableModel tableModel;
    private MedicalSupplyDAO medicalSupplyDAO;
    private List<MedicalSupply> currentSupplies;
    private JTextField searchField;
    
    public ManageSuppliesFrame() {
        medicalSupplyDAO = new MedicalSupplyDAO();
        initializeUI();
    }
    
    private void initializeUI() {
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        
        JLabel headerLabel = new JLabel("Manage Supplies");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.putClientProperty("JTextField.placeholderText", "Search supplies...");
        
        JButton addButton = new JButton("Add Supply");
        addButton.setBackground(new Color(40, 167, 69));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(108, 117, 125));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        
        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        String[] columns = {"ID", "Supply Name", "Category", "Quantity", "Expiry Date", "Location", "Supplier", "Min Stock", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only actions column is editable
            }
        };
        
        suppliesTable = new JTable(tableModel);
        suppliesTable.setRowHeight(35);
        suppliesTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        suppliesTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        // Hide ID column
        suppliesTable.removeColumn(suppliesTable.getColumn("ID"));
        
        loadSuppliesData();
        
        JScrollPane scrollPane = new JScrollPane(suppliesTable);
        
        contentPanel.add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(topPanel, BorderLayout.CENTER);
        contentPanel.add(scrollPane, BorderLayout.SOUTH);
        
        addButton.addActionListener(e -> {
            AddSupplyDialog dialog = new AddSupplyDialog();
            dialog.setVisible(true);
            if (dialog.isSuccess()) {
                loadSuppliesData(); // Refresh table after adding
            }
        });
        
        refreshButton.addActionListener(e -> {
            loadSuppliesData();
        });
        
        searchField.addActionListener(e -> {
            performSearch();
        });
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);
    }
    
    private void loadSuppliesData() {
        tableModel.setRowCount(0);
        currentSupplies = medicalSupplyDAO.getAllSupplies();
        
        for (MedicalSupply supply : currentSupplies) {
            Object[] row = {
                supply.getId(),
                supply.getName(),
                supply.getCategory(),
                supply.getQuantity(),
                supply.getExpiryDate().toString(),
                supply.getLocation(),
                supply.getSupplier() != null ? supply.getSupplier() : "N/A",
                supply.getMinStockLevel(),
                "Edit/Delete"
            };
            tableModel.addRow(row);
        }
        
        JOptionPane.showMessageDialog(contentPanel, 
            "Loaded " + currentSupplies.size() + " supplies from database.", 
            "Data Loaded", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            tableModel.setRowCount(0);
            currentSupplies = medicalSupplyDAO.searchSupplies(searchTerm);
            
            for (MedicalSupply supply : currentSupplies) {
                Object[] row = {
                    supply.getId(),
                    supply.getName(),
                    supply.getCategory(),
                    supply.getQuantity(),
                    supply.getExpiryDate().toString(),
                    supply.getLocation(),
                    supply.getSupplier() != null ? supply.getSupplier() : "N/A",
                    supply.getMinStockLevel(),
                    "Edit/Delete"
                };
                tableModel.addRow(row);
            }
            
            JOptionPane.showMessageDialog(contentPanel, 
                "Found " + currentSupplies.size() + " supplies matching '" + searchTerm + "'", 
                "Search Results", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            loadSuppliesData();
        }
    }
    
    public JPanel getContentPanel() {
        return contentPanel;
    }
    
    class ButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton editButton;
        private JButton deleteButton;
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);
            
            editButton = new JButton("Edit");
            deleteButton = new JButton("Delete");
            
            editButton.setBackground(new Color(255, 193, 7));
            deleteButton.setBackground(new Color(220, 53, 69));
            editButton.setForeground(Color.BLACK);
            deleteButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            deleteButton.setFocusPainted(false);
            editButton.setPreferredSize(new Dimension(60, 25));
            deleteButton.setPreferredSize(new Dimension(60, 25));
            
            add(editButton);
            add(deleteButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            
            editButton = new JButton("Edit");
            deleteButton = new JButton("Delete");
            
            editButton.setBackground(new Color(255, 193, 7));
            deleteButton.setBackground(new Color(220, 53, 69));
            editButton.setForeground(Color.BLACK);
            deleteButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            deleteButton.setFocusPainted(false);
            editButton.setPreferredSize(new Dimension(60, 25));
            deleteButton.setPreferredSize(new Dimension(60, 25));
            
            editButton.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < currentSupplies.size()) {
                    MedicalSupply supply = currentSupplies.get(currentRow);
                    EditSupplyDialog dialog = new EditSupplyDialog(supply);
                    dialog.setVisible(true);
                    if (dialog.isSuccess()) {
                        loadSuppliesData(); // Refresh table after editing
                    }
                }
            });
            
          // In the ButtonEditor class, update the deleteButton action:
deleteButton.addActionListener(e -> {
    fireEditingStopped();
    if (currentRow >= 0 && currentRow < currentSupplies.size()) {
        MedicalSupply supply = currentSupplies.get(currentRow);
        int confirm = JOptionPane.showConfirmDialog(panel,
            "Are you sure you want to delete '" + supply.getName() + "'?\n" +
            "Quantity: " + supply.getQuantity() + " units\n" +
            "This will record an OUT transaction.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = medicalSupplyDAO.deleteSupply(supply.getId());
            if (success) {
                JOptionPane.showMessageDialog(panel,
                    "✅ Supply '" + supply.getName() + "' deleted successfully!\n" +
                    "Transaction recorded for " + supply.getQuantity() + " units.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                loadSuppliesData(); // Refresh table after deletion
            } else {
                JOptionPane.showMessageDialog(panel,
                    "❌ Failed to delete supply '" + supply.getName() + "'!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
});
            
            panel.add(editButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Edit/Delete";
        }
    }
}