package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.edu.fatecgru.empresta.databinding.ActivityConversationsBinding;

public class ConversationsActivity extends AppCompatActivity {

    private ActivityConversationsBinding binding;
    private ConversationsAdapter adapter;
    private final List<Conversation> conversationList = new ArrayList<>();
    private final List<Conversation> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();
        loadConnectedUsersAndConversations();
        setupSearchView();
    }

    private void setupRecyclerView() {
        adapter = new ConversationsAdapter(this, filteredList);
        binding.conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.conversationsRecyclerView.setAdapter(adapter);
    }

    private void loadConnectedUsersAndConversations() {
        Set<String> connectedUserIds = new HashSet<>();

        // Get users from chats
        db.collection("chats").whereArrayContains("participants", currentUserId).get()
            .addOnSuccessListener(chatSnapshots -> {
                for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                    List<String> participants = (List<String>) chatDoc.get("participants");
                    for (String participantId : participants) {
                        if (!participantId.equals(currentUserId)) {
                            connectedUserIds.add(participantId);
                        }
                    }
                }

                // Get users from loans
                db.collection("loans").whereEqualTo("borrowerId", currentUserId).get()
                    .addOnSuccessListener(loanSnapshots -> {
                        for (QueryDocumentSnapshot loanDoc : loanSnapshots) {
                            connectedUserIds.add(loanDoc.getString("ownerId"));
                        }

                        loadConversations(new ArrayList<>(connectedUserIds));
                    });
            });
    }

    private void loadConversations(List<String> userIds) {
        conversationList.clear();

        if (userIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users").whereIn("uid", userIds).get()
            .addOnSuccessListener(userSnapshots -> {
                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                    User user = userDoc.toObject(User.class);
                    // For simplicity, we're not loading last message here. Can be added later.
                    conversationList.add(new Conversation(user.getUid(), user.getName(), user.getPhotoUrl(), ""));
                }
                filterList("");
            });
    }

    private void setupSearchView() {
        binding.userSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(conversationList);
        } else {
            for (Conversation conversation : conversationList) {
                if (conversation.getOtherUserName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(conversation);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
