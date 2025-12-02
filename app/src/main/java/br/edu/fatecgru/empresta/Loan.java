package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class Loan implements Serializable {

    public enum Status {
        REQUESTED,
        ACCEPTED,
        REJECTED,
        ACTIVE,
        RETURNED,
        COMPLETED,
        CANCELED
    }

    private String id;
    private String toolId;
    private String ownerId;
    private String borrowerId;

    // Denormalized data for easier display
    private String toolName;
    private String toolImageUrl;
    private String ownerName;
    private String borrowerName;

    private int loanDurationDays;
    private int loanDurationHours;

    @ServerTimestamp
    private Date requestDate;
    private Date pickupDate; // Requested pickup date
    private Date actualPickupDate; // Actual pickup date
    private Date expectedReturnDate;
    private Date returnedDate;
    private Date completedDate;

    private String status;

    @Exclude
    private boolean isExpanded = false; // Used for UI state

    public Loan() {
        // Required for Firestore
    }

    @Exclude
    public Status getStatusEnum() {
        if (status == null) {
            return null;
        }
        try {
            return Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            switch (status) {
                case "Solicitado":
                    return Status.REQUESTED;
                case "Aceito":
                    return Status.ACCEPTED;
                case "Rejeitado":
                    return Status.REJECTED;
                case "Ativo":
                    return Status.ACTIVE;
                case "Devolvido":
                    return Status.RETURNED;
                case "Concluído":
                    return Status.COMPLETED;
                case "Cancelado":
                    return Status.CANCELED;
                default:
                    return null;
            }
        }
    }

    @Exclude
    public void setStatusEnum(Status statusEnum) {
        if (statusEnum != null) {
            this.status = statusEnum.name();
        } else {
            this.status = null;
        }
    }

    @Exclude
    public boolean isExpanded() {
        return isExpanded;
    }

    @Exclude
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToolId() { return toolId; }
    public void setToolId(String toolId) { this.toolId = toolId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getToolImageUrl() { return toolImageUrl; }
    public void setToolImageUrl(String toolImageUrl) { this.toolImageUrl = toolImageUrl; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public int getLoanDurationDays() { return loanDurationDays; }
    public void setLoanDurationDays(int loanDurationDays) { this.loanDurationDays = loanDurationDays; }

    public int getLoanDurationHours() { return loanDurationHours; }
    public void setLoanDurationHours(int loanDurationHours) { this.loanDurationHours = loanDurationHours; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getPickupDate() { return pickupDate; }
    public void setPickupDate(Date pickupDate) { this.pickupDate = pickupDate; }

    public Date getActualPickupDate() { return actualPickupDate; }
    public void setActualPickupDate(Date actualPickupDate) { this.actualPickupDate = actualPickupDate; }

    public Date getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(Date expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    
    public Date getReturnedDate() { return returnedDate; }
    public void setReturnedDate(Date returnedDate) { this.returnedDate = returnedDate; }

    public Date getCompletedDate() { return completedDate; }
    public void setCompletedDate(Date completedDate) { this.completedDate = completedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
