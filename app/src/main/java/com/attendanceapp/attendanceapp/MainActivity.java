package com.attendanceapp.attendanceapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.restlet.resource.ClientResource;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    GoogleSignInClient mgsc;
    private static int RC_SIGN_IN = 100;

    private String user_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Google Login
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mgsc = GoogleSignIn.getClient(this,gso);

        //setView
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //Navgiation bar
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setVisibility(View.GONE);

        //Google Login button
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_ICON_ONLY);
        signInButton.setOnClickListener(clickHandler);

        Button generateQRCodeButton = findViewById(R.id.generate_qrcode_button);
        generateQRCodeButton.setOnClickListener(clickHandler);

        //Floating Action Button
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationView.setVisibility(View.VISIBLE);
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
            }
        });
        */
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);

    }

    private void updateUI(@Nullable GoogleSignInAccount account) {
        TextView main_textview = (TextView) findViewById(R.id.main_textview);
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Button qr_code = (Button) findViewById(R.id.generate_qrcode_button);
        Button sign_in = (Button) findViewById(R.id.sign_in_button);

        if (account != null) {
            main_textview.setText("Hi " + account.getGivenName() + "!");
            sign_in.setVisibility(View.GONE);
            navigationView.setVisibility(View.VISIBLE);
            qr_code.setVisibility(View.VISIBLE);

        } else {
            main_textview.setText("Please log in!");
            sign_in.setVisibility(View.VISIBLE);
            navigationView.setVisibility(View.GONE);
            qr_code.setVisibility(View.GONE);
        }
    }

    View.OnClickListener clickHandler = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sign_in_button:
                    signIn();
                    break;
                case R.id.generate_qrcode_button:
                    //"https://attendancetrackingdesktop.appspot.com/rest/attendance/token/get?studentId=112762937574526790868&weekNumber=1"
                    generateQRCode(user_ID , 1);
                    break;
            }
        }
    };

    private void signIn() {
        Intent signInIntent = mgsc.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mgsc.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);

                    }
                });
    }
    /*
    private  void changeUser() {
        mgsc.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);

                    }
                });
    }*/

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            updateUI(account);
            user_ID = account.getId();

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            //Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
            TextView main_textview = (TextView) findViewById(R.id.main_textview);
            main_textview.setText("Fehlerode: " + e.getStatusCode());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        TextView main_textview = (TextView) findViewById(R.id.main_textview);

        //url to get attendance information in style "attended", "attended and presented" or "not attended"
        String basicURL = "https://attendancetrackingdesktop.appspot.com/rest/attendance/list/users/";
        String studentID = "clemens@zuck-online.de";
        String url = basicURL + studentID;
        switch (id) {
            case R.id.week_1:
                try {
                    String rawAnswer = new ClientResource(url+"1").get().getText();
                    main_textview.setText("In week 1 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_2:
                try {
                    String rawAnswer = new ClientResource(url+"2").get().getText();
                    main_textview.setText("In week 2 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_3:
                try {
                    String rawAnswer = new ClientResource(url+"3").get().getText();
                    main_textview.setText("In week 3 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_4:
                try {
                    String rawAnswer = new ClientResource(url+"4").get().getText();
                    main_textview.setText("In week 4 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_5:
                try {
                    String rawAnswer = new ClientResource(url+"5").get().getText();
                    main_textview.setText("In week 5 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_6:
                try {
                    String rawAnswer = new ClientResource(url+"6").get().getText();
                    main_textview.setText("In week 6 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_7:
                try {
                    String rawAnswer = new ClientResource(url+"7").get().getText();
                    main_textview.setText("In week 7 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_8:
                try {
                    String rawAnswer = new ClientResource(url+"8").get().getText();
                    main_textview.setText("In week 8 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_9:
                try {
                    String rawAnswer = new ClientResource(url+"9").get().getText();
                    main_textview.setText("In week 9 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_10:
                try {
                    String rawAnswer = new ClientResource(url+"10").get().getText();
                    main_textview.setText("In week 10 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_11:
                try {
                    String rawAnswer = new ClientResource(url+"11").get().getText();
                    main_textview.setText("In week 11 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_12:
                try {
                    String rawAnswer = new ClientResource(url+"12").get().getText();
                    main_textview.setText("In week 12 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_13:
                try {
                    String rawAnswer = new ClientResource(url+"13").get().getText();
                    main_textview.setText("In week 13 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.week_14:
                try {
                    String rawAnswer = new ClientResource(url+"14").get().getText();
                    main_textview.setText("In week 14 you " + rawAnswer + "!");
                }
                catch(Exception exc) {
                    main_textview.setText(exc.getMessage());
                }
                break;
            case R.id.log_out:
                signOut();
                break;
        }

        /*
        if (id == R.id.week_1) {
            // Handle the camera action
        } else if (id == R.id.week_2) {

        } else if (id == R.id.week_3) {

        } else if (id == R.id.week_4) {

        } else if (id == R.id.week_5) {

        } else if (id == R.id.week_6) {

        } else if (id == R.id.week_7) {

        } else if (id == R.id.week_8) {

        } else if (id == R.id.week_9) {

        } else if (id == R.id.week_10) {

        } else if (id == R.id.week_11) {

        } else if (id == R.id.week_12) {

        } else if (id == R.id.week_13) {

        } else if (id == R.id.week_14) {

        } else if (id == R.id.log_out) {
            signOut();
        } else if (id == R.id.change_user) {
            changeUser();
        }*/


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, 400, 400, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private void generateQRCode(String id, Integer week) {
        ImageView imageView = (ImageView) findViewById(R.id.qrcode_view);
        TextView main_textview = (TextView) findViewById(R.id.main_textview);
        //https://attendancetrackingdesktop.appspot.com/rest/attendance/token/get?studentId=112762937574526790868&weekNumber=1
        String basicURL = "https://attendancetrackingdesktop.appspot.com/rest/attendance/token/get?";
        String studentID = "studentID=" + id;
        String weekNumber = "&weekNumber=" + week.toString();
        String url = basicURL + studentID + weekNumber;
        try {
            String rawAnswer = new ClientResource(url).get().getText();
            Bitmap bitmap = encodeAsBitmap(rawAnswer);
            imageView.setImageBitmap(bitmap);
        }
        catch(Exception exc) {
            main_textview.setText(exc.getMessage());
        }
    }
}
