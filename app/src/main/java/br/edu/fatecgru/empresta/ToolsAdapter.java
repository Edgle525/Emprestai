package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemToolBinding;

public class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.ToolViewHolder> {

    private final List<Tool> toolList;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ToolsAdapter(Context context, List<Tool> toolList) {
        this.context = context;
        this.toolList = toolList;
    }

    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemToolBinding binding = ItemToolBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ToolViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        Tool tool = toolList.get(position);
        holder.binding.toolName.setText(tool.getName());

        if (tool.getImageUrls() != null && !tool.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(tool.getImageUrls().get(0))
                    .into(holder.binding.toolImage);
        }

        if (tool.getOwnerId() != null) {
            db.collection("users").document(tool.getOwnerId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerName = documentSnapshot.getString("name");
                        String ownerPhotoUrl = documentSnapshot.getString("photoUrl");

                        holder.binding.ownerName.setText(ownerName);
                        if (ownerPhotoUrl != null && !ownerPhotoUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(ownerPhotoUrl)
                                    .into(holder.binding.ownerPhoto);
                        }
                    }
                });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ToolDetailActivity.class);
            intent.putExtra("TOOL_ID", tool.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return toolList.size();
    }

    static class ToolViewHolder extends RecyclerView.ViewHolder {
        private final ItemToolBinding binding;

        public ToolViewHolder(ItemToolBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
