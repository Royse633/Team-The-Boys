import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.*;

public class AddSupplyDialog extends JDialog {
    private boolean success = false;
    private MedicalSupplyDAO medicalSupplyDAO;
    private DateTimeFormatter dateFormatter;

    public AddSupplyDialog() {
        medicalSupplyDAO = new MedicalSupplyDAO();
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Add New Supply");
        setModal(true);
        setSize(450, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 247, 250));

        // ===========================
        // Top Bar with Back Button
        // ===========================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JButton backButton = new JButton("← Back");
        backButton.setFocusPainted(false);
        backButton.setBackground(new Color(220, 53, 69));
        backButton.setForeground(Color.WHITE);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        backButton.addActionListener(e -> dispose());

        JLabel titleLabel = new JLabel("Add New Supply", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // ===========================
        // Card-style Form Panel
        // ===========================
        JPanel card = new JPanel(new GridLayout(7, 2, 12, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel nameLabel = new JLabel("Supply Name:*");
        JTextField nameField = new JTextField();

        JLabel categoryLabel = new JLabel("Category:*");
        JTextField categoryField = new JTextField();

        JLabel quantityLabel = new JLabel("Quantity:*");
        JTextField quantityField = new JTextField();

        JLabel expiryLabel = new JLabel("Expiry Date:*");
        JTextField expiryField = new JTextField();
        expiryField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");

        JLabel locationLabel = new JLabel("Location:*");
        JTextField locationField = new JTextField("Storage A");

        JLabel supplierLabel = new JLabel("Supplier:");
        JTextField supplierField = new JTextField();

        JLabel minStockLabel = new JLabel("Min Stock Level:*");
        JTextField minStockField = new JTextField("10");

        card.add(nameLabel);      card.add(nameField);
        card.add(categoryLabel);  card.add(categoryField);
        card.add(quantityLabel);  card.add(quantityField);
        card.add(expiryLabel);    card.add(expiryField);
        card.add(locationLabel);  card.add(locationField);
        card.add(supplierLabel);  card.add(supplierField);
        card.add(minStockLabel);  card.add(minStockField);

        // ===========================
        // BUTTON PANEL
        // ===========================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton addButton = new JButton("Add Supply");
        JButton cancelButton = new JButton("Cancel");

        styleButton(addButton, new Color(40, 167, 69));
        styleButton(cancelButton, new Color(108, 117, 125));

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        // Add everything to the main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(card, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ===================================================
        // ADD SUPPLY LOGIC
        // ===================================================
        addButton.addActionListener(e -> {
            if (validateInput(nameField, categoryField, quantityField, expiryField, locationField, minStockField)) {
                try {
                    MedicalSupply supply = new MedicalSupply(
                            nameField.getText().trim(),
                            categoryField.getText().trim(),
                            Integer.parseInt(quantityField.getText().trim()),
                            LocalDate.parse(expiryField.getText().trim(), dateFormatter),
                            locationField.getText().trim()
                    );

                    supply.setSupplier(supplierField.getText().trim());
                    supply.setMinStockLevel(Integer.parseInt(minStockField.getText().trim()));

                    boolean result = medicalSupplyDAO.createSupply(supply);

                    if (result) {
                        JOptionPane.showMessageDialog(this,
                                "Supply '" + supply.getName() + "' added successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        success = true;
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to add supply! Please try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (DateTimeParseException ex) {
                    showError("Invalid date format! Please use YYYY-MM-DD");
                }
            }
        });

        cancelButton.addActionListener(e -> dispose());
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
    }

    private boolean validateInput(JTextField name, JTextField category, JTextField quantity,
                                  JTextField expiry, JTextField location, JTextField minStock) {
        if (name.getText().trim().isEmpty()) {
            showError("Please enter supply name");
            return false;
        }
        if (category.getText().trim().isEmpty()) {
            showError("Please enter category");
            return false;
        }
        try {
            int qty = Integer.parseInt(quantity.getText().trim());
            if (qty < 0) {
                showError("Quantity cannot be negative");
                return false;
            }
        } catch (Exception e) {
            showError("Quantity must be numbers only");
            return false;
        }

        if (expiry.getText().trim().isEmpty()) {
            showError("Please enter expiry date");
            return false;
        }

        if (location.getText().trim().isEmpty()) {
            showError("Please enter location");
            return false;
        }

        try {
            int min = Integer.parseInt(minStock.getText().trim());
            if (min < 0) {
                showError("Minimum stock must be positive");
                return false;
            }
        } catch (Exception e) {
            showError("Min stock must be numbers only");
            return false;
        }

        try {
            LocalDate.parse(expiry.getText().trim(), dateFormatter);
        } catch (Exception e) {
            showError("Invalid date format. Use YYYY-MM-DD");
            return false;
        }

        return true;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isSuccess() {
        return success;
    }
}
