package br.edu.fatecgru.empresta.ui.perfil;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import br.edu.fatecgru.empresta.R; // Import a ser adicionado
import br.edu.fatecgru.empresta.LoginActivity;
import br.edu.fatecgru.empresta.databinding.FragmentPerfilBinding;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelection(imageUri);
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

        loadUserProfile();

        binding.logoutButton.setOnClickListener(v -> logoutUser());
        binding.profileImage.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void handleImageSelection(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            Bitmap rotatedBitmap = getRotatedBitmap(imageUri, bitmap);
            String base64Image = bitmapToBase64(rotatedBitmap);
            saveImageStringToFirestore(base64Image);
            displayImage(rotatedBitmap);

        } catch (IOException e) {
            Toast.makeText(getContext(), "Falha ao carregar a imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getRotatedBitmap(Uri imageUri, Bitmap bitmap) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
        ExifInterface exifInterface = new ExifInterface(inputStream);
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        inputStream.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void saveImageStringToFirestore(String base64Image) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("profileImageBase64", base64Image)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao salvar a foto.", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (binding != null && task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                binding.profileName.setText(document.getString("name"));
                                binding.profileEmail.setText(document.getString("email"));

                                String base64Image = document.getString("profileImageBase64");
                                if (base64Image != null && !base64Image.isEmpty()) {
                                    Bitmap bitmap = base64ToBitmap(base64Image);
                                    displayImage(bitmap);
                                } else {
                                    displayPlaceholder();
                                }
                            } else {
                                Toast.makeText(getContext(), "Dados do usuário não encontrados.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (binding != null) {
                            Toast.makeText(getContext(), "Falha ao buscar dados: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    private void displayImage(Bitmap bitmap) {
        binding.profileImage.setImageBitmap(bitmap);
        binding.profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        binding.profileImage.setPadding(0, 0, 0, 0);
        binding.profileImage.setBackground(null);
    }

    private void displayPlaceholder() {
        binding.profileImage.setImageResource(android.R.drawable.ic_menu_camera);
        binding.profileImage.setBackgroundResource(R.drawable.circular_background);
        binding.profileImage.setPadding(28, 28, 28, 28);
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
