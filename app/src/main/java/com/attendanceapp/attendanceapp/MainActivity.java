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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.StringReader;


import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    GoogleSignInClient mgsc;
    private static int RC_SIGN_IN = 100;
    private String accountID;
    private String accountEmail;
    private boolean presented = false;

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
        final NavigationView navigationView = findViewById(R.id.nav_view);
        Button generateQRCodeButton = findViewById(R.id.generate_qrcode_button);
        ImageView qrCodeView = findViewById(R.id.qrcode_view);
        Switch presentedSwitch = findViewById(R.id.presented_switch);


        if (account != null) {
            main_textview.setText("Hello " + account.getGivenName());
            main_textview.setTextSize(20);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            navigationView.setVisibility(View.VISIBLE);
            generateQRCodeButton.setVisibility(View.VISIBLE);
            presentedSwitch.setVisibility(View.VISIBLE);


        } else {
            main_textview.setText("Please log in");
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            navigationView.setVisibility(View.GONE);
            generateQRCodeButton.setVisibility(View.GONE);
            qrCodeView.setVisibility(View.GONE);
            presentedSwitch.setVisibility(View.GONE);

        }
    }

    View.OnClickListener clickHandler = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sign_in_button:
                    signIn();
                    break;
                case R.id.generate_qrcode_button:
                    String url = "https://attendancetrackingdesktop.appspot.com/rest/attendance/week";
                    try {
                        String week = new ClientResource(url).get().getText();
                        generateQRCode(accountEmail, week);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.presented_switch:
                    presented = !presented;
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
            accountID = account.getId();
            accountEmail = account.getEmail();
            updateUI(account);

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
        main_textview.setVisibility(View.VISIBLE);
        View qrcodeView = findViewById(R.id.qrcode_view);
        qrcodeView.setVisibility(View.GONE);

        //url to get attendance information in style "attended", "attended and presented" or "not attended"
        String basicURL = "https://attendancetrackingdesktop.appspot.com/rest/attendance/list/users/";
        String url = basicURL + accountEmail;
        Log.d("URL:", url);

        String weeks [] = getContentFromXML(url,"weekId");
        String presented [] = getContentFromXML(url, "presented");

        switch (id) {
            case R.id.week_1:
                if (contains(weeks,"0")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 1 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 1 you did attend");
                    }

                } else {
                    main_textview.setText("In week 1 you did not attend");
                }
                break;
            case R.id.week_2:
                if (contains(weeks,"1")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 2 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 2 you did attend");
                    }
                } else {
                    main_textview.setText("In week 2 you did not attend");
                }
                break;
            case R.id.week_3:
                if (contains(weeks,"3")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 3 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 3 you did attend");
                    }
                } else {
                    main_textview.setText("In week 3 you did not attend");
                }
                break;
            case R.id.week_4:
                if (contains(weeks,"4")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 4 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 4 you did attend");
                    }
                } else {
                    main_textview.setText("In week 4 you did not attend");
                }
                break;
            case R.id.week_5:
                if (contains(weeks,"5")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 5 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 5 you did attend");
                    }
                } else {
                    main_textview.setText("In week 5 you did not attend");
                }
                break;
            case R.id.week_6:
                if (contains(weeks,"6")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 6 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 6 you did attend");
                    }
                } else {
                    main_textview.setText("In week 6 you did not attend");
                }
                break;
            case R.id.week_7:
                if (contains(weeks,"7")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 7 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 7 you did attend");
                    }
                } else {
                    main_textview.setText("In week 7 you did not attend");
                }
                break;
            case R.id.week_8:
                if (contains(weeks,"8")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 8 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 8 you did attend");
                    }
                } else {
                    main_textview.setText("In week 8 you did not attend");
                }
                break;
            case R.id.week_9:
                if (contains(weeks,"9")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 9 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 9 you did attend");
                    }
                } else {
                    main_textview.setText("In week 9 you did not attend");
                }
                break;
            case R.id.week_10:
                if (contains(weeks,"10")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 10 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 10 you did attend");
                    }
                } else {
                    main_textview.setText("In week 10 you did not attend");
                }
                break;
            case R.id.week_11:
                if (contains(weeks,"11")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 11 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 11 you did attend");
                    }
                } else {
                    main_textview.setText("In week 11 you did not attend");
                }
                break;
            case R.id.week_12:
                if (contains(weeks,"12")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 12 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 12 you did attend");
                    }
                } else {
                    main_textview.setText("In week 12 you did not attend");
                }
                break;
            case R.id.week_13:
                if (contains(weeks,"13")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 13 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 13 you did attend");
                    }
                } else {
                    main_textview.setText("In week 13 you did not attend");
                }
                break;
            case R.id.week_14:
                if (contains(weeks,"14")){
                    if(contains(presented,"true")){
                        main_textview.setText("In week 14 you did attend and presented a solution");
                    } else {
                        main_textview.setText("In week 14 you did attend");
                    }
                } else {
                    main_textview.setText("In week 14 you did not attend");
                }
                break;
            case R.id.log_out:
                signOut();
                break;
        }


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

    private void generateQRCode(String email, String week) {
        ImageView imageView = (ImageView) findViewById(R.id.qrcode_view);
        TextView main_textview = (TextView) findViewById(R.id.main_textview);
        String basicURL = "https://attendancetrackingdesktop.appspot.com/rest/attendance/token/get?";
        String studentEmail = "studentEmail=" + email;
        String weekNumber = "&weekNumber=" + week;
        String presentedString = "&presented=" + presented;
        String url = basicURL + studentEmail + weekNumber + presentedString;
        try {
            String attendanceToken = new ClientResource(url).get().getText();
            String groupID = new ClientResource("https://attendancetrackingdesktop.appspot.com/rest/group/user/clemens@zuck-online.de").get().getText();

            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<xs:attendance xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                    "<studentId>"+email+"</studentId>" +
                    "<groupId>"+groupID+"</groupId>" +
                    "<weekId>"+week+"</weekId>" +
                    "<presented>"+presented+"</presented>" +
                    "<attendanceToken>"+attendanceToken+"</attendanceToken>" +
                    "</xs:attendance>";
            Bitmap bitmap = encodeAsBitmap(xml);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            main_textview.setVisibility(View.GONE);
        }
        catch(Exception exc) {
            main_textview.setText(exc.getMessage());
        }
    }

    private String [] getContentFromXML(String url, String content) {
        String [] contentArray = new String[14];

        try {
            String xml = new ClientResource(url).get().getText();
            Log.d("XML Text: ", xml);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            InputSource inputStream = new InputSource(new StringReader(xml));

            Document document = documentBuilder.parse(inputStream);

            NodeList weekIds = document.getElementsByTagName(content);
            for(int i = 0; i< weekIds.getLength(); i++){
                contentArray[i] = weekIds.item(i).getTextContent();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentArray;
    }

    private static boolean contains (String [] stringArray, String element) {
        if(stringArray == null) {
            return false;
        } else {
            for(int i = 0; i < stringArray.length; i++){
                if (stringArray[i] != null && stringArray[i].equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }
}
