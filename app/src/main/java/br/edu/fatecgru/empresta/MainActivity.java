package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration unreadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_conversations, R.id.navigation_my_tools, R.id.navigation_loans, R.id.navigation_profile)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
        } else {
            listenForUnreadMessages(currentUser.getUid());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (unreadListener != null) {
            unreadListener.remove();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void listenForUnreadMessages(String userId) {
        unreadListener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen for unread messages failed.", error);
                    return;
                }

                if (snapshots != null) {
                    long totalUnreadCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Long> unreadCountMap = (Map<String, Long>) doc.get("unreadCount");
                        if (unreadCountMap != null && unreadCountMap.containsKey(userId)) {
                            totalUnreadCount += unreadCountMap.get(userId);
                        }
                    }
                    updateBottomNavBadge(totalUnreadCount);
                }
            });
    }

    private void updateBottomNavBadge(long count) {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        if (count > 0) {
            bottomNav.getOrCreateBadge(R.id.navigation_conversations).setNumber((int) count);
        } else {
            bottomNav.removeBadge(R.id.navigation_conversations);
        }
    }
}
