import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplyDAO {
    
    public List<Supply> getAllSupplies() {
        List<Supply> supplies = new ArrayList<>();
        String sql = "SELECT * FROM supplies";
        
        // ALL resources must be inside the same parentheses
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Supply supply = new Supply();
                supply.setId(rs.getInt("id"));
                supply.setName(rs.getString("name"));
                supply.setQuantity(rs.getInt("quantity"));
                supply.setExpiryDate(rs.getDate("expiry_date"));
                supply.setLocation(rs.getString("location"));
                supply.setMinStockLevel(rs.getInt("min_stock_level"));
                supplies.add(supply);
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting supplies: " + e.getMessage());
            e.printStackTrace();
        }
        
        return supplies;
    }
    
    // Also fix all your other methods the same way:
    public int getTotalSupplies() {
        String sql = "SELECT COUNT(*) as total FROM supplies";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting total supplies: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) as low_stock FROM supplies WHERE quantity <= min_stock_level";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("low_stock");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting low stock count: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getTotalCategories() {
        String sql = "SELECT COUNT(DISTINCT category) as total FROM supplies";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting total categories: " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getExpiringSoonCount() {
        String sql = "SELECT COUNT(*) as expiring FROM supplies WHERE expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("expiring");
            }
            
        } catch (SQLException e) {
            System.out.println("Error getting expiring soon count: " + e.getMessage());
        }
        
        return 0;
    }
}