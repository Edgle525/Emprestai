package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.FragmentConversationsBinding;

public class ConversationsFragment extends Fragment {

    private static final String TAG = "ConversationsFragment";
    private FragmentConversationsBinding binding;
    private ConversationsAdapter adapter;
    private final List<Conversation> conversationList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentConversationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
            loadConversations();
        }
    }

    private void setupRecyclerView() {
        adapter = new ConversationsAdapter(getContext(), conversationList);
        binding.conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.conversationsRecyclerView.setAdapter(adapter);
        registerForContextMenu(binding.conversationsRecyclerView);
    }

    private void loadConversations() {
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error);
                    return;
                }

                if (snapshots == null) {
                    conversationList.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                List<Conversation> tempConversations = new ArrayList<>();
                List<String> otherUserIds = new ArrayList<>();

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Object deletedForObj = doc.get("deletedFor");
                    Date lastMessageTimestamp = doc.getDate("lastMessageTimestamp");

                    boolean shouldHide = false;
                    if (deletedForObj instanceof Map) {
                        Map<String, Object> deletedForMap = (Map<String, Object>) deletedForObj;
                        if (deletedForMap.containsKey(currentUserId)) {
                            Object timestampObj = deletedForMap.get(currentUserId);
                            if (timestampObj instanceof Timestamp) {
                                Date deletedAt = ((Timestamp) timestampObj).toDate();
                                if (lastMessageTimestamp == null || lastMessageTimestamp.before(deletedAt)) {
                                    shouldHide = true;
                                }
                            } else { 
                                shouldHide = true;
                            }
                        }
                    }

                    if (shouldHide) {
                        continue;
                    }

                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants == null) continue;
                    
                    String otherUserId = "";
                    for(String id : participants){
                        if(!id.equals(currentUserId)){
                            otherUserId = id;
                            break;
                        }
                    }
                    if(otherUserId.isEmpty()) continue;

                    Conversation conversation = new Conversation();
                    conversation.setChatId(doc.getId());
                    conversation.setOtherUserId(otherUserId);
                    conversation.setLastMessage(doc.getString("lastMessage"));

                    if (lastMessageTimestamp != null) {
                        conversation.setLastMessageTimestamp(lastMessageTimestamp);
                    } else {
                        conversation.setLastMessageTimestamp(new Date(0));
                    }

                    Map<String, Long> unreadCountMap = (Map<String, Long>) doc.get("unreadCount");
                    if (unreadCountMap != null && unreadCountMap.get(currentUserId) != null) {
                        conversation.setUnreadCount(unreadCountMap.get(currentUserId));
                    } else {
                        conversation.setUnreadCount(0);
                    }

                    if (!otherUserIds.contains(otherUserId)) {
                        otherUserIds.add(otherUserId);
                    }
                    tempConversations.add(conversation);
                }

                if (otherUserIds.isEmpty()) {
                    conversationList.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                db.collection("users").whereIn(FieldPath.documentId(), otherUserIds)
                    .get()
                    .addOnSuccessListener(userSnapshots -> {
                        Map<String, DocumentSnapshot> userMap = new HashMap<>();
                        for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                            userMap.put(userDoc.getId(), userDoc);
                        }

                        for (Conversation conv : tempConversations) {
                            DocumentSnapshot userDoc = userMap.get(conv.getOtherUserId());
                            if (userDoc != null) {
                                conv.setOtherUserName(userDoc.getString("name"));
                                conv.setOtherUserPhotoUrl(userDoc.getString("photoUrl"));
                            }
                        }
                        
                        Collections.sort(tempConversations, (c1, c2) -> c2.getLastMessageTimestamp().compareTo(c1.getLastMessageTimestamp()));
                        
                        conversationList.clear();
                        conversationList.addAll(tempConversations);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user details", e);
                    });
            });
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.conversations_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search_user) {
            startActivity(new Intent(getActivity(), SearchUserActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_conversation) {
            int position = item.getGroupId();
            if (position >= 0 && position < conversationList.size()) {
                Conversation conversation = conversationList.get(position);
                if (conversation != null) {
                    deleteConversation(conversation.getChatId());
                }
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void deleteConversation(String chatId) {
        Map<String, Object> deleteUpdate = new HashMap<>();
        deleteUpdate.put("deletedFor." + currentUserId, FieldValue.serverTimestamp());

        db.collection("chats").document(chatId)
            .update(deleteUpdate)
            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Conversa excluída", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao excluir conversa", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
