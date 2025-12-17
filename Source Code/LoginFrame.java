import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.swing.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private MedicalSupplyDAO medicalSupplyDAO;
    
    public LoginFrame() throws SQLException {
        medicalSupplyDAO = new MedicalSupplyDAO();
        
        // Test database connection on startup
       if (!DatabaseConnection.testConnection()) {
    JOptionPane.showMessageDialog(this,
        "Cannot connect to database. Please check:\n" +
        "1. MySQL service is running\n" +
        "2. Database 'medical_inventory' exists\n" +
        "3. Correct password in DatabaseConnection.java",
        "Database Connection Failed",
        JOptionPane.ERROR_MESSAGE);
}

        
        initializeUI();
    }
    
    private void initializeUI() throws SQLException {
        setTitle("Barangay Medical Inventory Tracker - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 245, 255));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(240, 245, 255));
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        
        JLabel mainTitle = new JLabel("BarangayMedTrack");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        mainTitle.setForeground(new Color(0, 82, 165));
        mainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subTitle = new JLabel("Medical Supply Management");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subTitle.setForeground(new Color(100, 100, 100));
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        titlePanel.add(mainTitle);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        titlePanel.add(subTitle);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 40)));
        
        JPanel formPanel = new JPanel();
        formPanel.setBackground(new Color(240, 245, 255));
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        
        JPanel usernamePanel = new JPanel(new BorderLayout(10, 5));
        usernamePanel.setBackground(new Color(240, 245, 255));
        usernamePanel.setMaximumSize(new Dimension(300, 60));
        
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(250, 35));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        usernamePanel.add(usernameLabel, BorderLayout.NORTH);
        usernamePanel.add(usernameField, BorderLayout.CENTER);
        
        JPanel passwordPanel = new JPanel(new BorderLayout(10, 5));
        passwordPanel.setBackground(new Color(240, 245, 255));
        passwordPanel.setMaximumSize(new Dimension(300, 60));
        
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(250, 35));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        passwordPanel.add(passwordLabel, BorderLayout.NORTH);
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        
        JButton loginButton = new JButton("LOGIN");
        loginButton.setBackground(new Color(0, 123, 255));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        loginButton.setMaximumSize(new Dimension(300, 40));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Database status label
boolean isConnected = DatabaseConnection.testConnection();

JLabel dbStatusLabel = new JLabel("Database: " + (isConnected ? "Connected" : "Disconnected"));
dbStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
dbStatusLabel.setForeground(isConnected ? new Color(40, 167, 69) : new Color(220, 53, 69));
dbStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(passwordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        formPanel.add(loginButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(dbStatusLabel);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        add(mainPanel);
        
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
    }
    
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password",
                "Login Failed",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show loading dialog
        JDialog loadingDialog = new JDialog(this, "Authenticating...", true);
        loadingDialog.setSize(200, 100);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.add(new JLabel("Checking credentials...", JLabel.CENTER));
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        // Run authentication in background
        new Thread(() -> {
            boolean isAuthenticated = medicalSupplyDAO.authenticateUser(username, password);
            
            SwingUtilities.invokeLater(() -> {
                loadingDialog.dispose();
                
                if (isAuthenticated) {
                    dispose();
                    new DashboardFrame().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Invalid username or password!\n\nDefault credentials:\nUsername: admin\nPassword: admin123",
                        "Authentication Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
        
        loadingDialog.setVisible(true);
    }
}