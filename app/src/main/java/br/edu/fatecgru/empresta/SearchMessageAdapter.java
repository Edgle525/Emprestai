package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemSearchMessageResultBinding;

public class SearchMessageAdapter extends RecyclerView.Adapter<SearchMessageAdapter.SearchResultViewHolder> {

    private final List<ChatMessage> messages;
    private final Context context;

    public SearchMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchMessageResultBinding binding = ItemSearchMessageResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SearchResultViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        // TODO: Get user name from user ID
        holder.binding.userNameSearchMessage.setText(message.getSenderId());
        holder.binding.messageTextSearchMessage.setText(message.getText());

        holder.itemView.setOnClickListener(v -> {
            // TODO: Implement navigation to chat
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchMessageResultBinding binding;

        public SearchResultViewHolder(ItemSearchMessageResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
