import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QuantityDialog extends JDialog {
    private boolean success = false;
    private MedicalSupplyDAO medicalSupplyDAO;
    private MedicalSupply supply;
    private boolean isAddOperation;
    
    public QuantityDialog(MedicalSupply supply, boolean isAddOperation) {
        this.supply = supply;
        this.isAddOperation = isAddOperation;
        this.medicalSupplyDAO = new MedicalSupplyDAO();
        initializeUI();
    }
    
    private void initializeUI() {
        String operation = isAddOperation ? "Add Quantity" : "Remove Quantity";
        setTitle(operation);
        setModal(true);
        setSize(450, 300); // Made slightly taller
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(operation, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        
        JLabel itemLabel = new JLabel("Item:");
        JTextField itemField = new JTextField(supply.getName());
        itemField.setEditable(false);
        
        JLabel currentLabel = new JLabel("Current Quantity:");
        JTextField currentField = new JTextField(String.valueOf(supply.getQuantity()));
        currentField.setEditable(false);
        
        JLabel minStockLabel = new JLabel("Min Stock Level:");
        JTextField minStockField = new JTextField(String.valueOf(supply.getMinStockLevel()));
        minStockField.setEditable(false);
        
        JLabel updateLabel = new JLabel(isAddOperation ? "Quantity to Add:" : "Quantity to Remove:");
        JTextField updateField = new JTextField();
        
        JLabel reasonLabel = new JLabel("Reason:");
        JTextField reasonField = new JTextField();
        reasonField.putClientProperty("JTextField.placeholderText", "Enter reason for " + (isAddOperation ? "adding" : "removing"));
        
        formPanel.add(itemLabel);
        formPanel.add(itemField);
        formPanel.add(currentLabel);
        formPanel.add(currentField);
        formPanel.add(minStockLabel);
        formPanel.add(minStockField);
        formPanel.add(updateLabel);
        formPanel.add(updateField);
        formPanel.add(reasonLabel);
        formPanel.add(reasonField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        String actionText = isAddOperation ? "Add Quantity" : "Remove Quantity";
        JButton actionButton = new JButton(actionText);
        JButton cancelButton = new JButton("Cancel");
        
        if (isAddOperation) {
            actionButton.setBackground(new Color(40, 167, 69)); // Green for add
        } else {
            actionButton.setBackground(new Color(255, 193, 7)); // Yellow for remove
        }
        actionButton.setForeground(Color.WHITE);
        actionButton.setFocusPainted(false);
        
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        
        // Add hover effects
        actionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(isAddOperation ? new Color(56, 187, 85) : new Color(255, 203, 47));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(isAddOperation ? new Color(40, 167, 69) : new Color(255, 193, 7));
            }
        });
        
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(128, 137, 145));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(108, 117, 125));
            }
        });
        
        buttonPanel.add(actionButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Add Enter key support
        updateField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionButton.doClick();
            }
        });
        
        reasonField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionButton.doClick();
            }
        });
        
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateInput(updateField, reasonField)) {
                    try {
                        int quantityChange = Integer.parseInt(updateField.getText().trim());
                        int newQuantity;
                        
                        if (isAddOperation) {
                            newQuantity = supply.getQuantity() + quantityChange;
                        } else {
                            newQuantity = supply.getQuantity() - quantityChange;
                            if (newQuantity < 0) {
                                JOptionPane.showMessageDialog(QuantityDialog.this,
                                    "Cannot remove more than current quantity!\nCurrent: " + supply.getQuantity(),
                                    "Validation Error",
                                    JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        
                        // Check if removing below minimum stock
                        if (!isAddOperation && newQuantity < supply.getMinStockLevel()) {
                            int confirm = JOptionPane.showConfirmDialog(QuantityDialog.this,
                                "Warning: This will bring quantity below minimum stock level!\n" +
                                "Minimum: " + supply.getMinStockLevel() + "\n" +
                                "New quantity will be: " + newQuantity + "\n\n" +
                                "Do you want to continue?",
                                "Below Minimum Stock",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                            
                            if (confirm != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        
                        // Get the logged in user (you should replace this with actual logged in user)
                        String performedBy = "admin"; // Replace with actual username from login
                        String reason = reasonField.getText().trim();
                        if (reason.isEmpty()) {
                            reason = isAddOperation ? "Manual addition" : "Manual removal";
                        }
                        
                        // Use the enhanced method with transaction recording
                        boolean result = medicalSupplyDAO.updateQuantity(
                            supply.getId(), 
                            newQuantity, 
                            performedBy,
                            reason
                        );
                        
                        if (result) {
                            String message = String.format(
                                "✅ Quantity updated successfully!\n\n" +
                                "Item: %s\n" +
                                "Old Quantity: %d\n" +
                                "%s: %d\n" +
                                "New Quantity: %d\n" +
                                "Reason: %s\n\n" +
                                "Transaction has been recorded.",
                                supply.getName(), 
                                supply.getQuantity(),
                                isAddOperation ? "Added" : "Removed", 
                                quantityChange, 
                                newQuantity,
                                reason
                            );
                            
                            // Check if now below minimum stock after operation
                            String warning = "";
                            if (newQuantity <= supply.getMinStockLevel()) {
                                if (newQuantity == 0) {
                                    warning = "\n⚠️ WARNING: Item is now OUT OF STOCK!";
                                } else {
                                    warning = "\n⚠️ WARNING: Item is now LOW STOCK!";
                                }
                            }
                            
                            JOptionPane.showMessageDialog(QuantityDialog.this,
                                message + warning,
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            success = true;
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(QuantityDialog.this,
                                "❌ Failed to update quantity!\nPlease try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(QuantityDialog.this,
                            "Please enter a valid number for quantity",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private boolean validateInput(JTextField updateField, JTextField reasonField) {
        // Validate quantity
        if (updateField.getText().trim().isEmpty()) {
            showError("Please enter quantity");
            updateField.requestFocus();
            return false;
        }
        
        try {
            int quantity = Integer.parseInt(updateField.getText().trim());
            if (quantity <= 0) {
                showError("Please enter a positive number");
                updateField.requestFocus();
                return false;
            }
            
            if (!isAddOperation && quantity > supply.getQuantity()) {
                showError("Cannot remove more than current quantity\nCurrent: " + supply.getQuantity());
                updateField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
            updateField.requestFocus();
            return false;
        }
        
        // Validate reason (optional but recommended)
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "No reason provided. Continue without reason?\n\n" +
                "(Recommended to provide a reason for tracking)",
                "No Reason Provided",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) {
                reasonField.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public boolean isSuccess() {
        return success;
    }
}