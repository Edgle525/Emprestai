package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.FragmentConversationsBinding;

public class ConversationsFragment extends Fragment {

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
    }

    private void loadConversations() {
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    return;
                }

                conversationList.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants == null) continue;

                        String otherUserId = participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);

                        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                Conversation conversation = new Conversation();
                                conversation.setOtherUserId(otherUserId);
                                conversation.setOtherUserName(userDoc.getString("name"));
                                conversation.setOtherUserPhotoUrl(userDoc.getString("photoUrl"));
                                conversation.setLastMessage(doc.getString("lastMessage"));
                                conversation.setLastMessageTimestamp(doc.getDate("lastMessageTimestamp"));

                                conversationList.add(conversation);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.conversations_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            startActivity(new Intent(getActivity(), SearchUserActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
