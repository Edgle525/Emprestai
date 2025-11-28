package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
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

        // Exibe o contador de mensagens não lidas
        if (conversation.getUnreadCount() > 0) {
            holder.binding.unreadCount.setVisibility(View.VISIBLE);
            holder.binding.unreadCount.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.binding.unreadCount.setVisibility(View.GONE);
        }

        String photoUrl = conversation.getOtherUserPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(context)
                 .load(photoUrl)
                 .placeholder(R.mipmap.ic_launcher_round)
                 .error(R.mipmap.ic_launcher_round)
                 .into(holder.binding.userPhoto);
        } else {
            holder.binding.userPhoto.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, conversation.getOtherUserId());
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            v.setTag(holder.getAdapterPosition());
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    public Conversation getConversation(int position) {
        if (position >= 0 && position < conversationList.size()) {
            return conversationList.get(position);
        }
        return null;
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private final ItemConversationBinding binding;

        public ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(this.getAdapterPosition(), R.id.delete_conversation, 0, "Excluir conversa");
        }
    }
}
