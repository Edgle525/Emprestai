package br.edu.fatecgru.empresta;

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.santalu.maskara.Mask;
import com.santalu.maskara.MaskChangedListener;
import com.santalu.maskara.MaskStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityEditProfileBinding;
import br.edu.fatecgru.empresta.network.AddressResponse;
import br.edu.fatecgru.empresta.network.ViaCepService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ViaCepService viaCepService;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://viacep.com.br/ws/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        viaCepService = retrofit.create(ViaCepService.class);

        Mask phoneMask = new Mask("(__) _____-____", '_', MaskStyle.NORMAL);
        Mask cepMask = new Mask("_____-___", '_', MaskStyle.NORMAL);
        binding.editPhone.addTextChangedListener(new MaskChangedListener(phoneMask));
        binding.editCep.addTextChangedListener(new MaskChangedListener(cepMask));

        loadUserProfile();

        binding.searchCepButton.setOnClickListener(v -> searchCep());
        binding.saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        binding.deleteProfileButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        binding.addressHeader.setOnClickListener(v -> {
            if (binding.addressContainer.getVisibility() == View.VISIBLE) {
                binding.addressContainer.setVisibility(View.GONE);
                binding.addressHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
            } else {
                binding.addressContainer.setVisibility(View.VISIBLE);
                binding.addressHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);
            }
        });
    }

    private void searchCep() {
        String unmaskedCep = binding.editCep.getText().toString().replaceAll("[^\\d]", "");
        if (unmaskedCep.length() == 8) {
            viaCepService.getAddress(unmaskedCep).enqueue(new Callback<AddressResponse>() {
                @Override
                public void onResponse(Call<AddressResponse> call, Response<AddressResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AddressResponse address = response.body();
                        if (address.getStreet() != null) {
                            binding.editStreet.setText(address.getStreet());
                            binding.editNeighborhood.setText(address.getNeighborhood());
                            binding.editCity.setText(address.getCity());
                            binding.editState.setText(address.getState());
                        } else {
                            Toast.makeText(EditProfileActivity.this, "CEP não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<AddressResponse> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, "Erro ao buscar CEP", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "CEP inválido", Toast.LENGTH_SHORT).show();
        }
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
                binding.editFullName.setText(documentSnapshot.getString("name"));
                binding.editEmail.setText("E-mail: " + documentSnapshot.getString("email"));
                binding.editCpf.setText(documentSnapshot.getString("cpf"));
                binding.editPhone.setText(documentSnapshot.getString("phone"));
                binding.editSecondaryEmail.setText(documentSnapshot.getString("secondaryEmail"));

                binding.editCep.setText(documentSnapshot.getString("cep"));
                binding.editStreet.setText(documentSnapshot.getString("street"));
                binding.editNumber.setText(documentSnapshot.getString("number"));
                binding.editNeighborhood.setText(documentSnapshot.getString("neighborhood"));
                binding.editCity.setText(documentSnapshot.getString("city"));
                binding.editState.setText(documentSnapshot.getString("state"));
            }
        });
    }

    private void saveProfileChanges() {
        String phone = binding.editPhone.getText().toString().trim();
        if (phone.replaceAll("[^\\d]", "").length() != 11) {
            binding.layoutPhone.setError("O número de telefone está incompleto.");
            return;
        } else {
            binding.layoutPhone.setError(null);
        }

        String secondaryEmail = binding.editSecondaryEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(secondaryEmail) && !Patterns.EMAIL_ADDRESS.matcher(secondaryEmail).matches()) {
            binding.layoutSecondaryEmail.setError("O formato do e-mail de contato é inválido.");
            return;
        } else {
            binding.layoutSecondaryEmail.setError(null);
        }

        String cep = binding.editCep.getText().toString().trim();
        String street = binding.editStreet.getText().toString().trim();
        String number = binding.editNumber.getText().toString().trim();
        String neighborhood = binding.editNeighborhood.getText().toString().trim();
        String city = binding.editCity.getText().toString().trim();
        String state = binding.editState.getText().toString().trim();

        if (TextUtils.isEmpty(cep) || TextUtils.isEmpty(street) || TextUtils.isEmpty(number) || TextUtils.isEmpty(neighborhood) || TextUtils.isEmpty(city) || TextUtils.isEmpty(state)) {
            Toast.makeText(this, "Preencha o endereço completo", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(String.format("%s, %s - %s, %s", street, number, city, state), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                GeoPoint geoPoint = new GeoPoint(latitude, longitude);

                String geohash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));

                Map<String, Object> updates = new HashMap<>();
                updates.put("phone", phone);
                updates.put("secondaryEmail", secondaryEmail);
                updates.put("cep", cep);
                updates.put("street", street);
                updates.put("number", number);
                updates.put("neighborhood", neighborhood);
                updates.put("city", city);
                updates.put("state", state);
                updates.put("geohash", geohash);
                updates.put("location", geoPoint);

                db.collection("users").document(user.getUid()).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Falha ao atualizar o perfil", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Endereço não encontrado. Verifique os dados.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("EditProfileActivity", "Geocoding failed", e);
            Toast.makeText(this, "Erro ao processar a localização.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Perfil")
                .setMessage("Você tem certeza que deseja excluir seu perfil? Esta ação não pode ser desfeita.")
                .setPositiveButton("Confirmar", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nenhum usuário logado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Excluindo perfil...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                mAuth.signOut();
                                mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                                    Toast.makeText(EditProfileActivity.this, "Perfil excluído com sucesso.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(EditProfileActivity.this, "Falha ao excluir a conta. Tente fazer login novamente.", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Falha ao excluir os dados do perfil.", Toast.LENGTH_LONG).show();
                });
    }
}
