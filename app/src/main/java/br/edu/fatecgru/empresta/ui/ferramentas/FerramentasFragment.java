package br.edu.fatecgru.empresta.ui.ferramentas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.AddToolActivity;
import br.edu.fatecgru.empresta.MyToolsAdapter;
import br.edu.fatecgru.empresta.Tool;
import br.edu.fatecgru.empresta.databinding.FragmentFerramentasBinding;

public class FerramentasFragment extends Fragment {

    private FragmentFerramentasBinding binding;
    private MyToolsAdapter adapter;
    private final List<Tool> toolList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFerramentasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
            loadMyTools();
        }

        binding.fabAddTool.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddToolActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        adapter = new MyToolsAdapter(getContext(), toolList);
        binding.myToolsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.myToolsRecyclerView.setAdapter(adapter);
    }

    private void loadMyTools() {
        db.collection("tools").whereEqualTo("ownerId", currentUserId).get()
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
