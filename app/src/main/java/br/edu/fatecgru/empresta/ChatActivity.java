package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.OnMessageInteractionListener {

    public static final String EXTRA_OTHER_USER_ID = "OTHER_USER_ID";
    public static final String EXTRA_OTHER_USER_NAME = "OTHER_USER_NAME";
    public static final String EXTRA_OTHER_USER_PHOTO_URL = "OTHER_USER_PHOTO_URL";

    private ActivityChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private CollectionReference messagesCollection;
    private String currentUserId;
    private String otherUserId;
    private String otherUserPhotoUrl;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        String otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);
        otherUserPhotoUrl = getIntent().getStringExtra(EXTRA_OTHER_USER_PHOTO_URL);

        if (otherUserId == null) {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSupportActionBar(binding.chatToolbar.toolbarChatCustom);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        binding.chatToolbar.chatUserName.setText(otherUserName);
        if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
            Glide.with(this).load(otherUserPhotoUrl).into(binding.chatToolbar.chatUserPhoto);
        }

        List<String> ids = new ArrayList<>(Arrays.asList(currentUserId, otherUserId));
        Collections.sort(ids);
        chatRoomId = ids.get(0) + "_" + ids.get(1);

        db = FirebaseFirestore.getInstance();
        messagesCollection = db.collection("chats").document(chatRoomId).collection("messages");

        setupRecyclerView();
        listenForMessages();

        binding.sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages, otherUserPhotoUrl, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void listenForMessages() {
        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    ChatMessage message = dc.getDocument().toObject(ChatMessage.class);
                    message.setMessageId(dc.getDocument().getId()); // Correção: Atribui o ID manualmente

                    switch (dc.getType()) {
                        case ADDED:
                            chatMessages.add(message);
                            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                            binding.chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                            break;
                        case MODIFIED:
                            for (int i = 0; i < chatMessages.size(); i++) {
                                if (chatMessages.get(i).getMessageId().equals(message.getMessageId())) {
                                    chatMessages.set(i, message);
                                    chatAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                    }
                }
            });
    }

    private void sendMessage() {
        String messageText = binding.messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        binding.messageInput.setText("");

        ChatMessage chatMessage = new ChatMessage(currentUserId, messageText);
        messagesCollection.add(chatMessage);

        Map<String, Object> chatRoomData = new HashMap<>();
        chatRoomData.put("lastMessage", messageText);
        chatRoomData.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        chatRoomData.put("participants", Arrays.asList(currentUserId, otherUserId));
        db.collection("chats").document(chatRoomId).set(chatRoomData, SetOptions.merge());
    }

    @Override
    public void onEditMessage(ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Mensagem");

        final EditText input = new EditText(this);
        input.setText(message.getText());
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newText)) {
                messagesCollection.document(message.getMessageId())
                    .update("text", newText, "edited", true);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDeleteMessage(ChatMessage message) {
        messagesCollection.document(message.getMessageId())
            .update("text", "Mensagem apagada", "deleted", true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
