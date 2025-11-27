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

import br.edu.fatecgru.empresta.databinding.ActivitySearchMessageBinding;

public class SearchMessageActivity extends AppCompatActivity {

    private ActivitySearchMessageBinding binding;
    private SearchMessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarSearchMessage);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Buscar Mensagem");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();

        binding.searchMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMessages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new SearchMessageAdapter(this, messageList);
        binding.searchMessageResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.searchMessageResultsRecyclerView.setAdapter(adapter);
    }

    private void searchMessages(String query) {
        if (query.isEmpty()) {
            messageList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        messageList.clear();
        // TODO: Implement a more efficient way to search messages across all chats
        db.collectionGroup("messages")
                .whereGreaterThanOrEqualTo("text", query)
                .whereLessThanOrEqualTo("text", query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChatMessage message = document.toObject(ChatMessage.class);
                        message.setMessageId(document.getId());
                        // Check if user is a participant of the chat
                        document.getReference().getParent().getParent().get().addOnSuccessListener(chatDoc -> {
                            List<String> participants = (List<String>) chatDoc.get("participants");
                            if (participants != null && participants.contains(currentUserId)) {
                                messageList.add(message);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
