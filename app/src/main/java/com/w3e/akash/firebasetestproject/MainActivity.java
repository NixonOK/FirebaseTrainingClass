package com.w3e.akash.firebasetestproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        registerUser("n@n.com", "123");

        //sign in functionality
        usernameQuery = FirebaseDatabase.getInstance("https://money-bank-nixon-ok.firebaseio.com/").getReference().child("UserProfileDB").
                orderByChild("DisplayName").equalTo(username);


        usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        authorizeUser(userEmail, password);
                    }


                }
            }
        });

        // Forgot password funtionality
        forgotPass.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    forgotPass.setEnabled(false);
                                    Toasty.success( LoginActivity.this, "Sent reset password link to " + email +", Check email for further instructions.", 2000, false).show();
                                } else {
                                    // ...
                                }
                            }
                        });

            }
        });

        // Get the fire base instance
        firebaseFirestore = FirebaseFirestore.getInstance();
        // Fetching the place data from Places table
        firebaseFirestore.collection("Data").addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
            // Get the last visible document
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                // Check if There is any data in the cloud
                if (!documentSnapshots.isEmpty()) {
                    // Take each data/place
                    for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                        // Check if it was added previously
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            // Add the place to place list
                            // Update the recycler adapter
                            User user = doc.getDocument().toObject(User.class);
                            userDataList.add(user);
                            recyclerAdapter.notifyDataSetChanged();

                        }
                    }
                }
            }
        });

    }


    //Create Account
    private void registerUser(String email,String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("TESTING", "Sign up Successful" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegistrationActivity.this, "Sign-up Failed!", Toast.LENGTH_SHORT).show();

                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {

                                Toasty.error( RegistrationActivity.this, "User with this email already exist!", Toast.LENGTH_SHORT, true).show();

                                mEmailView.setError("Email already exists!");
                                mEmailView.requestFocus();
                                return;
                            }
                        }
                        else
                        {
                            createUserProfile();
                        }
                    }
                });
    }


    //Set UserDisplay Name
    private void createUserProfile()
    {
        FirebaseUser user = mAuth.getCurrentUser();

        if(ref.isEmpty()){
            ref = "Empty";
        }

        if(user != null){
            DatabaseReference currentUserDB = FirebaseDatabase.getInstance("https://money-bank-nixon-ok.firebaseio.com/").getReference().child("UserProfileDB").child(user.getUid());

            Map userMap = new HashMap();
            String joinDate = getDateTime();
            String deviceID = Settings.Secure.getString( RegistrationActivity.this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            userMap.put("DisplayName", username);
            userMap.put("Email", email);
            userMap.put("Phone", phone);
            userMap.put("Password", password);
            userMap.put("Referral", ref);
            userMap.put("Balance", "0.0");
            userMap.put("WithdrawPending", "0.0");
            userMap.put("TotalWithdraw", "0.0");
            userMap.put("JoinDate", joinDate);
            userMap.put("DeviceID", deviceID);
            userMap.put("ProfilePic", "");

            currentUserDB.setValue(userMap);

            Toasty.success( RegistrationActivity.this, "Account Successfully Created!", Toast.LENGTH_SHORT, true).show();

            mAuth.signOut();

            Intent intent = new Intent( RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);

            finish();

        }
    }



    private void getUserInformation() {

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {

            mQuery = firebaseRef.orderByChild("Email").equalTo(mAuth.getCurrentUser().getEmail());

            mQuery.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {

                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            createUser(child);
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("MyTag", "No Data!");
                }

            });
        }
    }


    private void createUser(DataSnapshot child) {
        Log.d("MyTag", "Creating User!");
        mUser = new UserProfile();

        mUser.setDisplayName(child.child("DisplayName").getValue().toString());
        mUser.setEmail(child.child("Email").getValue().toString());
        mUser.setPhone(child.child("Phone").getValue().toString());
        mUser.setReferral(child.child("Referral").getValue().toString());
        mUser.setBalance(child.child("Balance").getValue().toString());
        mUser.setPendingWithdraw(child.child("WithdrawPending").getValue().toString());
        mUser.setTotalWithdraw(child.child("TotalWithdraw").getValue().toString());
        mUser.setJoinDate(child.child("JoinDate").getValue().toString());
        mUser.setDeviceID(child.child("DeviceID").getValue().toString());
        mUser.setProfilePic(child.child("ProfilePic").getValue().toString());

        Log.d("Account Info : ", mUser.getBalance());

    }


    // Open image selector in file manager
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    // Get the image and compress
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        File compressedImageFile = null;

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1
                && data != null && data.getData() != null) {

            mImageUri = data.getData();
            File file = null;
            try {
                file = FileUtil.from(this.getContext(), mImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                compressedImageFile = new Compressor(this.getContext())
                        .setMaxWidth(512)
                        .setMaxHeight(512)
                        .setQuality(85)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }


            RequestOptions options = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .centerCrop()
                    .circleCropTransform()
                    .override(512, 512)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

            Glide.with(this.getContext())
                    .load(mImageUri)
                    .apply(options)
                    .into(userImageView);

            uploadFile(compressedImageFile);
        }
    }



    private void uploadFile(File compressedImageFile) {
        if (mImageUri != null) {
            final StorageReference fileReference = mStorageRef.child(mAuth.getUid()
                    + ".jpeg");

            fileReference.putFile(Uri.fromFile(compressedImageFile))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(getContext(), "Upload successful", Toast.LENGTH_LONG).show();
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final Uri downloadUrl = uri;
                                    //Do what you want with the url

                                    firebaseRef.child(mAuth.getUid()).child("ProfilePic").setValue(downloadUrl.toString());
                                }
                            });


                        }
                    });

        }
    }



    private void updateUserData() {
        mQuery = firebaseRef.child("UserProfileDB").
                orderByChild("Email").equalTo(mAuth.getCurrentUser().getEmail());


        mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot child: dataSnapshot.getChildren()){
                    createAndSaveUser(child);
                    balanceTickerText.setText(mUser.getBalance());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toasty.error( getContext(), "Firebase Error, Contact Admins!", 6000).show();
            }

        });
    }

}
