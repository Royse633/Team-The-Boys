import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;

public class DashboardFrame extends JFrame {
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private MedicalSupplyDAO medicalSupplyDAO;

    public DashboardFrame() {
        medicalSupplyDAO = new MedicalSupplyDAO();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Barangay Medical Inventory Tracker - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        // Add panels
        mainContentPanel.add(createDashboardContent(), "DASHBOARD");
        mainContentPanel.add(wrapWithBackButton(new ManageSuppliesFrame().getContentPanel()), "MANAGE_SUPPLIES");
        mainContentPanel.add(wrapWithBackButton(new ViewInventoryFrame().getContentPanel()), "VIEW_INVENTORY");
        mainContentPanel.add(wrapWithBackButton(new TransactionsFrame().getContentPanel()), "TRANSACTION_HISTORY");
        mainContentPanel.add(wrapWithBackButton(new GenerateReportsFrame().getContentPanel()), "GENERATE_REPORTS");
        mainContentPanel.add(wrapWithBackButton(new SystemSettingsFrame().getContentPane()), "SYSTEM_SETTINGS");

        add(mainContentPanel, BorderLayout.CENTER);
    }

    // =========================
    // HEADER
    // =========================
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(getWidth(), 90));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Gradient background
        headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(0, 82, 165);
                Color color2 = new Color(0, 123, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setOpaque(false);

        // Left panel: Logo + Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("🏥");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JLabel titleLabel = new JLabel("Barangay Medical Inventory Tracker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);

        leftPanel.add(logoLabel);
        leftPanel.add(titleLabel);

        // Right panel: Notifications + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        rightPanel.setOpaque(false);

        JButton notificationsBtn = createIconButton("🔔", "Notifications");
        JButton logoutBtn = createIconButton("🚪", "Logout");

        notificationsBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        notificationsBtn.setPreferredSize(new Dimension(60, 50));
        notificationsBtn.setBackground(new Color(255, 193, 7));
        notificationsBtn.setForeground(Color.WHITE);
        notificationsBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        notificationsBtn.setFocusPainted(false);

        logoutBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        logoutBtn.setPreferredSize(new Dimension(60, 50));
        logoutBtn.setBackground(new Color(220, 53, 69));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        logoutBtn.setFocusPainted(false);

        rightPanel.add(notificationsBtn);
        rightPanel.add(logoutBtn);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 4, 0, new Color(0, 0, 0, 50)));

        // Logout action
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                try {
                    new LoginFrame().setVisible(true);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        // Notifications action
        notificationsBtn.addActionListener(e -> showNotifications());

        return headerPanel;
    }

    // =========================
    // WRAP SUB-PAGE WITH BACK BUTTON
    // =========================
    private JPanel wrapWithBackButton(Container panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setFocusPainted(false);
        backBtn.setBackground(new Color(220, 53, 69));
        backBtn.setForeground(Color.WHITE);
        backBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> navigateTo("Dashboard"));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);
        topPanel.add(backBtn);

        wrapper.add(topPanel, BorderLayout.NORTH);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    // =========================
    // SIDEBAR + DASHBOARD
    // =========================
    private JPanel createDashboardContent() {
        JPanel dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboardPanel.setBackground(Color.WHITE);

        dashboardPanel.add(createSidebar(), BorderLayout.WEST);
        dashboardPanel.add(createDashboardMain(), BorderLayout.CENTER);

        return dashboardPanel;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(230, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 15, 30, 15));

        String[] menuItems = {
            "Dashboard",
            "Manage Supplies",
            "View Inventory",
            "Transaction History",
            "Generate Reports",
            "System Settings"
        };

        for (String item : menuItems) {
            JButton menuButton = new JButton(item);
            menuButton.setAlignmentX(Component.LEFT_ALIGNMENT);

            menuButton.setMaximumSize(new Dimension(200, 55));
            menuButton.setPreferredSize(new Dimension(200, 55));

            menuButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
            menuButton.setBackground(new Color(245, 245, 245));
            menuButton.setForeground(new Color(40, 40, 40));
            menuButton.setFocusPainted(false);
            menuButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
            menuButton.setHorizontalAlignment(SwingConstants.LEFT);

            menuButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    menuButton.setBackground(new Color(220, 220, 220));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    menuButton.setBackground(new Color(245, 245, 245));
                }
            });

            menuButton.addActionListener(e -> navigateTo(item));

            sidebar.add(menuButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        return sidebar;
    }

    private JPanel createDashboardMain() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(Color.WHITE);

        // Dashboard Title
        JLabel welcomeLabel = new JLabel("Dashboard", JLabel.LEFT);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));

        // Stats Cards Panel - REMOVED "Total Value"
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 20, 20)); // Changed to 2x2 grid
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));

        // Updated stats array without "Total Value"
        String[] stats = {"Total Supplies", "Categories", "Low Stock Items", "Expiring Soon", "Transactions"};
        String[] values = {
                String.valueOf(medicalSupplyDAO.getTotalSupplies()),
                String.valueOf(medicalSupplyDAO.getTotalCategories()),
                String.valueOf(medicalSupplyDAO.getLowStockCount()),
                String.valueOf(medicalSupplyDAO.getExpiringSoonCount()),
                String.valueOf(medicalSupplyDAO.getTransactionCount())
        };

        Color[] colors = {
                new Color(0, 82, 165),
                new Color(0, 123, 255),
                new Color(220, 53, 69),
                new Color(255, 193, 7),
                new Color(111, 66, 193) // Purple for transactions
        };

        for (int i = 0; i < stats.length; i++) {
            statsPanel.add(createStatCard(stats[i], values[i], colors[i]));
        }

        // Lower Panel: Low Stock + Recent Activity
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 20, 20));

        // Low Stock Panel
        JPanel lowStockPanel = new JPanel(new BorderLayout());
        lowStockPanel.setBorder(BorderFactory.createTitledBorder("Low Stock Alerts"));
        lowStockPanel.setBackground(Color.WHITE);

        List<MedicalSupply> lowStockItems = medicalSupplyDAO.getLowStockItems();
        DefaultListModel<String> lowStockModel = new DefaultListModel<>();
        if (lowStockItems.isEmpty()) lowStockModel.addElement("No low stock items");
        for (MedicalSupply item : lowStockItems) {
            lowStockModel.addElement(String.format("%s - %d left (Min: %d)",
                    item.getName(), item.getQuantity(), item.getMinStockLevel()));
        }

        JList<String> lowStockList = new JList<>(lowStockModel);
        lowStockList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lowStockPanel.add(new JScrollPane(lowStockList), BorderLayout.CENTER);

        // Recent Activity Panel
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));
        activityPanel.setBackground(Color.WHITE);

        DefaultListModel<String> activityModel = new DefaultListModel<>();
        activityModel.addElement("System started");
        activityModel.addElement("User logged in");
        
        // Add recent transactions to activity
        List<Transaction> recentTransactions = medicalSupplyDAO.getRecentTransactions(5);
        for (Transaction transaction : recentTransactions) {
            MedicalSupply supply = medicalSupplyDAO.getSupplyById(transaction.getSupplyId());
            String supplyName = supply != null ? supply.getName() : "Unknown";
            activityModel.addElement(String.format("%s: %s %d units", 
                transaction.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                supplyName, transaction.getQuantityChanged()));
        }
        
        if (medicalSupplyDAO.getTotalSupplies() > 0) {
            activityModel.addElement(medicalSupplyDAO.getTotalSupplies() + " supplies loaded");
        }

        JList<String> activityList = new JList<>(activityModel);
        activityList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        activityPanel.add(new JScrollPane(activityList), BorderLayout.CENTER);

        lowerPanel.add(lowStockPanel);
        lowerPanel.add(activityPanel);

        mainPanel.add(welcomeLabel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(lowerPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    // REMOVED calculateTotalValue() method entirely

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(100, 100, 100));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void navigateTo(String screen) {
        switch (screen) {
            case "Dashboard":
                cardLayout.show(mainContentPanel, "DASHBOARD");
                refreshDashboard();
                break;
            case "Manage Supplies":
                cardLayout.show(mainContentPanel, "MANAGE_SUPPLIES");
                break;
            case "View Inventory":
                cardLayout.show(mainContentPanel, "VIEW_INVENTORY");
                break;
            case "Transaction History":
                cardLayout.show(mainContentPanel, "TRANSACTION_HISTORY");
                break;
            case "Generate Reports":
                cardLayout.show(mainContentPanel, "GENERATE_REPORTS");
                break;
            case "System Settings":
                cardLayout.show(mainContentPanel, "SYSTEM_SETTINGS");
                break;
        }
    }

    private void refreshDashboard() {
        Component[] components = mainContentPanel.getComponents();
        for (Component comp : components) {
            if (comp.isVisible() && comp instanceof JPanel) {
                mainContentPanel.remove(comp);
                mainContentPanel.add(createDashboardContent(), "DASHBOARD");
                cardLayout.show(mainContentPanel, "DASHBOARD");
                break;
            }
        }
    }

    private JButton createIconButton(String icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        button.setBackground(new Color(0, 82, 165));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        return button;
    }

    private void showNotifications() {
        List<MedicalSupply> lowStockItems = medicalSupplyDAO.getLowStockItems();
        List<MedicalSupply> expiringItems = medicalSupplyDAO.getExpiringSoonItems(30);

        StringBuilder notificationText = new StringBuilder();
        notificationText.append("=== NOTIFICATIONS ===\n\n");

        if (lowStockItems.isEmpty() && expiringItems.isEmpty()) {
            notificationText.append("No notifications at this time.\n");
        } else {
            if (!lowStockItems.isEmpty()) {
                notificationText.append("LOW STOCK ITEMS:\n");
                for (MedicalSupply item : lowStockItems) {
                    notificationText.append(String.format("• %s: %d left (Min: %d)\n",
                            item.getName(), item.getQuantity(), item.getMinStockLevel()));
                }
                notificationText.append("\n");
            }

            if (!expiringItems.isEmpty()) {
                notificationText.append("ITEMS EXPIRING SOON:\n");
                for (MedicalSupply item : expiringItems) {
                    notificationText.append(String.format("• %s: Expires %s\n",
                            item.getName(), item.getExpiryDate()));
                }
            }
        }

        JTextArea textArea = new JTextArea(20, 40);
        textArea.setText(notificationText.toString());
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Notifications", JOptionPane.INFORMATION_MESSAGE);
    }
}