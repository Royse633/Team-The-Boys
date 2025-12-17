import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int supplyId;
    private String transactionType;
    private int quantityChanged;
    private int previousQuantity;
    private int newQuantity;
    private String reason;
    private String performedBy;
    private LocalDateTime transactionDate;
    
    public Transaction() {}
    
    public Transaction(int supplyId, String transactionType, int quantityChanged, 
                      int previousQuantity, int newQuantity, String reason, String performedBy) {
        this.supplyId = supplyId;
        this.transactionType = transactionType;
        this.quantityChanged = quantityChanged;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason;
        this.performedBy = performedBy;
        this.transactionDate = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSupplyId() { return supplyId; }
    public void setSupplyId(int supplyId) { this.supplyId = supplyId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public int getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(int quantityChanged) { this.quantityChanged = quantityChanged; }
    
    public int getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(int previousQuantity) { this.previousQuantity = previousQuantity; }
    
    public int getNewQuantity() { return newQuantity; }
    public void setNewQuantity(int newQuantity) { this.newQuantity = newQuantity; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    @Override
    public String toString() {
        return String.format("%s: %d units (From %d to %d) - %s", 
            transactionType, quantityChanged, previousQuantity, newQuantity, reason);
    }
}