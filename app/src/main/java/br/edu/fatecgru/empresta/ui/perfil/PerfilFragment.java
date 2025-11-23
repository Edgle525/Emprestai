package br.edu.fatecgru.empresta.ui.perfil;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import br.edu.fatecgru.empresta.MyToolsActivity;
import br.edu.fatecgru.empresta.R;
import br.edu.fatecgru.empresta.LoginActivity;
import br.edu.fatecgru.empresta.databinding.FragmentPerfilBinding;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadImageToStorage(imageUri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        loadUserProfile();

        binding.logoutButton.setOnClickListener(v -> logoutUser());
        binding.manageToolsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyToolsActivity.class);
            startActivity(intent);
        });
        binding.profileImageCard.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadImageToStorage(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Enviando imagem...", Toast.LENGTH_SHORT).show();

        StorageReference fileRef = storageReference.child("profile_images/" + user.getUid());

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveImageUrlToFirestore(imageUrl);
                }))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Falha no upload: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("profileImageUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
                        if (binding != null) {
                            displayImage(imageUrl);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao salvar URL da foto.", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(task -> {
            if (binding == null) return; // Fragment was destroyed

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    binding.profileName.setText(document.getString("name"));
                    binding.profileEmail.setText(document.getString("email"));

                    String imageUrl = document.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        displayImage(imageUrl);
                    } else {
                        displayPlaceholder();
                    }
                } else {
                    Toast.makeText(getContext(), "Dados do usuário não encontrados.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Falha ao buscar dados: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .into(binding.profileImage);
        binding.profileImage.setPadding(0, 0, 0, 0);
    }

    private void displayPlaceholder() {
        binding.profileImage.setImageResource(android.R.drawable.ic_menu_camera);
        binding.profileImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        binding.profileImage.setPadding(28,28,28,28);
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
