package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ToolsAdapter adapter;
    private final List<Tool> fullToolList = new ArrayList<>();
    private final List<Tool> filteredToolList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "Todas";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupCategorySpinner();
        setupSearchView();
        loadTools();
    }

    private void setupRecyclerView() {
        adapter = new ToolsAdapter(getContext(), filteredToolList);
        binding.toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.toolsRecyclerView.setAdapter(adapter);
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>(Arrays.asList("Todas", "Elétrica", "Marcenaria", "Jardinagem", "Hidráulica", "Mecânica", "Outros"));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(spinnerAdapter);

        binding.categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = parent.getItemAtPosition(position).toString();
                filterTools();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterTools();
                return true;
            }
        });
    }

    private void loadTools() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return; // User not logged in
        }
        String currentUserId = currentUser.getUid();

        db.collection("tools").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullToolList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Tool tool = document.toObject(Tool.class);
                            tool.setId(document.getId());

                            if (tool.getOwnerId() != null && !tool.getOwnerId().equals(currentUserId)) {
                                fullToolList.add(tool);
                            }
                        }
                        filterTools(); // Initial filter
                    }
                });
    }

    private void filterTools() {
        filteredToolList.clear();
        for (Tool tool : fullToolList) {
            boolean categoryMatches = currentCategoryFilter.equals("Todas") || (tool.getCategories() != null && tool.getCategories().contains(currentCategoryFilter));
            boolean nameMatches = tool.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (categoryMatches && nameMatches) {
                filteredToolList.add(tool);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
