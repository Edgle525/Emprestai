package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityRegisterBinding;
import com.santalu.maskara.Mask;
import com.santalu.maskara.MaskChangedListener;
import com.santalu.maskara.MaskStyle;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Apply masks
        Mask cpfMask = new Mask("___.___.___-__", '_', MaskStyle.NORMAL);
        Mask phoneMask = new Mask("(__) _____-____", '_', MaskStyle.NORMAL);
        binding.cpf.addTextChangedListener(new MaskChangedListener(cpfMask));
        binding.phone.addTextChangedListener(new MaskChangedListener(phoneMask));

        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        if (!validateFields()) {
            return;
        }

        showLoading(true);

        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String cpf = binding.cpf.getText().toString().trim();

        db.collection("users").whereEqualTo("cpf", cpf).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        binding.cpf.setError("CPF já cadastrado.");
                        showLoading(false);
                    } else {
                        // CPF is unique, proceed with Firebase Auth registration
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, authTask -> {
                                    if (authTask.isSuccessful()) {
                                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                        if (firebaseUser != null) {
                                            saveUserToFirestore(firebaseUser.getUid());
                                        }
                                    } else {
                                        showLoading(false);
                                        try {
                                            throw authTask.getException();
                                        } catch (FirebaseAuthWeakPasswordException e) {
                                            binding.passwordLayout.setError("A senha é muito fraca. Use no mínimo 6 caracteres.");
                                        } catch (FirebaseAuthInvalidCredentialsException e) {
                                            binding.email.setError("O e-mail informado é inválido.");
                                        } catch (FirebaseAuthUserCollisionException e) {
                                            binding.email.setError("Este e-mail já está em uso.");
                                        } catch (Exception e) {
                                            Toast.makeText(this, "Falha no cadastro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                });
    }

    private boolean validateFields() {
        // Reset errors
        binding.fullName.setError(null);
        binding.cpf.setError(null);
        binding.phone.setError(null);
        binding.email.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);

        boolean isValid = true;

        String fullName = binding.fullName.getText().toString().trim();
        String cpf = binding.cpf.getText().toString().trim();
        String phone = binding.phone.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String confirmPassword = binding.confirmPassword.getText().toString().trim();
        String unmaskedPhone = phone.replaceAll("[^\\d]", "");
        String unmaskedCpf = cpf.replaceAll("[^\\d]", "");

        if (TextUtils.isEmpty(fullName)) {
            binding.fullName.setError("O nome completo é obrigatório.");
            isValid = false;
        } else if (fullName.length() < 3) {
            binding.fullName.setError("O nome completo deve ter no mínimo 3 caracteres.");
            isValid = false;
        } else if (!fullName.matches("[a-zA-Z ]+")) {
            binding.fullName.setError("O nome deve conter apenas letras e espaços.");
            isValid = false;
        }

        if (TextUtils.isEmpty(cpf)) {
            binding.cpf.setError("O CPF é obrigatório.");
            isValid = false;
        } else if (unmaskedCpf.length() != 11) {
            binding.cpf.setError("O CPF está incompleto.");
            isValid = false;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.phone.setError("O telefone é obrigatório.");
            isValid = false;
        } else if (unmaskedPhone.length() != 11) {
            binding.phone.setError("O número de telefone está incompleto.");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.email.setError("O e-mail é obrigatório.");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("O formato do e-mail é inválido.");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError("A senha é obrigatória.");
            isValid = false;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError("A senha deve ter no mínimo 6 caracteres.");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.confirmPasswordLayout.setError("A confirmação de senha é obrigatória.");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError("As senhas não coincidem.");
            isValid = false;
        }

        return isValid;
    }

    private void saveUserToFirestore(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", binding.fullName.getText().toString().trim());
        user.put("email", binding.email.getText().toString().trim());
        user.put("cpf", binding.cpf.getText().toString().trim());
        user.put("phone", binding.phone.getText().toString().trim());
        user.put("photoUrl", null);
        user.put("rating", 0.0);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Erro ao salvar dados do usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!isLoading);
        binding.fullName.setEnabled(!isLoading);
        binding.cpf.setEnabled(!isLoading);
        binding.phone.setEnabled(!isLoading);
        binding.email.setEnabled(!isLoading);
        binding.password.setEnabled(!isLoading);
        binding.confirmPassword.setEnabled(!isLoading);
    }
}
