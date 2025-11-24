package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.FragmentToolsBinding;

public class ToolsFragment extends Fragment {

    private FragmentToolsBinding binding;
    private ToolsAdapter adapter;
    private final List<Tool> toolList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentToolsBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadTools();
    }

    private void setupRecyclerView() {
        adapter = new ToolsAdapter(getContext(), toolList);
        binding.toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.toolsRecyclerView.setAdapter(adapter);
    }

    private void loadTools() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return; // Não carrega ferramentas se o usuário não estiver logado
        }
        String currentUserId = currentUser.getUid();

        db.collection("tools")
                .whereNotEqualTo("ownerId", currentUserId) // Filtra as ferramentas do usuário atual
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toolList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Tool tool = document.toObject(Tool.class);
                            tool.setId(document.getId());
                            toolList.add(tool);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
