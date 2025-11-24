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

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemLoanBinding;

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

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        Loan loan = loanList.get(position);
        holder.binding.loanStatus.setText("Status: " + loan.getStatus());

        // Carregar detalhes da ferramenta
        db.collection("tools").document(loan.getToolId()).get().addOnSuccessListener(toolDoc -> {
            if (toolDoc.exists()) {
                Tool tool = toolDoc.toObject(Tool.class);
                holder.binding.loanToolName.setText(tool.getName());
                if (tool.getImageUrls() != null && !tool.getImageUrls().isEmpty()) {
                    Glide.with(context).load(tool.getImageUrls().get(0)).into(holder.binding.loanToolImage);
                }
            }
        });

        // Determinar o papel do usuário (dono ou locatário)
        boolean isOwner = loan.getOwnerId().equals(currentUserId);
        String otherUserId = isOwner ? loan.getBorrowerId() : loan.getOwnerId();
        String userInfoPrefix = isOwner ? "Emprestado para: " : "Emprestado de: ";

        // Carregar informações do outro usuário
        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                holder.binding.loanUserInfo.setText(userInfoPrefix + userDoc.getString("name"));
            }
        });

        // Configurar a visibilidade e a ação dos botões
        setupActionButtons(holder, loan, isOwner);
    }

    private void setupActionButtons(LoanViewHolder holder, Loan loan, boolean isOwner) {
        // Ocultar todos os botões por padrão
        holder.binding.approveButton.setVisibility(View.GONE);
        holder.binding.denyButton.setVisibility(View.GONE);
        holder.binding.confirmPickupButton.setVisibility(View.GONE);
        holder.binding.returnButton.setVisibility(View.GONE);

        switch (loan.getStatus()) {
            case "Aguardando Aprovação":
                if (isOwner) {
                    holder.binding.approveButton.setVisibility(View.VISIBLE);
                    holder.binding.denyButton.setVisibility(View.VISIBLE);
                    holder.binding.approveButton.setOnClickListener(v -> updateLoanStatus(loan, "Aprovado"));
                    holder.binding.denyButton.setOnClickListener(v -> updateLoanStatus(loan, "Recusado"));
                }
                break;
            case "Aprovado":
                if (!isOwner) { // Locatário
                    holder.binding.confirmPickupButton.setVisibility(View.VISIBLE);
                    holder.binding.confirmPickupButton.setOnClickListener(v -> updateLoanStatus(loan, "Em Andamento"));
                }
                break;
            case "Em Andamento":
                if (!isOwner) { // Locatário
                    holder.binding.returnButton.setVisibility(View.VISIBLE);
                    holder.binding.returnButton.setOnClickListener(v -> updateLoanStatus(loan, "Devolvido"));
                }
                break;
        }
    }

    private void updateLoanStatus(Loan loan, String newStatus) {
        db.collection("loans").document(loan.getId()).update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                loan.setStatus(newStatus);
                notifyDataSetChanged();
                Toast.makeText(context, "Status do empréstimo atualizado para " + newStatus, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());
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
