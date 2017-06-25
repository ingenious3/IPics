package com.ingenious.ishant.ipics;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.firebase.client.AuthData;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ingenious.ishant.ipics.model.CardModel;

import static com.ingenious.ishant.ipics.R.id.fab;

public class DashboardActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private AuthData mAuthData;
    public static final String CARDS = "cards";
    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";
    private String user;
    private int authVar=0;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference userReference;
    private String mUsername;
    private String mPhotoUrl;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<CardModel, CardViewHolder> mFirebaseAdapter;
    private FloatingActionButton addCard;

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        isOnline();

        mRecyclerView = (RecyclerView) findViewById(R.id.items_list);
        mLinearLayoutManager = new LinearLayoutManager(this);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null ) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            this.user=mFirebaseUser.getUid().toString();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }
        addCard = (FloatingActionButton) findViewById(fab);
        addCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, ObjectAddEditorActivity.class);
                intent.putExtra("user",user);
                startActivity(intent);
            }
        });


        userReference= FirebaseDatabase.getInstance().getReference().child("cards").child(user);
        userReference.keepSynced(true);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        //Database Initialization
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<CardModel, CardViewHolder>(
                CardModel.class,
                R.layout.item_card,
                CardViewHolder.class,
                mFirebaseDatabaseReference.child(CARDS).child(user)) {
            @Override
            protected void populateViewHolder(CardViewHolder viewHolder, CardModel model, int position) {
                final String key=getRef(position).toString();
                final String picUrl=model.getPhotoUrl();
                final String title=model.getDescription();
                final String fullDes=model.getDescriptionFull();

                viewHolder.descriptionView.setText(model.getDescription());
                Glide.with(DashboardActivity.this).load(model.getPhotoUrl()).into(viewHolder.cardPhoto);
                viewHolder.item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent goToDetailView=new Intent(getBaseContext(),DetailViewActivity.class);
                        goToDetailView.putExtra("user",user);
                        goToDetailView.putExtra("picUrl",picUrl);
                        goToDetailView.putExtra("title",title);
                        goToDetailView.putExtra("full",fullDes);
                        startActivity(goToDetailView);
                    }
                });
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int cardCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 || (positionStart >= (cardCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mFirebaseAdapter);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    protected void isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            Toast.makeText(DashboardActivity.this,"Network Status : Online",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DashboardActivity.this,"Network Status : Offline",Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                mFirebaseAuth.getInstance().signOut();
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                LoginManager.getInstance().logOut();
                mFirebaseUser = null;
                mUsername = ANONYMOUS;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionView;
        TextView detailDescription;
        ImageView cardPhoto;
        View item;

        public CardViewHolder(View v) {
            super(v);
            item=itemView;
            descriptionView = (TextView) itemView.findViewById(R.id.short_desc);
            cardPhoto = (ImageView) itemView.findViewById(R.id.card_photo);
        }
    }
}
