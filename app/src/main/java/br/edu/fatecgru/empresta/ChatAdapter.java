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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE = 3;

    private final List<Object> chatItems;
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

    public ChatAdapter(Context context, List<Object> chatItems, String otherUserName, String otherUserPhotoUrl, String currentUserPhotoUrl, OnMessageInteractionListener listener) {
        this.context = context;
        this.chatItems = chatItems;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.otherUserName = otherUserName;
        this.otherUserPhotoUrl = otherUserPhotoUrl;
        this.currentUserPhotoUrl = currentUserPhotoUrl;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = chatItems.get(position);
        if (item instanceof ChatMessage) {
            if (((ChatMessage) item).getSenderId().equals(currentUserId)) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
            }
        } else {
            return VIEW_TYPE_DATE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
            return new MessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
            return new MessageViewHolder(view);
        } else { // VIEW_TYPE_DATE
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_date, parent, false);
            return new DateViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_DATE) {
            DateViewHolder dateViewHolder = (DateViewHolder) holder;
            dateViewHolder.dateTextView.setText((String) chatItems.get(position));
        } else { // Message View
            MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
            ChatMessage message = (ChatMessage) chatItems.get(position);

            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                messageViewHolder.messageTime.setText(sdf.format(message.getTimestamp()));
            }

            if (message.isDeleted()) {
                messageViewHolder.messageText.setText("Mensagem apagada");
                messageViewHolder.messageText.setTypeface(null, Typeface.ITALIC);
                messageViewHolder.itemView.setLongClickable(false);
                if (messageViewHolder.senderPhoto != null) messageViewHolder.senderPhoto.setVisibility(View.INVISIBLE);
                if (messageViewHolder.senderName != null) messageViewHolder.senderName.setVisibility(View.INVISIBLE);
                TextView editedIndicator = messageViewHolder.itemView.findViewById(R.id.edited_indicator);
                if (editedIndicator != null) editedIndicator.setVisibility(View.GONE);

            } else {
                messageViewHolder.messageText.setText(message.getText());
                messageViewHolder.messageText.setTypeface(null, Typeface.NORMAL);
                messageViewHolder.itemView.setLongClickable(true);
                if (messageViewHolder.senderPhoto != null) messageViewHolder.senderPhoto.setVisibility(View.VISIBLE);
                if (messageViewHolder.senderName != null) messageViewHolder.senderName.setVisibility(View.VISIBLE);

                if (viewType == VIEW_TYPE_SENT) {
                    messageViewHolder.senderName.setText("Você");
                    if (currentUserPhotoUrl != null && !currentUserPhotoUrl.isEmpty()) {
                        Glide.with(context).load(currentUserPhotoUrl).into(messageViewHolder.senderPhoto);
                    }

                    messageViewHolder.itemView.setOnLongClickListener(v -> {
                        new AlertDialog.Builder(v.getContext())
                                .setItems(new CharSequence[]{"Editar", "Apagar"}, (dialog, which) -> {
                                    if (which == 0) listener.onEditMessage(message);
                                    else listener.onDeleteMessage(message);
                                }).show();
                        return true;
                    });

                    TextView editedIndicator = messageViewHolder.itemView.findViewById(R.id.edited_indicator);
                    if (editedIndicator != null) {
                        editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
                    }

                } else { // RECEIVED
                    messageViewHolder.senderName.setText(otherUserName);
                    if (otherUserPhotoUrl != null && !otherUserPhotoUrl.isEmpty()) {
                        Glide.with(context).load(otherUserPhotoUrl).into(messageViewHolder.senderPhoto);
                    }

                    TextView editedIndicator = messageViewHolder.itemView.findViewById(R.id.edited_indicator_received);
                    if (editedIndicator != null) {
                        editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        CircleImageView senderPhoto;
        TextView senderName;
        TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            senderPhoto = itemView.findViewById(R.id.sender_photo);
            senderName = itemView.findViewById(R.id.sender_name);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }
}
