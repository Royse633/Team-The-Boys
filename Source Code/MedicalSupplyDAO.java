import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class MedicalSupplyDAO {
    
    // ===================== CREATE SUPPLY WITH TRANSACTION =====================
    public boolean createSupply(MedicalSupply supply) {
        String sql = "INSERT INTO supplies (name, category, quantity, expiry_date, location, supplier, min_stock_level) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, supply.getName());
                pstmt.setString(2, supply.getCategory());
                pstmt.setInt(3, supply.getQuantity());
                
                // Handle null expiry date
                if (supply.getExpiryDate() != null) {
                    pstmt.setDate(4, Date.valueOf(supply.getExpiryDate()));
                } else {
                    pstmt.setNull(4, Types.DATE);
                }
                
                pstmt.setString(5, supply.getLocation());
                pstmt.setString(6, supply.getSupplier());
                pstmt.setInt(7, supply.getMinStockLevel());
                
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows > 0) {
                    // Get the generated ID
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int supplyId = generatedKeys.getInt(1);
                        
                        // Record transaction for initial quantity
                        Transaction transaction = new Transaction(
                            supplyId,
                            "IN",
                            supply.getQuantity(),
                            0,
                            supply.getQuantity(),
                            "New supply added to inventory",
                            "admin" // TODO: Replace with actual logged-in user
                        );
                        
                        // Record the transaction
                        recordTransactionInConnection(conn, transaction);
                        
                        System.out.println("✅ Transaction recorded for new supply: " + supply.getName() + 
                                         " (ID: " + supplyId + ", Qty: " + supply.getQuantity() + ")");
                    }
                    
                    conn.commit();
                    return true;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error creating supply: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error creating supply: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return false;
    }
    
    // ===================== UPDATE SUPPLY WITH TRANSACTION =====================
    public boolean updateSupply(MedicalSupply supply) {
        // First get the old supply data
        MedicalSupply oldSupply = getSupplyById(supply.getId());
        if (oldSupply == null) {
            System.err.println("❌ Cannot find supply with ID: " + supply.getId());
            return false;
        }
        
        String sql = "UPDATE supplies SET name = ?, category = ?, quantity = ?, expiry_date = ?, " +
                    "location = ?, supplier = ?, min_stock_level = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, supply.getName());
                pstmt.setString(2, supply.getCategory());
                pstmt.setInt(3, supply.getQuantity());
                
                // Handle null expiry date
                if (supply.getExpiryDate() != null) {
                    pstmt.setDate(4, Date.valueOf(supply.getExpiryDate()));
                } else {
                    pstmt.setNull(4, Types.DATE);
                }
                
                pstmt.setString(5, supply.getLocation());
                pstmt.setString(6, supply.getSupplier());
                pstmt.setInt(7, supply.getMinStockLevel());
                pstmt.setInt(8, supply.getId());
                
                int affectedRows = pstmt.executeUpdate();
                
                // Check if quantity changed
                if (affectedRows > 0 && oldSupply.getQuantity() != supply.getQuantity()) {
                    int quantityChanged = supply.getQuantity() - oldSupply.getQuantity();
                    String transactionType = quantityChanged > 0 ? "IN" : "OUT";
                    
                    // Record transaction
                    Transaction transaction = new Transaction(
                        supply.getId(),
                        transactionType,
                        Math.abs(quantityChanged),
                        oldSupply.getQuantity(),
                        supply.getQuantity(),
                        "Supply quantity updated in Manage Supplies",
                        "admin" // TODO: Replace with actual logged-in user
                    );
                    
                    recordTransactionInConnection(conn, transaction);
                    
                    System.out.println("✅ Transaction recorded for updated supply: " + supply.getName() + 
                                     " (Changed: " + quantityChanged + ", New Qty: " + supply.getQuantity() + ")");
                }
                
                conn.commit();
                return affectedRows > 0;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating supply: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error updating supply: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    // ===================== DELETE SUPPLY WITH TRANSACTION =====================
    public boolean deleteSupply(int id) {
        // First get the supply data before deleting
        MedicalSupply supply = getSupplyById(id);
        if (supply == null) {
            System.err.println("❌ Cannot find supply with ID: " + id);
            return false;
        }
        
        String sql = "DELETE FROM supplies WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Record transaction for deletion (OUT transaction for remaining quantity)
                if (supply.getQuantity() > 0) {
                    Transaction transaction = new Transaction(
                        id,
                        "OUT",
                        supply.getQuantity(),
                        supply.getQuantity(),
                        0,
                        "Supply deleted from system",
                        "admin" // TODO: Replace with actual logged-in user
                    );
                    
                    recordTransactionInConnection(conn, transaction);
                    
                    System.out.println("✅ Transaction recorded for deleted supply: " + supply.getName() + 
                                     " (Removed: " + supply.getQuantity() + " units)");
                }
                
                // Now delete the supply
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, id);
                    int affectedRows = pstmt.executeUpdate();
                    
                    conn.commit();
                    return affectedRows > 0;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error deleting supply: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error deleting supply: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    // ===================== TRANSACTION METHODS =====================
    
    // Helper method to record transaction within a connection
    private boolean recordTransactionInConnection(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (supply_id, transaction_type, quantity_changed, " +
                    "previous_quantity, new_quantity, reason, performed_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, transaction.getSupplyId());
            pstmt.setString(2, transaction.getTransactionType());
            pstmt.setInt(3, transaction.getQuantityChanged());
            pstmt.setInt(4, transaction.getPreviousQuantity());
            pstmt.setInt(5, transaction.getNewQuantity());
            pstmt.setString(6, transaction.getReason());
            pstmt.setString(7, transaction.getPerformedBy());
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // Record a transaction (standalone method)
    public boolean recordTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (supply_id, transaction_type, quantity_changed, " +
                    "previous_quantity, new_quantity, reason, performed_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, transaction.getSupplyId());
            pstmt.setString(2, transaction.getTransactionType());
            pstmt.setInt(3, transaction.getQuantityChanged());
            pstmt.setInt(4, transaction.getPreviousQuantity());
            pstmt.setInt(5, transaction.getNewQuantity());
            pstmt.setString(6, transaction.getReason());
            pstmt.setString(7, transaction.getPerformedBy());
            
            boolean result = pstmt.executeUpdate() > 0;
            System.out.println("✅ Transaction recorded: " + (result ? "SUCCESS" : "FAILED"));
            return result;
            
        } catch (SQLException e) {
            System.err.println("❌ Error recording transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Get all transactions with limit
    public List<Transaction> getAllTransactions(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = extractTransactionFromResultSet(rs);
                transactions.add(transaction);
            }
            
            System.out.println("✅ Loaded " + transactions.size() + " transactions from database");
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting all transactions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    // Direct SQL method for TransactionsFrame
    public List<Transaction> getTransactionsDirect() {
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT t.*, s.name as supply_name FROM transactions t " +
                        "LEFT JOIN supplies s ON t.supply_id = s.id " +
                        "ORDER BY t.transaction_date DESC LIMIT 100";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Transaction transaction = extractTransactionFromResultSet(rs);
                    // Also get supply name if available
                    transaction.setReason(rs.getString("reason") + " [" + rs.getString("supply_name") + "]");
                    transactions.add(transaction);
                }
                
                System.out.println("✅ Direct SQL loaded " + transactions.size() + " transactions");
                
            }
        } catch (SQLException e) {
            System.err.println("❌ Error in getTransactionsDirect: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    // Helper method to extract transaction from ResultSet
    private Transaction extractTransactionFromResultSet(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setSupplyId(rs.getInt("supply_id"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setQuantityChanged(rs.getInt("quantity_changed"));
        transaction.setPreviousQuantity(rs.getInt("previous_quantity"));
        transaction.setNewQuantity(rs.getInt("new_quantity"));
        transaction.setReason(rs.getString("reason"));
        transaction.setPerformedBy(rs.getString("performed_by"));
        
        Timestamp timestamp = rs.getTimestamp("transaction_date");
        if (timestamp != null) {
            transaction.setTransactionDate(timestamp.toLocalDateTime());
        }
        
        return transaction;
    }
    
    // Get transactions by supply ID
    public List<Transaction> getTransactionsBySupplyId(int supplyId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE supply_id = ? ORDER BY transaction_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, supplyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = extractTransactionFromResultSet(rs);
                transactions.add(transaction);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting transactions: " + e.getMessage());
        }
        
        return transactions;
    }
    
    // Get recent transactions within specified days
    public List<Transaction> getRecentTransactions(int days) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "ORDER BY transaction_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = extractTransactionFromResultSet(rs);
                transactions.add(transaction);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting recent transactions: " + e.getMessage());
        }
        
        return transactions;
    }
    
    // Get total transaction count
    public int getTransactionCount() {
        String sql = "SELECT COUNT(*) as total FROM transactions";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int count = rs.getInt("total");
                System.out.println("📊 Total transactions in database: " + count);
                return count;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting transaction count: " + e.getMessage());
        }
        
        return 0;
    }
    
    // ===================== ENHANCED UPDATE QUANTITY METHOD =====================
    public boolean updateQuantity(int supplyId, int newQuantity, String performedBy, String reason) {
        // Get current quantity
        int currentQuantity = getQuantityById(supplyId);
        int quantityChanged = newQuantity - currentQuantity;
        
        if (quantityChanged == 0) {
            System.out.println("⚠️ No quantity change for supply ID: " + supplyId);
            return true; // No change needed
        }
        
        String transactionType = quantityChanged > 0 ? "IN" : "OUT";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Update quantity
                String updateSql = "UPDATE supplies SET quantity = ? WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, newQuantity);
                    updateStmt.setInt(2, supplyId);
                    updateStmt.executeUpdate();
                }
                
                // Record transaction
                Transaction transaction = new Transaction(
                    supplyId,
                    transactionType,
                    Math.abs(quantityChanged),
                    currentQuantity,
                    newQuantity,
                    reason,
                    performedBy
                );
                
                recordTransactionInConnection(conn, transaction);
                
                System.out.println("✅ Transaction recorded: " + transactionType + " " + 
                                 Math.abs(quantityChanged) + " units for supply ID: " + supplyId +
                                 " (From: " + currentQuantity + " To: " + newQuantity + ")");
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating quantity: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Helper method to get current quantity
    private int getQuantityById(int supplyId) {
        String sql = "SELECT quantity FROM supplies WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, supplyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("quantity");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting quantity: " + e.getMessage());
        }
        
        return 0;
    }
    
    // ===================== ORIGINAL METHODS (UNCHANGED) =====================
    
    // Get all supplies
    public List<MedicalSupply> getAllSupplies() {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies ORDER BY name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting supplies: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error loading supplies: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        return supplies;
    }
    
    // Helper method to extract supply from ResultSet
    private MedicalSupply extractSupplyFromResultSet(ResultSet rs) throws SQLException {
        LocalDate expiryDate = null;
        Date expiryDateSql = rs.getDate("expiry_date");
        if (expiryDateSql != null) {
            expiryDate = expiryDateSql.toLocalDate();
        }
        
        return new MedicalSupply(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getInt("quantity"),
            expiryDate,
            rs.getString("location"),
            rs.getString("supplier"),
            rs.getInt("min_stock_level")
        );
    }
    
    // Get supply by ID
    public MedicalSupply getSupplyById(int id) {
        String sql = "SELECT * FROM supplies WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractSupplyFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting supply by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    // Search supplies by name or category
    public List<MedicalSupply> searchSupplies(String searchTerm) {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies WHERE name LIKE ? OR category LIKE ? ORDER BY name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setString(2, "%" + searchTerm + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching supplies: " + e.getMessage());
        }
        
        return supplies;
    }
    
    // Get low stock items
    public List<MedicalSupply> getLowStockItems() {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies WHERE quantity <= min_stock_level ORDER BY quantity ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting low stock items: " + e.getMessage());
        }
        
        return supplies;
    }
    
    // Get expiring soon items (within specified days)
    public List<MedicalSupply> getExpiringSoonItems(int daysThreshold) {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                     "AND expiry_date >= CURDATE() ORDER BY expiry_date ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, daysThreshold);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting expiring items: " + e.getMessage());
        }
        
        return supplies;
    }
    
    // Original update quantity method (kept for backward compatibility)
    public boolean updateQuantity(int supplyId, int newQuantity) {
        // Use the enhanced version with default values
        return updateQuantity(supplyId, newQuantity, "admin", "Quantity adjusted");
    }
    
    // User authentication
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next(); // Returns true if user exists
            
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Database authentication error: " + e.getMessage(), 
                "Authentication Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    // Get expired items
    public List<MedicalSupply> getExpiredItems() {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies WHERE expiry_date < CURDATE() AND expiry_date IS NOT NULL ORDER BY expiry_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting expired items: " + e.getMessage());
        }
        
        return supplies;
    }
    
    // Get items expiring in specific days
    public List<MedicalSupply> getExpiringInDays(int days) {
        List<MedicalSupply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                     "AND expiry_date >= CURDATE() AND expiry_date IS NOT NULL ORDER BY expiry_date ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MedicalSupply supply = extractSupplyFromResultSet(rs);
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting expiring items: " + e.getMessage());
        }
        
        return supplies;
    }
    
    // Dispose expired item
    public boolean disposeExpiredItem(int itemId, String reason) {
        String sql = "UPDATE supplies SET quantity = 0, notes = CONCAT(IFNULL(notes, ''), ?) WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String note = "\n[Disposed on " + LocalDate.now() + ": " + reason + "]";
            pstmt.setString(1, note);
            pstmt.setInt(2, itemId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error disposing expired item: " + e.getMessage());
            return false;
        }
    }
    
    // Save report
    public boolean saveReport(String reportType, String dateRange, String generatedBy, String filePath) {
        String fullReportType = reportType + " - " + dateRange;
        
        String sql = "INSERT INTO reports (report_type, report_date, generated_by, file_path) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
            
            pstmt.setString(1, fullReportType);
            pstmt.setDate(2, currentDate);
            pstmt.setString(3, generatedBy);
            pstmt.setString(4, filePath);
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error saving report: " + e.getMessage());
            return false;
        }
    }
    
    // Get all reports
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LocalDate reportDate = null;
                Date sqlDate = rs.getDate("report_date");
                if (sqlDate != null) {
                    reportDate = sqlDate.toLocalDate();
                }
                
                LocalDateTime createdAt = null;
                Timestamp sqlTimestamp = rs.getTimestamp("created_at");
                if (sqlTimestamp != null) {
                    createdAt = sqlTimestamp.toLocalDateTime();
                }
                
                Report report = new Report(
                    rs.getInt("id"),
                    rs.getString("report_type"),
                    reportDate,
                    rs.getString("generated_by"),
                    rs.getString("file_path"),
                    createdAt
                );
                reports.add(report);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting reports: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Error loading reports: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        return reports;
    }
    
    // Delete report
    public boolean deleteReport(int reportId) {
        String sql = "DELETE FROM reports WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
            pstmt.setInt(1, reportId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting report: " + e.getMessage());
            return false;
        }
    }
    
    // Dashboard statistics methods
    public int getTotalSupplies() {
        String sql = "SELECT COUNT(*) as total FROM supplies";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total supplies: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getTotalCategories() {
        String sql = "SELECT COUNT(DISTINCT category) as total FROM supplies";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total categories: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) as total FROM supplies WHERE quantity <= min_stock_level";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting low stock count: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getExpiringSoonCount() {
        String sql = "SELECT COUNT(*) as total FROM supplies WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                     "AND expiry_date >= CURDATE() AND expiry_date IS NOT NULL";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting expiring soon count: " + e.getMessage());
        }
        
        return 0;
    }
    
    // Debug method to test transactions
    public void testTransactionSystem() {
        System.out.println("\n=== TESTING TRANSACTION SYSTEM ===");
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Database connection: OK");
            
            // Check if transactions table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "transactions", null);
            
            if (tables.next()) {
                System.out.println("✅ Transactions table: EXISTS");
                
                // Count transactions
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM transactions");
                if (rs.next()) {
                    System.out.println("📊 Total transactions: " + rs.getInt("count"));
                }
                
                // Show recent transactions
                rs = stmt.executeQuery("SELECT COUNT(*) as count FROM transactions WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 1 DAY)");
                if (rs.next()) {
                    System.out.println("📊 Today's transactions: " + rs.getInt("count"));
                }
                
            } else {
                System.out.println("❌ Transactions table: DOES NOT EXIST");
                System.out.println("   Run: CREATE TABLE transactions (...)");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
}