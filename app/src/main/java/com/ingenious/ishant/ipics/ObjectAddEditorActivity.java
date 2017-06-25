package com.ingenious.ishant.ipics;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ingenious.ishant.ipics.model.CardModel;

import java.io.File;

import static com.ingenious.ishant.ipics.R.id.object_photo;

public class ObjectAddEditorActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int CAMERA_REQUEST=101;
    private static final int PHOTO_REQUEST = 102;
    private static final int REQUEST_READ_PERMISSION = 103;
    private String user;
    private Button submit;
    private ImageView photo;
    private EditText desc;
    private EditText descFull;
    private DatabaseReference mDatabaseReference;
    private StorageReference mStorageReference;
    private Uri photoUri;
    private ProgressDialog progressBar;
    private String photoUrl;
    private Uri outputFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_add_editor);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent i = getIntent();
        user = i.getStringExtra("user");
        desc = (EditText) findViewById(R.id.object_desc);
        descFull = (EditText) findViewById(R.id.object_desc_full);
        photo = (ImageView) findViewById(object_photo);
        submit = (Button) findViewById(R.id.submitButton);
        progressBar = new ProgressDialog(ObjectAddEditorActivity.this);
        mStorageReference = FirebaseStorage.getInstance().getReference();
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission();
            }
        });
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Uploading Image to Google Cloud",Toast.LENGTH_LONG).show();
                String descText = "";
                String descFullText = "";
                if (desc.getText().toString() != null) {
                    descText = desc.getText().toString();
                }
                if (descFull.getText().toString() != null) {
                    descFullText = descFull.getText().toString();
                }
                final StorageReference onlineStoragePhotoRef = mStorageReference.child("Photos").child(descText).child(photoUri.getLastPathSegment());

                final String finalDescText = descText;
                final String finalDescFullText = descFullText;
                onlineStoragePhotoRef.putFile(photoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        photoUrl = taskSnapshot.getDownloadUrl().toString();
                        CardModel cardModel = new CardModel(finalDescText, finalDescFullText, photoUrl);
                        mDatabaseReference.child("cards").child(user).push().setValue(cardModel);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ObjectAddEditorActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        photoUrl = "";
                        CardModel cardModel = new CardModel(finalDescText, finalDescFullText, photoUrl);
                        mDatabaseReference.child("cards").child(user).push().setValue(cardModel);
                    }
                });
                progressBar.dismiss();
                finish();
            }
        });
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(ObjectAddEditorActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ObjectAddEditorActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION);
            }
            else {
                openFilePicker();
            }
        } else {
            openFilePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    Toast.makeText(ObjectAddEditorActivity.this, "Cannot pick file from storage", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void openFilePicker() {

        final CharSequence[] items = {"Take Photo", "Choose from Library","Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ObjectAddEditorActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                     cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(Environment.getExternalStorageDirectory(),"MyPhoto.jpg");
        outputFileUri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }
    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == CAMERA_REQUEST&& resultCode == RESULT_OK ))
        {
            photoUri=outputFileUri;
            Glide.with(ObjectAddEditorActivity.this).load(outputFileUri).into(photo);
        }
        else if((requestCode == PHOTO_REQUEST && resultCode == RESULT_OK && data != null))
        {
            photoUri = data.getData();
            Glide.with(ObjectAddEditorActivity.this).load(photoUri).into(photo);
        }
    }
}