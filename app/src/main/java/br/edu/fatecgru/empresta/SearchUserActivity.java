package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.ActivitySearchUserBinding;

public class SearchUserActivity extends AppCompatActivity {

    private ActivitySearchUserBinding binding;
    private SearchUserAdapter adapter;
    private final List<User> userList = new ArrayList<>();
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

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new SearchUserAdapter(this, userList);
        binding.searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResultsRecyclerView.setAdapter(adapter);
    }

    private void searchUsers(String query) {
        if (query.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (!document.getId().equals(currentUserId)) {
                            User user = document.toObject(User.class);
                            user.setUid(document.getId());
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
