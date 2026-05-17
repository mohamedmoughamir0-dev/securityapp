package com.example.quizzapp_security;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.quizzapp_security.anti_cheat.FaceVerificationManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText      etEmail, etPassword;
    private Button        btnLogin;
    private TextView      tvToRegister, tvError;
    private LinearLayout  errorCard, successLayout;
    private FirebaseAuth  mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        tvToRegister  = findViewById(R.id.tvToRegister);
        tvError       = findViewById(R.id.tvError);
        errorCard     = findViewById(R.id.errorCard);
        successLayout = findViewById(R.id.successLayout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnLogin.setOnClickListener(v -> signIn());

        tvToRegister.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, Register.class)));
    }

    private void signIn() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Veuillez entrer votre adresse email.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Veuillez entrer votre mot de passe.");
            return;
        }

        setLoadingState(true);
        hideError();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            showSuccess();
                        } else {
                            setLoadingState(false);
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Erreur inconnue";
                            showError(friendlyError(msg));
                        }
                    }
                });
    }

    private void showSuccess() {
        successLayout.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FaceVerificationManager fvm = new FaceVerificationManager(MainActivity.this);
            Class<?> dest = fvm.hasEmbedding() ? QuizActivity.class : FaceRegistrationActivity.class;
            startActivity(new Intent(MainActivity.this, dest));
            finish();
        }, 1500);
    }

    private void showError(String message) {
        tvError.setText(message);
        errorCard.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorCard.setVisibility(View.GONE);
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Connexion en cours…" : "SE CONNECTER");
    }

    private String friendlyError(String raw) {
        if (raw == null) return "Une erreur s'est produite.";
        if (raw.contains("password"))   return "Mot de passe incorrect.";
        if (raw.contains("no user"))    return "Aucun compte trouvé avec cet email.";
        if (raw.contains("email"))      return "Adresse email invalide.";
        if (raw.contains("network"))    return "Pas de connexion internet.";
        if (raw.contains("blocked"))    return "Trop de tentatives. Réessayez plus tard.";
        return "Identifiants incorrects. Vérifiez votre email et mot de passe.";
    }
}
