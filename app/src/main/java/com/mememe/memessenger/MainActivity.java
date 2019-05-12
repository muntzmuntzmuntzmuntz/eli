package com.mememe.memessenger;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.mememe.memessenger.models.User;
import com.mememe.memessenger.processors.Save;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends Activity {

    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private CircleImageView circleImageView;

    private PaintView paintView;
    SeekBar seekBar;
    DisplayMetrics metrics;
    int[] COLORS = {Color.BLUE, Color.CYAN, Color.DKGRAY, Color.BLACK, Color.LTGRAY, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW};
    int BRUSH_COLOR = 3;
    int BRUSH_SIZE = 5;
    int BACKGROUND_COLOR = 9;

    User user;

    Dialog loginPopup;
    Boolean isLoggedIn = false;

    Boolean colorButtonClicked = false;
    Boolean sizeButtonClicked = false;
    Boolean bgButtonClicked = false;
    int COLOR_INDEX = 3;
    int SIZE_INDEX = 5;
    int BG_INDEX = 10;

    private Button setColorButton, setSizeButton, setBackgroundButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginPopup = new Dialog(this);
        callbackManager = CallbackManager.Factory.create();
        circleImageView = findViewById(R.id.profile_image);

        setColorButton = findViewById(R.id.brush_color);
        setSizeButton = findViewById(R.id.brush_size);
        setBackgroundButton = findViewById(R.id.background_color);

        //initiate paintView
        paintView = findViewById(R.id.paintView);
        seekBar = findViewById(R.id.seekbar_font);
        try{
            metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            paintView.init(metrics,COLORS[BRUSH_COLOR],BRUSH_SIZE, COLORS[BG_INDEX]);
            paintView.normal();
        }catch (Exception e){
            Log.e(">> GG", "GG" +e);
        }
        seekBar.setVisibility(View.GONE);

        checkLoginStatus();
    }

    public void checkLoginStatus(){
        loginPopup.setContentView(R.layout.login_popup);
        loginButton = loginPopup.findViewById(R.id.login_button);

        if (AccessToken.getCurrentAccessToken()!=null){
            loadUserProfile(AccessToken.getCurrentAccessToken());
            loginPopup.dismiss();
        }else{
            Toast.makeText(MainActivity.this,"Please Login",Toast.LENGTH_LONG).show();
            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    loadUserProfile(AccessToken.getCurrentAccessToken());
                    loginPopup.dismiss();
                    isLoggedIn = true;
                }

                @Override
                public void onCancel() {
                    Toast.makeText(MainActivity.this, "Login UNSUCCESSFUL CANCELLED", Toast.LENGTH_LONG).show();
                    // App code
                }

                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(MainActivity.this, "Login UNSUCCESSFUL", Toast.LENGTH_LONG).show();
                    Log.d(">> FACEBOOK CONNECTION","LOGIN UNSUCCESSFULL "+error);
                    isLoggedIn = false;
                }
            });
            loginPopup.show();
        }

        loginPopup.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!isLoggedIn)
                    Glide.with(MainActivity.this).load("http://via.placeholder.com/300.png").into(circleImageView);
            }
        });
    }

    AccessTokenTracker tokenTracker = new AccessTokenTracker() {
        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
            if (currentAccessToken==null){
                Toast.makeText(MainActivity.this,"Please Login",Toast.LENGTH_LONG).show();
                isLoggedIn = false;
                Glide.with(MainActivity.this).load("http://via.placeholder.com/300.png").into(circleImageView);
            }else{
                loadUserProfile(currentAccessToken);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadUserProfile(AccessToken newAccessToken){
        GraphRequest request = GraphRequest.newMeRequest(newAccessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.d(">> PARSING",""+object.toString());
                try {
                    user = new User(object.getString("id"),object.getString("name"),"https://graph.facebook.com/"+object.getString("id")+"/picture?type=normal");
                    Glide.with(MainActivity.this).load(user.getImage_url()).into(circleImageView);
                    Toast.makeText(MainActivity.this,"Logged In as: "+user.getName(),Toast.LENGTH_LONG).show();
                    isLoggedIn = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        request.executeAsync();
    }

    public void profileOnClick(View v){
        if (isLoggedIn){
            logout();
        }else {
            checkLoginStatus();
        }
    }

    public void logout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("CONFIRM LOGOUT?");

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                LoginManager.getInstance().logOut();
                Glide.with(MainActivity.this).load("http://via.placeholder.com/300.png").into(circleImageView);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setColor(View view) {
        sizeButtonClicked = false;
        bgButtonClicked = false;

        if (colorButtonClicked) {
            seekBar.setVisibility(View.GONE);
            colorButtonClicked = false;
        }
        else {
            seekBar.setVisibility(View.VISIBLE);
            colorButtonClicked = true;

            seekBar.setMax(10);

            Log.i(">> COLOR INDEX: ", "" + COLOR_INDEX);
            seekBar.setProgress(COLOR_INDEX);
            paintView.init(metrics, COLORS[COLOR_INDEX], BRUSH_SIZE, COLORS[BG_INDEX]);
            Shader shader = new LinearGradient(0, 0, 800.f, 0.0f, COLORS, null, Shader.TileMode.CLAMP);

            ShapeDrawable shape = new ShapeDrawable(new RectShape());
            shape.getPaint().setShader(shader);

            seekBar.setProgressDrawable(shape);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int index = seekBar.getProgress();
                    try {
                        BRUSH_COLOR = COLORS[index];
                        paintView.init(metrics, BRUSH_COLOR, BRUSH_SIZE, COLORS[BG_INDEX]);
                        setColorButton.setTextColor(BRUSH_COLOR);
                        Log.i(">> SET COLOR", "INDEX: " + index);
                        COLOR_INDEX = index;
                    } catch (Exception e) {
                        Log.e(">> SET COLOR", "INDEX: " + index + " Error :" + e);
                    }
                }
            });
        }
    }

    public void setSize(View view) {
        colorButtonClicked = false;
        bgButtonClicked = false;

        if (sizeButtonClicked) {
            seekBar.setVisibility(View.GONE);
            sizeButtonClicked = false;
        }
        else {
            seekBar.setVisibility(View.VISIBLE);
            sizeButtonClicked = true;

            seekBar.setMax(30);

            seekBar.setProgress(SIZE_INDEX);

            Shader shader = new LinearGradient(0, 0, 800.f, 0.0f, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);

            ShapeDrawable shape = new ShapeDrawable(new RectShape());
            shape.getPaint().setShader(shader);

            seekBar.setProgressDrawable(shape);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    BRUSH_SIZE = seekBar.getProgress();
                    try {
                        paintView.init(metrics, COLORS[COLOR_INDEX], BRUSH_SIZE, COLORS[BG_INDEX]);
                        Log.i(">> SIZE:", "" +BRUSH_SIZE);
                        SIZE_INDEX = BRUSH_SIZE;
                    } catch (Exception e) {
                        Log.e(">> SET SIZE:", "" + e);
                    }
                }
            });
        }
    }

    public void setBgColor(View view){
        sizeButtonClicked = false;
        colorButtonClicked = false;

        if (bgButtonClicked) {
            seekBar.setVisibility(View.GONE);
            bgButtonClicked = false;
        }
        else {
            seekBar.setVisibility(View.VISIBLE);
            bgButtonClicked = true;

            seekBar.setMax(10);

            Log.i(">> COLOR INDEX: ", "" + COLOR_INDEX);
            seekBar.setProgress(BG_INDEX);
            paintView.init(metrics, COLORS[COLOR_INDEX], BRUSH_SIZE, COLORS[BG_INDEX]);
            Shader shader = new LinearGradient(0, 0, 800.f, 0.0f, COLORS, null, Shader.TileMode.CLAMP);

            ShapeDrawable shape = new ShapeDrawable(new RectShape());
            shape.getPaint().setShader(shader);

            seekBar.setProgressDrawable(shape);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int index = seekBar.getProgress();
                    try {
                        BACKGROUND_COLOR = COLORS[index];
                        paintView.init(metrics, COLORS[COLOR_INDEX], BRUSH_SIZE, BACKGROUND_COLOR);
                        paintView.setBackground(BACKGROUND_COLOR);

                        Toast.makeText(MainActivity.this,"Color: "+BRUSH_COLOR+" Size: "+BRUSH_SIZE,Toast.LENGTH_LONG).show();

                        BG_INDEX = index;
                    } catch (Exception e) {
                        Log.e(">> SET COLOR", "INDEX: " + index + " Error :" + e);
                    }
                }
            });
        }
    }
    public void save(View view){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},  1);
        new Save().execute(getContentResolver(),getApplicationContext(),paintView);
    }
}
