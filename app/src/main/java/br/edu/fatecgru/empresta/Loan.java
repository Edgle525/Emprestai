package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Loan {
    private String id;
    private String toolId;
    private String ownerId;
    private String borrowerId;
    private String status;
    private String borrowingTime;
    @ServerTimestamp
    private Date requestDate;
    private Date approvalDate;
    private Date returnDate;

    public Loan() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToolId() { return toolId; }
    public void setToolId(String toolId) { this.toolId = toolId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBorrowingTime() { return borrowingTime; }
    public void setBorrowingTime(String borrowingTime) { this.borrowingTime = borrowingTime; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getApprovalDate() { return approvalDate; }
    public void setApprovalDate(Date approvalDate) { this.approvalDate = approvalDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
}
