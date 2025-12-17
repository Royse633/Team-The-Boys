import java.time.LocalDate;

public class MedicalSupply {
    private int id;
    private String name;
    private String category;
    private int quantity;
    private LocalDate expiryDate;
    private String location;
    private String supplier;
    private int minStockLevel;
    
    public MedicalSupply(String name, String category, int quantity, LocalDate expiryDate, String location) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.location = location;
    }
    
    // Full constructor
    public MedicalSupply(int id, String name, String category, int quantity, LocalDate expiryDate, String location, String supplier, int minStockLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.location = location;
        this.supplier = supplier;
        this.minStockLevel = minStockLevel;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Qty: %d, Expires: %s", name, category, quantity, expiryDate);
    }
}