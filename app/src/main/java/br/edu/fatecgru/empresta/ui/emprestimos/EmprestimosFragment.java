package br.edu.fatecgru.empresta.ui.emprestimos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.Loan;
import br.edu.fatecgru.empresta.LoansAdapter;
import br.edu.fatecgru.empresta.databinding.FragmentEmprestimosBinding;

public class EmprestimosFragment extends Fragment {

    private FragmentEmprestimosBinding binding;
    private LoansAdapter adapter;
    private final List<Loan> loanList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEmprestimosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
            loadLoans();
        }
    }

    private void setupRecyclerView() {
        adapter = new LoansAdapter(getContext(), loanList);
        binding.loansRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.loansRecyclerView.setAdapter(adapter);
    }

    private void loadLoans() {
        loanList.clear();

        Task<QuerySnapshot> borrowedQuery = db.collection("loans").whereEqualTo("borrowerId", currentUserId).get();
        Task<QuerySnapshot> ownedQuery = db.collection("loans").whereEqualTo("ownerId", currentUserId).get();

        Tasks.whenAllSuccess(borrowedQuery, ownedQuery).addOnSuccessListener(results -> {
            for (Object snapshot : results) {
                 for (QueryDocumentSnapshot document : (QuerySnapshot) snapshot) {
                    Loan loan = document.toObject(Loan.class);
                    loan.setId(document.getId());
                    
                    boolean alreadyExists = false;
                    for (Loan item : loanList) {
                        if (item.getId().equals(loan.getId())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        loanList.add(loan);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
