import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class EditSupplyDialog extends JDialog {
    private boolean success = false;
    private MedicalSupplyDAO medicalSupplyDAO;
    private DateTimeFormatter dateFormatter;
    private MedicalSupply supply;
    
    public EditSupplyDialog(MedicalSupply supply) {
        this.supply = supply;
        medicalSupplyDAO = new MedicalSupplyDAO();
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Edit Supply");
        setModal(true);
        setSize(400, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Edit Supply: " + supply.getName(), JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        
        JLabel nameLabel = new JLabel("Supply Name:*");
        JTextField nameField = new JTextField(supply.getName());
        
        JLabel categoryLabel = new JLabel("Category:*");
        JTextField categoryField = new JTextField(supply.getCategory());
        
        JLabel quantityLabel = new JLabel("Quantity:*");
        JTextField quantityField = new JTextField(String.valueOf(supply.getQuantity()));
        
        JLabel expiryLabel = new JLabel("Expiry Date:*");
        JTextField expiryField = new JTextField(supply.getExpiryDate().toString());
        
        JLabel locationLabel = new JLabel("Location:*");
        JTextField locationField = new JTextField(supply.getLocation());
        
        JLabel supplierLabel = new JLabel("Supplier:");
        JTextField supplierField = new JTextField(supply.getSupplier() != null ? supply.getSupplier() : "");
        
        JLabel minStockLabel = new JLabel("Min Stock Level:*");
        JTextField minStockField = new JTextField(String.valueOf(supply.getMinStockLevel()));
        
        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(categoryLabel);
        formPanel.add(categoryField);
        formPanel.add(quantityLabel);
        formPanel.add(quantityField);
        formPanel.add(expiryLabel);
        formPanel.add(expiryField);
        formPanel.add(locationLabel);
        formPanel.add(locationField);
        formPanel.add(supplierLabel);
        formPanel.add(supplierField);
        formPanel.add(minStockLabel);
        formPanel.add(minStockField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton("Save Changes");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.setBackground(new Color(255, 193, 7));
        saveButton.setForeground(Color.BLACK);
        cancelButton.setBackground(new Color(108, 117, 125));
        cancelButton.setForeground(Color.WHITE);
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateInput(nameField, categoryField, quantityField, expiryField, locationField, minStockField)) {
                    try {
                        // Update the supply object with new values
                        supply.setName(nameField.getText().trim());
                        supply.setCategory(categoryField.getText().trim());
                        supply.setQuantity(Integer.parseInt(quantityField.getText().trim()));
                        supply.setExpiryDate(LocalDate.parse(expiryField.getText().trim(), dateFormatter));
                        supply.setLocation(locationField.getText().trim());
                        supply.setSupplier(supplierField.getText().trim());
                        supply.setMinStockLevel(Integer.parseInt(minStockField.getText().trim()));
                        
                        boolean result = medicalSupplyDAO.updateSupply(supply);
                        if (result) {
                            JOptionPane.showMessageDialog(EditSupplyDialog.this,
                                "Supply '" + supply.getName() + "' updated successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                            success = true;
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(EditSupplyDialog.this,
                                "Failed to update supply! Please try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (DateTimeParseException ex) {
                        JOptionPane.showMessageDialog(EditSupplyDialog.this,
                            "Invalid date format! Please use YYYY-MM-DD",
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
    
    private boolean validateInput(JTextField name, JTextField category, JTextField quantity, 
                                 JTextField expiry, JTextField location, JTextField minStock) {
        if (name.getText().trim().isEmpty()) {
            showError("Please enter supply name");
            name.requestFocus();
            return false;
        }
        if (category.getText().trim().isEmpty()) {
            showError("Please enter category");
            category.requestFocus();
            return false;
        }
        try {
            int qty = Integer.parseInt(quantity.getText().trim());
            if (qty < 0) {
                showError("Quantity cannot be negative");
                quantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid quantity (numbers only)");
            quantity.requestFocus();
            return false;
        }
        if (expiry.getText().trim().isEmpty()) {
            showError("Please enter expiry date");
            expiry.requestFocus();
            return false;
        }
        if (location.getText().trim().isEmpty()) {
            showError("Please enter location");
            location.requestFocus();
            return false;
        }
        try {
    int quantityValue = Integer.parseInt(quantity.getText().toString().trim());
    if (quantityValue < 0) {
        showError("Please enter valid quantity (cannot be negative)");
        quantity.requestFocus();

                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid minimum stock level (numbers only)");
            minStock.requestFocus();
            return false;
        }
        
        // Validate date format
        try {
            LocalDate.parse(expiry.getText().trim(), dateFormatter);
        } catch (DateTimeParseException e) {
            showError("Invalid date format! Please use YYYY-MM-DD (e.g., 2024-12-31)");
            expiry.requestFocus();
            return false;
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