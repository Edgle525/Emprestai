package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemConversationBinding;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private final List<Conversation> conversationList;
    private final Context context;

    public ConversationsAdapter(Context context, List<Conversation> conversationList) {
        this.context = context;
        this.conversationList = conversationList;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);

        holder.binding.userName.setText(conversation.getOtherUserName());
        holder.binding.lastMessage.setText(conversation.getLastMessage());

        if (conversation.getOtherUserPhotoUrl() != null && !conversation.getOtherUserPhotoUrl().isEmpty()) {
            Glide.with(context).load(conversation.getOtherUserPhotoUrl()).into(holder.binding.userPhoto);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, conversation.getOtherUserId());
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, conversation.getOtherUserName());
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_PHOTO_URL, conversation.getOtherUserPhotoUrl()); // Corrigido
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        public ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
