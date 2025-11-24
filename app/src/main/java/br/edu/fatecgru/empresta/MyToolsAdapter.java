package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemMyToolBinding;
import br.edu.fatecgru.empresta.databinding.ItemToolImageBinding;

public class MyToolsAdapter extends RecyclerView.Adapter<MyToolsAdapter.ToolViewHolder> {

    private final List<Tool> toolList;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public MyToolsAdapter(Context context, List<Tool> toolList) {
        this.context = context;
        this.toolList = toolList;
    }

    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMyToolBinding binding = ItemMyToolBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ToolViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        Tool tool = toolList.get(position);
        holder.binding.toolNameManage.setText(tool.getName());

        ImageGalleryAdapter imageAdapter = new ImageGalleryAdapter(context, tool.getImageUrls());
        holder.binding.toolImageViewPager.setAdapter(imageAdapter);

        db.collection("loans")
            .whereEqualTo("toolId", tool.getId())
            .whereEqualTo("status", "Aguardando Aprovação")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int requestCount = queryDocumentSnapshots.size();
                if (requestCount > 0) {
                    holder.binding.requestsCount.setText(String.valueOf(requestCount));
                    holder.binding.requestsCount.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.requestsCount.setVisibility(View.GONE);
                }
            });

        holder.binding.editToolButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddToolActivity.class);
            intent.putExtra("EDIT_TOOL_ID", tool.getId());
            context.startActivity(intent);
        });

        holder.binding.deleteToolButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Excluir Ferramenta")
                    .setMessage("Você tem certeza que deseja excluir '" + tool.getName() + "'?")
                    .setPositiveButton("Excluir", (dialog, which) -> deleteTool(tool, position))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LoanRequestsActivity.class);
            intent.putExtra("TOOL_ID", tool.getId());
            context.startActivity(intent);
        });
    }

    private void deleteTool(Tool tool, int position) {
        if (tool.getImageUrls() != null) {
            for (String imageUrl : tool.getImageUrls()) {
                StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                photoRef.delete();
            }
        }

        db.collection("tools").document(tool.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Ferramenta excluída", Toast.LENGTH_SHORT).show();
                    toolList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, toolList.size());
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao excluir", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return toolList.size();
    }

    static class ToolViewHolder extends RecyclerView.ViewHolder {
        private final ItemMyToolBinding binding;

        public ToolViewHolder(ItemMyToolBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // Adapter interno para a galeria de imagens no ViewPager2
    static class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageGalleryViewHolder> {

        private final Context context;
        private final List<String> imageUrls;

        ImageGalleryAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            this.imageUrls = (imageUrls != null) ? imageUrls : new ArrayList<>();
        }

        @NonNull
        @Override
        public ImageGalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemToolImageBinding binding = ItemToolImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ImageGalleryViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageGalleryViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            Glide.with(context).load(imageUrl).into(holder.binding.toolImageItem);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageGalleryViewHolder extends RecyclerView.ViewHolder {
            private final ItemToolImageBinding binding;

            public ImageGalleryViewHolder(ItemToolImageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
