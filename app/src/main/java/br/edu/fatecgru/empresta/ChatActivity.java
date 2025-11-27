package br.edu.fatecgru.empresta;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
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
    private static final String TAG = "ChatActivity";

    private ActivityChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private CollectionReference messagesCollection;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;
    private String currentUserPhotoUrl;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);

        if (otherUserId == null) {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        loadUsersInfo();

        List<String> ids = new ArrayList<>(Arrays.asList(currentUserId, otherUserId));
        Collections.sort(ids);
        chatRoomId = ids.get(0) + "_" + ids.get(1);

        messagesCollection = db.collection("chats").document(chatRoomId).collection("messages");

        binding.sendButton.setOnClickListener(v -> sendMessage());
        setSupportActionBar(binding.chatToolbar.toolbarChatCustom);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void loadUsersInfo() {
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);
        DocumentReference otherUserRef = db.collection("users").document(otherUserId);

        currentUserRef.get().addOnSuccessListener(currentUserDoc -> {
            if (currentUserDoc.exists()) {
                currentUserPhotoUrl = currentUserDoc.getString("photoUrl");
            }

            otherUserRef.get().addOnSuccessListener(otherUserDoc -> {
                if (otherUserDoc.exists()) {
                    otherUserName = otherUserDoc.getString("name");
                    otherUserPhotoUrl = otherUserDoc.getString("photoUrl");
                }

                binding.chatToolbar.chatUserName.setText(otherUserName);
                if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
                    Glide.with(this).load(otherUserPhotoUrl).into(binding.chatToolbar.chatUserPhoto);
                }

                setupRecyclerView();
                listenForMessages();
            });
        });
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages, otherUserName, otherUserPhotoUrl, currentUserPhotoUrl, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void listenForMessages() {
        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error);
                    return;
                }

                if (snapshots != null) {
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        ChatMessage message = dc.getDocument().toObject(ChatMessage.class);
                        message.setMessageId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                if (!chatMessages.stream().anyMatch(m -> m.getMessageId().equals(message.getMessageId()))) {
                                    chatMessages.add(message);
                                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                                    binding.chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                                }
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
                }
            });
    }

    private void sendMessage() {
        String messageText = binding.messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        binding.messageInput.setText("");
        binding.sendButton.setEnabled(false);

        // Primeiro, crie ou atualize o documento de chat
        Map<String, Object> chatRoomData = new HashMap<>();
        chatRoomData.put("lastMessage", messageText);
        chatRoomData.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        chatRoomData.put("participants", Arrays.asList(currentUserId, otherUserId));
        // Garante que o campo deletedFor exista
        chatRoomData.put("deletedFor", Collections.emptyList());

        db.collection("chats").document(chatRoomId).set(chatRoomData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // Se a atualização do chat for bem-sucedida, envie a mensagem
                ChatMessage chatMessage = new ChatMessage(currentUserId, messageText);
                messagesCollection.add(chatMessage)
                    .addOnSuccessListener(documentReference -> {
                        // Mensagem enviada com sucesso
                        binding.sendButton.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        // Falha ao enviar a mensagem
                        Log.e(TAG, "Error sending message", e);
                        Toast.makeText(ChatActivity.this, "Falha ao enviar mensagem.", Toast.LENGTH_SHORT).show();
                        binding.messageInput.setText(messageText); // Restaura o texto
                        binding.sendButton.setEnabled(true);
                    });
            })
            .addOnFailureListener(e -> {
                // Falha ao criar/atualizar o documento do chat
                Log.e(TAG, "Error updating chat document", e);
                Toast.makeText(ChatActivity.this, "Falha ao iniciar o chat.", Toast.LENGTH_SHORT).show();
                binding.messageInput.setText(messageText); // Restaura o texto
                binding.sendButton.setEnabled(true);
            });
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
