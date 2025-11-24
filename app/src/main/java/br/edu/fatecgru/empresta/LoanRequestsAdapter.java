package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemLoanRequestBinding;

public class LoanRequestsAdapter extends RecyclerView.Adapter<LoanRequestsAdapter.LoanRequestViewHolder> {

    private final List<Loan> loanRequests;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LoanRequestsAdapter(Context context, List<Loan> loanRequests) {
        this.context = context;
        this.loanRequests = loanRequests;
    }

    @NonNull
    @Override
    public LoanRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoanRequestBinding binding = ItemLoanRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LoanRequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanRequestViewHolder holder, int position) {
        Loan loan = loanRequests.get(position);

        holder.binding.borrowingTimeRequest.setText("Período: " + loan.getBorrowingTime());

        // Carregar dados do solicitante
        db.collection("users").document(loan.getBorrowerId()).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String borrowerName = userDoc.getString("name");
                String photoUrl = userDoc.getString("photoUrl");
                holder.binding.borrowerName.setText(borrowerName);
                
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(context).load(photoUrl).into(holder.binding.borrowerPhoto);
                }

                holder.binding.chatButtonRequest.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, loan.getBorrowerId());
                    intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, borrowerName);
                    intent.putExtra(ChatActivity.EXTRA_OTHER_USER_PHOTO_URL, photoUrl); // Corrigido
                    context.startActivity(intent);
                });
            }
        });

        // Ações dos botões
        holder.binding.approveRequestButton.setOnClickListener(v -> updateLoanStatus(loan, "Aprovado", position));
        holder.binding.denyRequestButton.setOnClickListener(v -> updateLoanStatus(loan, "Recusado", position));
    }

    private void updateLoanStatus(Loan loan, String newStatus, int position) {
        db.collection("loans").document(loan.getId()).update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Solicitação " + newStatus.toLowerCase(), Toast.LENGTH_SHORT).show();
                loanRequests.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, loanRequests.size());
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Erro ao atualizar solicitação", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return loanRequests.size();
    }

    static class LoanRequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoanRequestBinding binding;

        public LoanRequestViewHolder(ItemLoanRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
