package com.ingenious.ishant.ipics;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static android.view.View.VISIBLE;

public class DetailViewActivity extends AppCompatActivity {
    private String user;
    private String title;
    private String picUrl;
    private String descFull;
    private StorageReference mStorageRef;
    private TextView titleView;
    private TextView descView;
    private EditText titleEdit;
    private EditText descEdit;
    private Button submit2;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       Intent intent = getIntent();

       user=intent.getStringExtra("user");
       title=intent.getStringExtra("title");

        titleView=(TextView)findViewById(R.id.object_desc_title_detail_view);
        titleView.setText(title);

        descFull=intent.getStringExtra("full");
        descView=(TextView)findViewById(R.id.object_desc_full_detail_view);
        descView.setText(descFull);

        picUrl=intent.getStringExtra("picUrl");
        Uri picUri = Uri.parse(picUrl);
        ImageView img=(ImageView)findViewById(R.id.object_photo_detail_view) ;
        Glide.with(DetailViewActivity.this).load(picUri).into(img);

        titleEdit=(EditText)findViewById(R.id.object_desc2);
        descEdit=(EditText)findViewById(R.id.object_desc_full2);
        submit2=(Button)findViewById(R.id.submitButton2);
    }
    public void editValues()
    {
        titleView.setVisibility(View.INVISIBLE);
        titleEdit.setVisibility(VISIBLE);
        titleEdit.setText(titleView.getText().toString());

        descView.setVisibility(View.INVISIBLE);
        descEdit.setVisibility(View.VISIBLE);
        descEdit.setText(descView.getText().toString());

        submit2.setVisibility(VISIBLE);
        submit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getBaseContext(),"Updating Data",Toast.LENGTH_SHORT).show();

                titleView.setVisibility(View.VISIBLE);
                titleEdit.setVisibility(View.INVISIBLE);
                titleView.setText(titleEdit.getText().toString());
                title=titleView.getText().toString();

                descView.setVisibility(View.VISIBLE);
                descEdit.setVisibility(View.INVISIBLE);
                descView.setText(descEdit.getText().toString());
                descFull=descView.getText().toString();

                submit2.setVisibility(View.INVISIBLE);

                ref=FirebaseDatabase.getInstance().getReference();
                String key = ref.child("cards").child(user).push().getKey();

                ref = FirebaseDatabase.getInstance().getReference();
                Query applesQuery = ref.child("cards").child(user).orderByChild("photoUrl").equalTo(picUrl);

                applesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                            appleSnapshot.getRef().removeValue();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                //Firebase taskRef = ref.child(key);
                ref.child("cards").child(user).child(key).child("description").setValue(title);
                ref.child("cards").child(user).child(key).child("descriptionFull").setValue(descFull);
                ref.child("cards").child(user).child(key).child("photoUrl").setValue(picUrl);
            }
        });

    }
    public void deleteStorageDatabase()
    {
        ref = FirebaseDatabase.getInstance().getReference();
        Query applesQuery = ref.child("cards").child(user).orderByChild("photoUrl").equalTo(this.picUrl);
        applesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                    appleSnapshot.getRef().removeValue();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(this.picUrl);
        mStorageRef.delete().addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //  an error occurred!
            }
        });
    }
    public void shareUrl()
    {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, this.picUrl);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_view_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit:
                editValues();
                return true;

            case R.id.menu_share:
                 shareUrl();
                return true;

            case R.id.menu_delete:
                deleteStorageDatabase();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}