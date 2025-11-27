package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        registerForContextMenu(binding.conversationsRecyclerView);
    }

    private void setupRecyclerView() {
        adapter = new ConversationsAdapter(getContext(), conversationList);
        binding.conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.conversationsRecyclerView.setAdapter(adapter);
    }

    private void loadConversations() {
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error);
                    return;
                }

                if (snapshots == null) return;

                List<DocumentSnapshot> validChatDocs = new ArrayList<>();
                List<String> userIdsToFetch = new ArrayList<>();

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    List<String> deletedFor = (List<String>) doc.get("deletedFor");
                    if (deletedFor != null && deletedFor.contains(currentUserId)) {
                        continue; // Skip conversations deleted by the current user
                    }
                    validChatDocs.add(doc);
                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants != null) {
                        for (String participantId : participants) {
                            if (!participantId.equals(currentUserId) && !userIdsToFetch.contains(participantId)) {
                                userIdsToFetch.add(participantId);
                            }
                        }
                    }
                }

                if (userIdsToFetch.isEmpty()) {
                    conversationList.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Fetch user details for all conversations at once
                db.collection("users").whereIn("uid", userIdsToFetch).get()
                    .addOnSuccessListener(userSnapshots -> {
                        Map<String, DocumentSnapshot> userMap = userSnapshots.getDocuments().stream()
                                .collect(Collectors.toMap(DocumentSnapshot::getId, userDoc -> userDoc));

                        List<Conversation> newConversationList = new ArrayList<>();
                        for (DocumentSnapshot chatDoc : validChatDocs) {
                            List<String> participants = (List<String>) chatDoc.get("participants");
                            if (participants == null) continue;

                            String otherUserId = participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);
                            DocumentSnapshot userDoc = userMap.get(otherUserId);

                            if (userDoc != null && userDoc.exists()) {
                                Conversation conversation = new Conversation();
                                conversation.setChatId(chatDoc.getId());
                                conversation.setOtherUserId(otherUserId);
                                conversation.setOtherUserName(userDoc.getString("name"));
                                conversation.setOtherUserPhotoUrl(userDoc.getString("photoUrl"));
                                conversation.setLastMessage(chatDoc.getString("lastMessage"));
                                conversation.setLastMessageTimestamp(chatDoc.getDate("lastMessageTimestamp"));
                                newConversationList.add(conversation);
                            }
                        }

                        // Sort by timestamp again as the order might be lost
                        Collections.sort(newConversationList, (c1, c2) -> c2.getLastMessageTimestamp().compareTo(c1.getLastMessageTimestamp()));

                        conversationList.clear();
                        conversationList.addAll(newConversationList);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
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
                Conversation conversation = adapter.getConversation(position);
                if (conversation != null) {
                    deleteConversation(conversation.getChatId());
                }
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void deleteConversation(String chatId) {
        db.collection("chats").document(chatId)
            .update("deletedFor", FieldValue.arrayUnion(currentUserId))
            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Conversa excluída", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao excluir conversa", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
