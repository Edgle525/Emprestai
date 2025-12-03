package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {

    private final Context context;
    private final List<Conversation> conversationList;

    public ConversationsAdapter(Context context, List<Conversation> conversationList) {
        this.context = context;
        this.conversationList = conversationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.userName.setText(conversation.getOtherUserName());
        holder.lastMessage.setText(conversation.getLastMessage());

        if (conversation.getOtherUserPhotoUrl() != null && !conversation.getOtherUserPhotoUrl().isEmpty()) {
            Glide.with(context).load(conversation.getOtherUserPhotoUrl()).into(holder.userImage);
        } else {
            holder.userImage.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, conversation.getOtherUserId());
            context.startActivity(intent);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView userName;
        public TextView lastMessage;
        public CircleImageView userImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.conversation_user_name);
            lastMessage = itemView.findViewById(R.id.conversation_last_message);
            userImage = itemView.findViewById(R.id.conversation_user_image);
        }
    }
}
