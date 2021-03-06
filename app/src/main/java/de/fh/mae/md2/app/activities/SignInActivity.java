package de.fh.mae.md2.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import de.fh.mae.md2.app.Main;
import de.fh.mae.md2.app.MyPayments;
import de.fh.mae.md2.app.R;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private SignInButton signInButtonGoogle;
    private Button signInButtonGuest;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.sign_in_label));
        setContentView(R.layout.activity_sign_in);

        setOnClickListeners();

        configure();
    }

    private void setOnClickListeners() {
        signInButtonGoogle = (SignInButton) findViewById(R.id.sign_in_button_google);
        signInButtonGuest = (Button) findViewById(R.id.sign_in_button_guest);

        signInButtonGoogle.setOnClickListener(this);
        signInButtonGuest.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            setSharedPreferences(currentUser);
            switchToMainActivity();
        }
    }

    private void configure() {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Snackbar.make(findViewById(R.id.sign_in_layout), "Google sign in Failed.", Snackbar.LENGTH_SHORT).show();
                //updateUI(null);
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Authentifizierung...");
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success");
                    FirebaseUser user = mAuth.getCurrentUser();

                    setSharedPreferences(user);

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Snackbar.make(findViewById(R.id.sign_in_layout), "Authentication failed.", Snackbar.LENGTH_SHORT).show();
                    //updateUI(null);
                }

                progressDialog.hide();
                MyPayments.signIn();
                switchToMainActivity();
                }
            });
    }

    // sign in
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signInGuest() {
        setSharedPreferences(null);
        switchToMainActivity();
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.sign_in_button_google) {
            signIn();
        } else if (i == R.id.sign_in_button_guest) {
            signInGuest();
        }
    }

    private void setSharedPreferences(FirebaseUser user) {
        String name = getResources().getString(R.string.sign_in_guest_name);
        String email = getResources().getString(R.string.sign_in_guest_email);;
        String imageUrl = String.valueOf(R.drawable.ic_default_user);

        if(user != null) {
            name = user.getDisplayName();
            email = user.getEmail();
            imageUrl = user.getPhotoUrl().toString();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("accountInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("imageUrl", imageUrl);
        editor.commit();
    }

    private void switchToMainActivity() {
        finish();
        Intent myIntent = new Intent(SignInActivity.this, Main.class);
        startActivity(myIntent);
    }
}
