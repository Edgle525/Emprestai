package br.edu.fatecgru.empresta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import br.edu.fatecgru.empresta.databinding.ItemSearchUserBinding;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final Context context;

    public SearchUserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchUserBinding binding = ItemSearchUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.binding.userNameSearch.setText(user.getName());

        String photoUrl = user.getPhotoUrl();

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(context)
                 .load(photoUrl)
                 .placeholder(R.mipmap.ic_launcher_round) // Imagem de carregamento
                 .error(R.mipmap.ic_launcher_round) // Imagem em caso de erro
                 .into(holder.binding.userPhotoSearch);
        } else {
            holder.binding.userPhotoSearch.setImageResource(R.mipmap.ic_launcher_round); // Imagem padrão
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchUserBinding binding;

        public UserViewHolder(ItemSearchUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
