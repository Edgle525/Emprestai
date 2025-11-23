package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.ActivityMyToolsBinding;

public class MyToolsActivity extends AppCompatActivity {

    private ActivityMyToolsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private MyToolsAdapter adapter;
    private final List<Tool> toolList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyToolsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserTools(); // Carrega as ferramentas toda vez que a tela se torna visível
    }

    private void setupRecyclerView() {
        adapter = new MyToolsAdapter(this, toolList);
        binding.myToolsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.myToolsRecyclerView.setAdapter(adapter);
    }

    private void loadUserTools() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("tools").whereEqualTo("ownerId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toolList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Tool tool = document.toObject(Tool.class);
                            tool.setId(document.getId());
                            toolList.add(tool);
                        }
                        adapter.notifyDataSetChanged();

                        // Controla a visibilidade da mensagem de lista vazia
                        if (toolList.isEmpty()) {
                            binding.myToolsRecyclerView.setVisibility(View.GONE);
                            binding.emptyView.setVisibility(View.VISIBLE);
                        } else {
                            binding.myToolsRecyclerView.setVisibility(View.VISIBLE);
                            binding.emptyView.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(MyToolsActivity.this, "Erro ao buscar ferramentas: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
