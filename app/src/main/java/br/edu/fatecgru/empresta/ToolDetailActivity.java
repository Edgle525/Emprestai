package br.edu.fatecgru.empresta;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.ActivityToolDetailBinding;

public class ToolDetailActivity extends BaseActivity {

    private ActivityToolDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String toolId;
    private Tool currentTool;
    private User owner;
    private User borrower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityToolDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        toolId = getIntent().getStringExtra("TOOL_ID");

        binding.borrowButton.setVisibility(View.GONE); // Ocultar por padrão

        if (toolId != null) {
            loadToolDetails();
        }

        binding.borrowButton.setOnClickListener(v -> {
            if (borrower == null || TextUtils.isEmpty(borrower.getAddress())) {
                showProfileCompletionDialog();
            } else if (currentTool != null && owner != null) {
                showPickupDatePicker();
            } else {
                Toast.makeText(this, "Carregando dados... Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadToolDetails() {
        db.collection("tools").document(toolId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentTool = documentSnapshot.toObject(Tool.class);
                    if (currentTool != null) {
                        currentTool.setId(documentSnapshot.getId());
                        updateUIWithToolDetails();
                        if (currentTool.getOwnerId() != null) {
                            loadOwnerAndBorrowerDetails(currentTool.getOwnerId());
                        }
                    }
                } else {
                    Toast.makeText(this, "Ferramenta não encontrada.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }

    private void updateUIWithToolDetails() {
        binding.toolNameDetail.setText(currentTool.getName());
        binding.toolBrandDetail.setText(currentTool.getBrand());
        binding.toolDescriptionDetail.setText(currentTool.getDescription());

        if (currentTool.getImageUrls() != null && !currentTool.getImageUrls().isEmpty()) {
            ToolImagePagerAdapter adapter = new ToolImagePagerAdapter(this, currentTool.getImageUrls());
            binding.toolImagePager.setAdapter(adapter);
        }
    }

    private void loadOwnerAndBorrowerDetails(String ownerId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (ownerId.equals(currentUser.getUid())) {
            binding.borrowButton.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(ownerId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    owner = documentSnapshot.toObject(User.class);
                    if (owner != null) {
                        owner.setUid(documentSnapshot.getId());
                        updateUIWithOwnerDetails();

                        db.collection("users").document(currentUser.getUid()).get()
                            .addOnSuccessListener(borrowerSnapshot -> {
                                if (borrowerSnapshot.exists()) {
                                    borrower = borrowerSnapshot.toObject(User.class);
                                    if (borrower != null) {
                                        borrower.setUid(borrowerSnapshot.getId());
                                        binding.borrowButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                    }
                }
            });
    }

    private void updateUIWithOwnerDetails() {
        binding.ownerNameDetail.setText(owner.getName());
        if (owner.getRating() != null) {
            binding.ownerRatingBar.setRating(owner.getRating().floatValue());
        }

        if (owner.getPhotoUrl() != null && !owner.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(owner.getPhotoUrl()).placeholder(R.mipmap.ic_launcher_round).error(R.mipmap.ic_launcher_round).into(binding.ownerPhotoDetail);
        } else {
            binding.ownerPhotoDetail.setImageResource(R.mipmap.ic_launcher_round);
        }

        binding.chatButtonDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, owner.getUid());
            startActivity(intent);
        });
    }

    private void showPickupDatePicker() {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar pickupDate = Calendar.getInstance();
                    pickupDate.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                    pickupDate.set(Calendar.MILLISECOND, 0);

                    showReturnDatePicker(pickupDate.getTime());
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.setTitle("Selecione a data de retirada");
        datePickerDialog.show();
    }

    private void showReturnDatePicker(final Date pickupDate) {
        final Calendar c = Calendar.getInstance();
        c.setTime(pickupDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar returnDate = Calendar.getInstance();
                    returnDate.set(year, monthOfYear, dayOfMonth, 23, 59, 59);
                    returnDate.set(Calendar.MILLISECOND, 999);

                    createLoanRequest(pickupDate, returnDate.getTime());
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(pickupDate.getTime());
        datePickerDialog.setTitle("Selecione a data de devolução");
        datePickerDialog.show();
    }

    private void createLoanRequest(Date pickupDate, Date expectedReturnDate) {
        DocumentReference loanRef = db.collection("loans").document();

        Loan loan = new Loan();
        loan.setId(loanRef.getId());
        loan.setToolId(currentTool.getId());
        loan.setOwnerId(owner.getUid());
        loan.setBorrowerId(borrower.getUid());

        loan.setToolName(currentTool.getName());
        if (currentTool.getImageUrls() != null && !currentTool.getImageUrls().isEmpty()) {
            loan.setToolImageUrl(currentTool.getImageUrls().get(0));
        }
        loan.setOwnerName(owner.getName());
        loan.setBorrowerName(borrower.getName());

        loan.setRequestDate(new Date());
        loan.setPickupDate(pickupDate);
        loan.setExpectedReturnDate(expectedReturnDate);
        loan.setStatusEnum(Loan.Status.REQUESTED);

        loanRef.set(loan)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Solicitação de empréstimo enviada.", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Falha ao enviar solicitação: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    
    private void showProfileCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Perfil Incompleto")
                .setMessage("Para solicitar um empréstimo, por favor, complete seu perfil com um endereço válido.")
                .setPositiveButton("Completar Perfil", (dialog, which) -> {
                    Intent intent = new Intent(this, EditProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static class ToolImagePagerAdapter extends RecyclerView.Adapter<ToolImagePagerAdapter.ImageViewHolder> {

        private final List<String> imageUrls;
        private final Context context;

        ToolImagePagerAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Glide.with(context)
                    .load(imageUrls.get(position))
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls != null ? imageUrls.size() : 0;
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageViewHolder(@NonNull ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }
}
