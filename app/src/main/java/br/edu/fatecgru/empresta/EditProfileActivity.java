package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityEditProfileBinding;
import com.santalu.maskara.Mask;
import com.santalu.maskara.MaskChangedListener;
import com.santalu.maskara.MaskStyle;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Apply masks
        Mask cpfMask = new Mask("___.___.___-__", '_', MaskStyle.NORMAL);
        Mask phoneMask = new Mask("(__) _____-____", '_', MaskStyle.NORMAL);
        binding.editCpf.addTextChangedListener(new MaskChangedListener(cpfMask));
        binding.editPhone.addTextChangedListener(new MaskChangedListener(phoneMask));

        loadUserProfile();

        binding.saveProfileButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String cpf = documentSnapshot.getString("cpf");
                String phone = documentSnapshot.getString("phone");

                binding.editFullName.setText(name);
                binding.editEmail.setText("E-mail: " + email);

                if (cpf != null && !cpf.isEmpty()) {
                    binding.editCpf.setText(cpf);
                    binding.editCpf.setEnabled(false); // CPF is not editable
                } else {
                    binding.layoutCpf.setVisibility(View.VISIBLE);
                }

                if (phone != null && !phone.isEmpty()) {
                    binding.editPhone.setText(phone);
                } else {
                    binding.layoutPhone.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void saveProfileChanges() {
        String newName = binding.editFullName.getText().toString().trim();
        String newCpf = binding.editCpf.getText().toString().trim();
        String newPhone = binding.editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newCpf) || TextUtils.isEmpty(newPhone)) {
            Toast.makeText(this, "Preencha todos os campos para continuar", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference docRef = db.collection("users").document(user.getUid());

        // Check if CPF is unique before saving
        db.collection("users").whereEqualTo("cpf", newCpf).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Check if the CPF belongs to the current user
                        String docId = task.getResult().getDocuments().get(0).getId();
                        if (!docId.equals(user.getUid())) {
                            Toast.makeText(this, "Este CPF já está em uso por outra conta.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // CPF is unique or belongs to the current user, proceed with saving
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", newName);
                    updates.put("cpf", newCpf);
                    updates.put("phone", newPhone);

                    docRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Falha ao atualizar o perfil", Toast.LENGTH_SHORT).show());
                });
    }
}
