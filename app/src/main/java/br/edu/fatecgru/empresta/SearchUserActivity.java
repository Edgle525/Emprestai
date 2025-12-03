package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.edu.fatecgru.empresta.databinding.ActivitySearchUserBinding;

public class SearchUserActivity extends AppCompatActivity {

    private static final String TAG = "SearchUserActivity";
    private ActivitySearchUserBinding binding;
    private SearchUserAdapter adapter;
    private final List<User> searchResults = new ArrayList<>();
    private final List<User> connectedUsers = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarSearch);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Buscar Usuário");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        loadConnectedUsers();

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new SearchUserAdapter(this, searchResults);
        binding.searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResultsRecyclerView.setAdapter(adapter);
    }

    private void loadConnectedUsers() {
        binding.progressBarSearch.setVisibility(View.VISIBLE);
        binding.searchInput.setEnabled(false);
        Set<String> connectedIds = new HashSet<>();

        Task<QuerySnapshot> chatsTask = db.collection("chats").whereArrayContains("participants", currentUserId).get();
        Task<QuerySnapshot> loansAsBorrowerTask = db.collection("loans").whereEqualTo("borrowerId", currentUserId).get();
        Task<QuerySnapshot> loansAsOwnerTask = db.collection("loans").whereEqualTo("ownerId", currentUserId).get();

        Tasks.whenAllSuccess(chatsTask, loansAsBorrowerTask, loansAsOwnerTask).addOnSuccessListener(results -> {
            // From chats
            QuerySnapshot chatsSnapshot = (QuerySnapshot) results.get(0);
            for (DocumentSnapshot doc : chatsSnapshot.getDocuments()) {
                List<String> participants = (List<String>) doc.get("participants");
                if (participants != null) {
                    for (String id : participants) {
                        if (!id.equals(currentUserId)) {
                            connectedIds.add(id);
                        }
                    }
                }
            }

            // From loans as borrower
            QuerySnapshot loansAsBorrowerSnapshot = (QuerySnapshot) results.get(1);
            for (DocumentSnapshot doc : loansAsBorrowerSnapshot.getDocuments()) {
                String ownerId = doc.getString("ownerId");
                if (ownerId != null) {
                    connectedIds.add(ownerId);
                }
            }

            // From loans as owner
            QuerySnapshot loansAsOwnerSnapshot = (QuerySnapshot) results.get(2);
            for (DocumentSnapshot doc : loansAsOwnerSnapshot.getDocuments()) {
                String borrowerId = doc.getString("borrowerId");
                if (borrowerId != null) {
                    connectedIds.add(borrowerId);
                }
            }

            if (connectedIds.isEmpty()) {
                binding.progressBarSearch.setVisibility(View.GONE);
                binding.searchInput.setEnabled(true);
                return;
            }

            db.collection("users").whereIn(FieldPath.documentId(), new ArrayList<>(connectedIds)).get()
                .addOnSuccessListener(userSnapshots -> {
                    connectedUsers.clear();
                    for (DocumentSnapshot doc : userSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            connectedUsers.add(user);
                        }
                    }
                    binding.progressBarSearch.setVisibility(View.GONE);
                    binding.searchInput.setEnabled(true);
                    filterUsers(binding.searchInput.getText().toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching connected users", e);
                    Toast.makeText(SearchUserActivity.this, "Erro ao carregar contatos.", Toast.LENGTH_SHORT).show();
                    binding.progressBarSearch.setVisibility(View.GONE);
                    binding.searchInput.setEnabled(true);
                });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching connections", e);
            Toast.makeText(SearchUserActivity.this, "Erro ao carregar conexões.", Toast.LENGTH_SHORT).show();
            binding.progressBarSearch.setVisibility(View.GONE);
            binding.searchInput.setEnabled(true);
        });
    }

    private void filterUsers(String query) {
        searchResults.clear();
        if (!query.isEmpty()) {
            for (User user : connectedUsers) {
                if (user.getName() != null && user.getName().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
