package br.edu.fatecgru.empresta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import br.edu.fatecgru.empresta.databinding.ItemLoanBinding;

import static br.edu.fatecgru.empresta.Loan.Status.*;

public class LoansAdapter extends RecyclerView.Adapter<LoansAdapter.LoanViewHolder> {

    private final List<Loan> loanList;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public LoansAdapter(Context context, List<Loan> loanList) {
        this.context = context;
        this.loanList = loanList;
    }

    @NonNull
    @Override
    public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoanBinding binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LoanViewHolder(binding);
    }

    private String getStatusInPortuguese(Loan.Status status) {
        if (status == null) {
            return "Desconhecido";
        }
        switch (status) {
            case REQUESTED:
                return "Solicitado";
            case ACCEPTED:
                return "Aceito";
            case REJECTED:
                return "Rejeitado";
            case ACTIVE:
                return "Ativo";
            case RETURNED:
                return "Devolvido";
            case COMPLETED:
                return "Concluído";
            case CANCELED:
                return "Cancelado";
            default:
                return status.name();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        Loan loan = loanList.get(position);

        db.collection("tools").document(loan.getToolId()).get().addOnSuccessListener(toolDoc -> {
            if (toolDoc.exists()) {
                Tool tool = toolDoc.toObject(Tool.class);
                if (tool != null) {
                    holder.binding.loanToolName.setText(tool.getName());
                    if (tool.getImageUrls() != null && !tool.getImageUrls().isEmpty()) {
                        Glide.with(context).load(tool.getImageUrls().get(0)).into(holder.binding.loanToolImage);
                    }
                }
            }
        });

        boolean isOwner = loan.getOwnerId().equals(currentUserId);
        String otherUserId = isOwner ? loan.getBorrowerId() : loan.getOwnerId();
        String userInfoPrefix = isOwner ? "Para: " : "De: ";

        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                holder.binding.loanUserInfo.setText(userInfoPrefix + userDoc.getString("name"));
            }
        });

        updateStatusText(holder, loan, loan.isExpanded());

        holder.itemView.setOnClickListener(v -> {
            loan.setExpanded(!loan.isExpanded());
            notifyItemChanged(position);
        });

        setupActionButtons(holder, loan, isOwner);
    }

    private void updateStatusText(LoanViewHolder holder, Loan loan, boolean isExpanded) {
        String statusText = "Status: " + getStatusInPortuguese(loan.getStatusEnum());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        if (loan.getPickupDate() != null && loan.getExpectedReturnDate() != null) {
            long diffInMillis = Math.abs(loan.getExpectedReturnDate().getTime() - loan.getPickupDate().getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

            if (loan.getStatusEnum() == REQUESTED) {
                 statusText += "\nPor " + (diffInDays + 1) + " dia(s)";
            } else if (loan.getStatusEnum() == ACTIVE) {
                long daysLeft = TimeUnit.DAYS.convert(loan.getExpectedReturnDate().getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);
                statusText += "\nDevolver em " + (daysLeft >= 0 ? daysLeft + 1 : 0) + " dia(s)";
            }
        }

        if (isExpanded) {
            if (loan.getPickupDate() != null) {
                statusText += "\nRetirada: " + sdf.format(loan.getPickupDate());
            }
            if (loan.getExpectedReturnDate() != null) {
                statusText += "\nDevolução: " + sdf.format(loan.getExpectedReturnDate());
            }
            if (loan.getActualPickupDate() != null) {
                statusText += "\nRetirado em: " + sdf.format(loan.getActualPickupDate());
            }
            if (loan.getReturnedDate() != null) {
                statusText += "\nDevolvido em: " + sdf.format(loan.getReturnedDate());
            }
             if (loan.getCompletedDate() != null) {
                statusText += "\nConcluído em: " + sdf.format(loan.getCompletedDate());
            }
        }
        holder.binding.loanStatus.setText(statusText);
    }

    private void setupActionButtons(LoanViewHolder holder, Loan loan, boolean isOwner) {
        holder.binding.approveButton.setVisibility(View.GONE);
        holder.binding.denyButton.setVisibility(View.GONE);
        holder.binding.confirmPickupButton.setVisibility(View.GONE);
        holder.binding.returnButton.setVisibility(View.GONE);

        Loan.Status status = loan.getStatusEnum();
        if (status == null) return;

        switch (status) {
            case REQUESTED:
                if (isOwner) {
                    holder.binding.approveButton.setVisibility(View.VISIBLE);
                    holder.binding.denyButton.setVisibility(View.VISIBLE);
                    holder.binding.approveButton.setText("Aprovar");
                    holder.binding.denyButton.setText("Recusar");
                    holder.binding.approveButton.setOnClickListener(v -> updateLoanStatus(loan, ACCEPTED));
                    holder.binding.denyButton.setOnClickListener(v -> updateLoanStatus(loan, REJECTED));
                } else {
                    holder.binding.denyButton.setVisibility(View.VISIBLE);
                    holder.binding.denyButton.setText("Cancelar");
                    holder.binding.denyButton.setOnClickListener(v -> updateLoanStatus(loan, CANCELED));
                }
                break;
            case ACCEPTED:
                if (!isOwner) {
                    holder.binding.confirmPickupButton.setVisibility(View.VISIBLE);
                    holder.binding.confirmPickupButton.setText("Confirmar Retirada");

                    Date now = new Date();
                    boolean canPickup = !now.before(loan.getPickupDate()) && now.before(loan.getExpectedReturnDate());
                    holder.binding.confirmPickupButton.setEnabled(canPickup);
                    holder.binding.confirmPickupButton.setAlpha(canPickup ? 1.0f : 0.5f);

                    holder.binding.confirmPickupButton.setOnClickListener(v -> {
                        Date clickTime = new Date();
                        if (!clickTime.before(loan.getPickupDate()) && clickTime.before(loan.getExpectedReturnDate())) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", ACTIVE.name());
                            updates.put("actualPickupDate", clickTime);
                            updateLoan(loan, updates, ACTIVE);
                        } else if (clickTime.before(loan.getPickupDate())) {
                            Toast.makeText(context, "A retirada só pode ser feita a partir da data combinada.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "O prazo para retirada expirou.", Toast.LENGTH_LONG).show();
                        }
                    });
                    holder.binding.denyButton.setVisibility(View.VISIBLE);
                    holder.binding.denyButton.setText("Cancelar");
                    holder.binding.denyButton.setOnClickListener(v -> updateLoanStatus(loan, CANCELED));
                } else {
                    holder.binding.denyButton.setVisibility(View.VISIBLE);
                    holder.binding.denyButton.setText("Cancelar");
                    holder.binding.denyButton.setOnClickListener(v -> updateLoanStatus(loan, CANCELED));
                }
                break;
            case ACTIVE:
                if (!isOwner) {
                    holder.binding.returnButton.setVisibility(View.VISIBLE);
                    holder.binding.returnButton.setText("Devolver");
                    holder.binding.returnButton.setOnClickListener(v -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", RETURNED.name());
                        updates.put("returnedDate", new Date());
                        updateLoan(loan, updates, RETURNED);
                    });
                }
                break;
            case RETURNED:
                if (isOwner) {
                    holder.binding.approveButton.setVisibility(View.VISIBLE);
                    holder.binding.approveButton.setText("Confirmar Devolução");
                    holder.binding.approveButton.setOnClickListener(v -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", COMPLETED.name());
                        updates.put("completedDate", new Date());
                        updateLoan(loan, updates, COMPLETED);
                    });
                }
                break;
        }
    }

    private void updateLoanStatus(Loan loan, Loan.Status newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus.name());
        updateLoan(loan, updates, newStatus);
    }

    private void updateLoan(Loan loan, Map<String, Object> updates, Loan.Status newStatus) {
        int position = loanList.indexOf(loan);
        db.collection("loans").document(loan.getId()).update(updates)
            .addOnSuccessListener(aVoid -> {
                loan.setStatusEnum(newStatus);
                if (updates.containsKey("actualPickupDate")) loan.setActualPickupDate((Date) updates.get("actualPickupDate"));
                if (updates.containsKey("returnedDate")) loan.setReturnedDate((Date) updates.get("returnedDate"));
                if (updates.containsKey("completedDate")) loan.setCompletedDate((Date) updates.get("completedDate"));

                if (position != -1) {
                    notifyItemChanged(position);
                }
                Toast.makeText(context, "Status atualizado!", Toast.LENGTH_SHORT).show();

                if (newStatus == ACCEPTED) {
                    rejectConflictingRequests(loan);
                }
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectConflictingRequests(Loan acceptedLoan) {
        if (acceptedLoan.getPickupDate() == null || acceptedLoan.getExpectedReturnDate() == null) {
            return; // No dates to check against
        }

        db.collection("loans")
                .whereEqualTo("toolId", acceptedLoan.getToolId())
                .whereEqualTo("status", REQUESTED.name())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Date acceptedStart = acceptedLoan.getPickupDate();
                    Date acceptedEnd = acceptedLoan.getExpectedReturnDate();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Loan requestedLoan = document.toObject(Loan.class);
                        requestedLoan.setId(document.getId());

                        if (requestedLoan.getId().equals(acceptedLoan.getId())) {
                            continue; // Don't reject the one we just accepted
                        }

                        Date requestedStart = requestedLoan.getPickupDate();
                        Date requestedEnd = requestedLoan.getExpectedReturnDate();

                        if (requestedStart == null || requestedEnd == null) {
                            continue;
                        }

                        // Check for overlap: (StartA <= EndB) and (EndA >= StartB)
                        if (!acceptedStart.after(requestedEnd) && !acceptedEnd.before(requestedStart)) {
                            db.collection("loans").document(requestedLoan.getId()).update("status", REJECTED.name());
                            
                            int conflictPosition = -1;
                            for(int i = 0; i < loanList.size(); i++){
                                if(loanList.get(i).getId().equals(requestedLoan.getId())){
                                    conflictPosition = i;
                                    loanList.get(i).setStatusEnum(REJECTED);
                                    break;
                                }
                            }
                            if (conflictPosition != -1) {
                                notifyItemChanged(conflictPosition);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Falha ao verificar conflitos.", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public int getItemCount() {
        return loanList.size();
    }

    static class LoanViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoanBinding binding;

        public LoanViewHolder(ItemLoanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
