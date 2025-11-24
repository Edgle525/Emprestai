package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.TimeUnit;

import br.edu.fatecgru.empresta.databinding.ActivityToolDetailBinding;

public class ToolDetailActivity extends AppCompatActivity {

    private ActivityToolDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String toolId;
    private Tool currentTool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityToolDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        toolId = getIntent().getStringExtra("TOOL_ID");

        if (toolId != null) {
            loadToolDetails();
        }

        binding.borrowButton.setOnClickListener(v -> showModernBorrowDialog());
    }

    private void loadToolDetails() {
        db.collection("tools").document(toolId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentTool = documentSnapshot.toObject(Tool.class);
                    if (currentTool != null) {
                        currentTool.setId(documentSnapshot.getId());
                        binding.toolNameDetail.setText(currentTool.getName());
                        binding.toolBrandDetail.setText(currentTool.getBrand());
                        binding.toolDescriptionDetail.setText(currentTool.getDescription());

                        if (currentTool.getImageUrls() != null && !currentTool.getImageUrls().isEmpty()) {
                            ToolImagePagerAdapter adapter = new ToolImagePagerAdapter(currentTool.getImageUrls());
                            binding.toolImagePager.setAdapter(adapter);
                        }

                        if (currentTool.getOwnerId() != null) {
                            loadOwnerDetails(currentTool.getOwnerId());
                        }
                    }
                }
            });
    }

    private void loadOwnerDetails(String ownerId) {
        db.collection("users").document(ownerId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    String photoUrl = documentSnapshot.getString("photoUrl");
                    Double rating = documentSnapshot.getDouble("rating");

                    binding.ownerNameDetail.setText(name);
                    if (rating != null) {
                        binding.ownerRatingBar.setRating(rating.floatValue());
                    }

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this).load(photoUrl).into(binding.ownerPhotoDetail);
                    }

                    binding.chatButtonDetail.setOnClickListener(v -> {
                        Intent intent = new Intent(this, ChatActivity.class);
                        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, ownerId);
                        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, name);
                        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_PHOTO_URL, photoUrl); // Corrigido
                        startActivity(intent);
                    });
                }
            });
    }

    private void showModernBorrowDialog() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Selecione o período do empréstimo");
        builder.setCalendarConstraints(
            new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build());

        final MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();
        datePicker.show(getSupportFragmentManager(), datePicker.toString());

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                long diffInMillis = selection.second - selection.first;
                long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;

                String borrowingTime = diffInDays + (diffInDays > 1 ? " dias" : " dia");
                createLoanRequest(borrowingTime);
            }
        });
    }

    private void createLoanRequest(String borrowingTime) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Você precisa estar logado para solicitar.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTool == null) {
             Toast.makeText(this, "Não foi possível carregar os detalhes da ferramenta.", Toast.LENGTH_SHORT).show();
            return;
        }

        Loan loan = new Loan();
        loan.setToolId(currentTool.getId());
        loan.setOwnerId(currentTool.getOwnerId());
        loan.setBorrowerId(currentUser.getUid());
        loan.setStatus("Aguardando Aprovação");
        loan.setBorrowingTime(borrowingTime);

        db.collection("loans").add(loan)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Solicitação de empréstimo enviada.", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Falha ao enviar solicitação.", Toast.LENGTH_SHORT).show());
    }


    private static class ToolImagePagerAdapter extends RecyclerView.Adapter<ToolImagePagerAdapter.ImageViewHolder> {

        private final List<String> imageUrls;

        ToolImagePagerAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrls.get(position))
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
