package br.edu.fatecgru.empresta;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.ActivityLoanRequestsBinding;

public class LoanRequestsActivity extends AppCompatActivity {

    private ActivityLoanRequestsBinding binding;
    private LoanRequestsAdapter adapter;
    private final List<Loan> loanRequests = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String toolId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoanRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolId = getIntent().getStringExtra("TOOL_ID");

        setupRecyclerView();
        loadLoanRequests();
    }

    private void setupRecyclerView() {
        adapter = new LoanRequestsAdapter(this, loanRequests);
        binding.loanRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.loanRequestsRecyclerView.setAdapter(adapter);
    }

    private void loadLoanRequests() {
        if (toolId == null) return;

        db.collection("loans")
            .whereEqualTo("toolId", toolId)
            .whereEqualTo("status", "Aguardando Aprovação")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                loanRequests.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Loan loan = document.toObject(Loan.class);
                    loan.setId(document.getId());
                    loanRequests.add(loan);
                }
                adapter.notifyDataSetChanged();
            });
    }
}
