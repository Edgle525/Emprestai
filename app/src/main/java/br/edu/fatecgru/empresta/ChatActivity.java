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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.OnMessageInteractionListener {

    public static final String EXTRA_OTHER_USER_ID = "OTHER_USER_ID";
    private static final String TAG = "ChatActivity";

    private ActivityChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<Object> chatItems;
    private FirebaseFirestore db;
    private DocumentReference chatDocRef;
    private CollectionReference messagesCollection;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;
    private String currentUserPhotoUrl;
    private String chatRoomId;
    private Date conversationDeletedAt = null;

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

        List<String> ids = new ArrayList<>(Arrays.asList(currentUserId, otherUserId));
        Collections.sort(ids);
        chatRoomId = ids.get(0) + "_" + ids.get(1);

        chatDocRef = db.collection("chats").document(chatRoomId);
        messagesCollection = chatDocRef.collection("messages");

        loadInitialData();

        binding.sendButton.setOnClickListener(v -> sendMessage());
        setSupportActionBar(binding.chatToolbar.toolbarChatCustom);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetUnreadCount();
    }

    private void resetUnreadCount() {
        if (chatRoomId != null && currentUserId != null) {
            chatDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    chatDocRef.update("unreadCount." + currentUserId, 0);
                }
            });
        }
    }

    private void loadInitialData() {
        chatDocRef.get().addOnSuccessListener(chatDoc -> {
            if (chatDoc.exists()) {
                Object deletedForObj = chatDoc.get("deletedFor");
                if (deletedForObj instanceof Map) {
                    Map<String, Object> deletedForMap = (Map<String, Object>) deletedForObj;
                    if (deletedForMap.containsKey(currentUserId)) {
                        Object timestampObj = deletedForMap.get(currentUserId);
                        if (timestampObj instanceof Timestamp) {
                            conversationDeletedAt = ((Timestamp) timestampObj).toDate();
                        }
                    }
                }
            }
            loadUsersInfoAndListenMessages();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load chat document.", e);
            loadUsersInfoAndListenMessages();
        });
    }

    private void loadUsersInfoAndListenMessages() {
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
        chatItems = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatItems, otherUserName, otherUserPhotoUrl, currentUserPhotoUrl, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void listenForMessages() {
        Query query = messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING);
        if (conversationDeletedAt != null) {
            query = query.whereGreaterThan("timestamp", conversationDeletedAt);
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        ChatMessage message = dc.getDocument().toObject(ChatMessage.class);
                        message.setMessageId(dc.getDocument().getId());
                        addMessageWithDate(message);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                binding.chatRecyclerView.scrollToPosition(chatItems.size() - 1);
            }
        });
    }

    private void addMessageWithDate(ChatMessage message) {
        Date messageDate = message.getTimestamp();
        if (messageDate == null) return;

        boolean dateAdded = false;
        if (chatItems.isEmpty()) {
            chatItems.add(getFormattedDate(messageDate));
            dateAdded = true;
        } else {
            Object lastItem = chatItems.get(chatItems.size() - 1);
            if (lastItem instanceof ChatMessage) {
                if (!isSameDay(((ChatMessage) lastItem).getTimestamp(), messageDate)) {
                    chatItems.add(getFormattedDate(messageDate));
                    dateAdded = true;
                }
            }
        }

        chatItems.add(message);
    }


    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String getFormattedDate(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTime(date);

        if (today.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR)) {
            return "Hoje";
        }

        today.add(Calendar.DAY_OF_YEAR, -1);
        if (today.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR)) {
            return "Ontem";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private void sendMessage() {
        String messageText = binding.messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        binding.messageInput.setText("");
        binding.sendButton.setEnabled(false);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot chatDoc = transaction.get(chatDocRef);

            if (!chatDoc.exists()) {
                Map<String, Object> chatRoomData = new HashMap<>();
                chatRoomData.put("participants", Arrays.asList(currentUserId, otherUserId));
                chatRoomData.put("deletedFor", new HashMap<>());
                Map<String, Long> unreadCount = new HashMap<>();
                unreadCount.put(currentUserId, 0L);
                unreadCount.put(otherUserId, 0L);
                chatRoomData.put("unreadCount", unreadCount);
                transaction.set(chatDocRef, chatRoomData);
            }

            transaction.update(chatDocRef, "lastMessage", messageText);
            transaction.update(chatDocRef, "lastMessageTimestamp", FieldValue.serverTimestamp());
            transaction.update(chatDocRef, "unreadCount." + otherUserId, FieldValue.increment(1));
            transaction.update(chatDocRef, "unreadCount." + currentUserId, 0);

            DocumentReference newMessageRef = messagesCollection.document();
            ChatMessage chatMessage = new ChatMessage(currentUserId, messageText);
            transaction.set(newMessageRef, chatMessage);

            return null;
        }).addOnSuccessListener(aVoid -> {
            binding.sendButton.setEnabled(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed: ", e);
            Toast.makeText(ChatActivity.this, "Falha ao enviar mensagem.", Toast.LENGTH_SHORT).show();
            binding.messageInput.setText(messageText);
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
