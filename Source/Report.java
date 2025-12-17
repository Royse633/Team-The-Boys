import java.time.LocalDate;
import java.time.LocalDateTime;

public class Report {
    private int id;
    private String reportType;
    private LocalDate reportDate;
    private String generatedBy;
    private String filePath;
    private LocalDateTime createdAt;
    
    // Constructor
    public Report(int id, String reportType, LocalDate reportDate, 
                  String generatedBy, String filePath, LocalDateTime createdAt) {
        this.id = id;
        this.reportType = reportType;
        this.reportDate = reportDate;
        this.generatedBy = generatedBy;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("%s - %s (Generated: %s)", 
            reportType, reportDate.toString(), generatedBy);
    }
}