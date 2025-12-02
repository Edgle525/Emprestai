package br.edu.fatecgru.empresta;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private AlertDialog noConnectionDialog;

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoConnectionDialog();
        } else {
            if (noConnectionDialog != null && noConnectionDialog.isShowing()) {
                noConnectionDialog.dismiss();
            }
        }
    }

    private void showNoConnectionDialog() {
        if (noConnectionDialog == null) {
            noConnectionDialog = new AlertDialog.Builder(this)
                    .setTitle("Sem Conexão")
                    .setMessage("Você está offline. Por favor, verifique sua conexão com a internet e tente novamente.")
                    .setCancelable(false)
                    .setPositiveButton("Tentar Novamente", (dialog, which) -> {
                        checkNetworkConnection();
                    })
                    .create();
        }
        if (!noConnectionDialog.isShowing()) {
            noConnectionDialog.show();
        }
    }
}
