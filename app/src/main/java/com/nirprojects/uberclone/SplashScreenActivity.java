package com.nirprojects.uberclone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.nirprojects.uberclone.Model.DriverInfoModel;
import com.nirprojects.uberclone.Utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SplashScreenActivity extends AppCompatActivity {
    public static final String TAG ="SplashScreenActivity";

    private final static int LOGIN_REQUEST_CODE = 7171; //random number here..
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    FirebaseDatabase database;
    DatabaseReference driverInfoRef; //reference to realtime-firebase DB.

    //Butterknife's library annotations..
    @BindView(R.id.progress_bar)
    ProgressBar progress_bar; //binds the above id to progress_bar instance.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        init();
    }

    private void init(){
        Log.d(TAG,"onInit()");

        ButterKnife.bind(this); //BindView annotated fields and methods in the specified Activity.
        //The current content view is used as the view root.
        //Parameters:
        //target - Target activity for view binding.

        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE);


        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()); //Holds types of Auth-Configurations (email and phone..)

        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth->{ //A Lambda Expression here (to return back, on Modules -> java 1.7...)
            //What we do here is we applying a new method: "myFirebaseAuth" is the parameter-name for the method...
            //... which is of TYPE=FirebaseAuth.
            //it applies logics to the interface coded within listener-instance which is an inner-interface within FirebaseAuth class.
            //so how does it work? this inner-interface has only 1-singular-method! thus it knows to "apply logics" specificly to it!
            //BELOW: is the overriden method by this lambda-expression:
            /*public interface AuthStateListener {
                    void onAuthStateChanged(@NonNull FirebaseAuth var1);
                }*/
                Log.d(TAG,"Within listener.onAuthStateChanged()");
                FirebaseUser user = myFirebaseAuth.getCurrentUser(); //TODO: NOTICE, It works with Token-Logic, thus once signing-in, it will almost-always return !null for that device!
                if(user!=null) {//Auto-Login that user (this is the case the device already possesses a valid TOKEN)...
                    //Update Token
                    FirebaseInstanceId.getInstance().getInstanceId()
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                                @Override
                                public void onSuccess(InstanceIdResult instanceIdResult) {
                                    Log.d(TAG,"TOKEN="+instanceIdResult.getToken());
                                    UserUtils.updateToken(SplashScreenActivity.this,instanceIdResult.getToken());
                                }
                            });
                    checkUserFromFirebase();
                }
                else {
                    showLoginLayout(); //Begin sign-in process...
                }
            };
    }

    private void checkUserFromFirebase() {
        Log.d(TAG,"OnCheckUserFromFirebase()");
        driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()) //a ref' to the user-line in the Authenication-Firebase-DB
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    //Add a listener for a single change in the data at this location.
                    //This listener will be triggered once with the value of the data at the location.
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            //Toast.makeText(SplashScreenActivity.this, "User already registered", Toast.LENGTH_SHORT).show();
                            DriverInfoModel driverInfoModel = dataSnapshot.getValue(DriverInfoModel.class);
                            goToHomeActivity(driverInfoModel);

                        } else{
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(SplashScreenActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToHomeActivity(DriverInfoModel driverInfoModel) {
        Common.currentUser = driverInfoModel; //init value
        startActivity(new Intent(SplashScreenActivity.this,DriverHomeActivity.class));
        finish();
    }

    private void showRegisterLayout(){
        Log.d(TAG,"onShowRegisterLayout()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);

        TextInputEditText edt_first_name = (TextInputEditText)itemView.findViewById(R.id.edt_first_name);
        TextInputEditText edt_last_name = (TextInputEditText)itemView.findViewById(R.id.edt_last_name);
        TextInputEditText edt_phone = (TextInputEditText)itemView.findViewById(R.id.edt_phone_number);

        Button btn_continue = (Button)itemView.findViewById(R.id.btn_register);

        //set Data
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null //phone doesn't exist..  && phone != null/empty string...
                    &&  !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())) {
            edt_phone.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        }

        //set view
        builder.setView(itemView); //Sets a custom view to be the contents of the alert dialog.
        AlertDialog dialog = builder.create();
        dialog.show();

        btn_continue.setOnClickListener( view -> { //apply some registeration logic to reg'-fields(NOTICE IT'S A LAMBDA instead of the regular new OnClickListener...)
            if(TextUtils.isEmpty(edt_first_name.getText().toString())){
                Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show();
                return;
            }
            else if(TextUtils.isEmpty(edt_last_name.getText().toString())){
                Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show();
                return;
            }
            else if(TextUtils.isEmpty(edt_phone.getText().toString())){
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            else{
                DriverInfoModel model = new DriverInfoModel();
                model.setFirstName(edt_first_name.getText().toString());
                model.setLastName(edt_last_name.getText().toString());
                model.setPhoneNumber(edt_phone.getText().toString());
                model.setRating(0.0);

                driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(SplashScreenActivity.this, "Register Succesfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                goToHomeActivity(model);
                            }
                        });
            }
        });

    }

    private void showLoginLayout() {
        Log.d(TAG,"onShowLoginLayout()");
        //Layout model to help customizing layout of the AuthMethodPickerActivity screen,
        //where the user is presented with a list of sign-in providers to choose from.


        //above docs ^^
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_signin)
                .setPhoneButtonId(R.id.btn_phone_sign_in) //setting id's of signings buttons
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance() //beginning an auth-signing activitiy.. (built in within Firebase already..)
        .createSignInIntentBuilder() //Starts the process of creating a sign in intent, with the mandatory application context parameter.
        .setAuthMethodPickerLayout(authMethodPickerLayout) //sets the signing options UI applied above..
        .setIsSmartLockEnabled(false) //Enables or disables the use of Smart Lock for Passwords in the sign in flow.
                .setTheme(R.style.LoginTheme) //applies new theme to that acitvity.
        .setAvailableProviders(providers) //Specified the set of supported authentication providers. At least one provider must be specified.
        .build(),LOGIN_REQUEST_CODE);
    }

    private void delaySplashScreen(){
        Log.d(TAG,"onDelaySplashScreen()");

        progress_bar.setVisibility(View.VISIBLE);

        //The Completable class represents a deferred computation without any value but only indication for completion or exception.
        Completable.timer(3, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread()) //A Scheduler which executes actions on the Android main thread.
                .subscribe(() ->  //this Subscribes to this^^ Completable and calls the given Action(the ()-> parameter is a LAMBDA exression of Action-Interface) when this Completable completes normally.
                    //Action acts as OnCompleteListener... performs "run()" upon completion
                    //TODO: in sum up, after -> is the body of Action's run() overriden method!
                    //After show Splash Screen, ask for login if not logged-in
                    firebaseAuth.addAuthStateListener(listener)
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG,"onOnActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST_CODE){ //incase coming from Login-Auth-Intent..
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else
            {
                Toast.makeText(this,"[ERROR]: "+response.getError().getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() { // from the lifecycle: onCreate() -> onStart() ->...
        //in our flow, it's invoked right after init().
        Log.d(TAG,"onStart()");
        super.onStart();
        delaySplashScreen();
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop()");
        super.onStop();
    }
}