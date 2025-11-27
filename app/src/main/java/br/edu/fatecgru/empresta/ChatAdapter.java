package br.edu.fatecgru.empresta;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> chatMessages;
    private final String currentUserId;
    private final String otherUserName;
    private final String otherUserPhotoUrl;
    private final String currentUserPhotoUrl;
    private final OnMessageInteractionListener listener;
    private final Context context;

    public interface OnMessageInteractionListener {
        void onEditMessage(ChatMessage message);
        void onDeleteMessage(ChatMessage message);
    }

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String otherUserName, String otherUserPhotoUrl, String currentUserPhotoUrl, OnMessageInteractionListener listener) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.otherUserName = otherUserName;
        this.otherUserPhotoUrl = otherUserPhotoUrl;
        this.currentUserPhotoUrl = currentUserPhotoUrl;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        if (message.isDeleted()) {
            holder.messageText.setText("Mensagem apagada");
            holder.messageText.setTypeface(null, Typeface.ITALIC);
            holder.itemView.setLongClickable(false);
            if (holder.senderPhoto != null) holder.senderPhoto.setVisibility(View.INVISIBLE);
            if (holder.senderName != null) holder.senderName.setVisibility(View.INVISIBLE);

            TextView editedIndicatorSent = holder.itemView.findViewById(R.id.edited_indicator);
            if (editedIndicatorSent != null) editedIndicatorSent.setVisibility(View.GONE);
            TextView editedIndicatorReceived = holder.itemView.findViewById(R.id.edited_indicator_received);
            if (editedIndicatorReceived != null) editedIndicatorReceived.setVisibility(View.GONE);

        } else {
            holder.messageText.setText(message.getText());
            holder.messageText.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setLongClickable(true);
            if (holder.senderPhoto != null) holder.senderPhoto.setVisibility(View.VISIBLE);
            if (holder.senderName != null) holder.senderName.setVisibility(View.VISIBLE);

            if (getItemViewType(position) == VIEW_TYPE_SENT) {
                holder.senderName.setText("Você");
                if (currentUserPhotoUrl != null && !currentUserPhotoUrl.isEmpty()) {
                    Glide.with(context).load(currentUserPhotoUrl).into(holder.senderPhoto);
                }

                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(v.getContext())
                        .setItems(new CharSequence[]{"Editar", "Apagar"}, (dialog, which) -> {
                            if (which == 0) listener.onEditMessage(message);
                            else listener.onDeleteMessage(message);
                        }).show();
                    return true;
                });

                TextView editedIndicator = holder.itemView.findViewById(R.id.edited_indicator);
                if (editedIndicator != null) {
                    editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
                }

            } else { // RECEIVED
                holder.senderName.setText(otherUserName);
                if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
                    Glide.with(context).load(otherUserPhotoUrl).into(holder.senderPhoto);
                }

                TextView editedIndicator = holder.itemView.findViewById(R.id.edited_indicator_received);
                if (editedIndicator != null) {
                    editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        CircleImageView senderPhoto;
        TextView senderName;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            senderPhoto = itemView.findViewById(R.id.sender_photo);
            senderName = itemView.findViewById(R.id.sender_name);
        }
    }
}
