package com.github.potatodealer.gfiphotopickersample;

import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.potatodealer.gfiphotopicker.GFIPhotoPicker;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String FACEBOOK_AUTHORITY = "com.github.potatodealer.gfiphotopickersample.facebook";
    private static final String INSTAGRAM_AUTHORITY = "com.github.potatodealer.gfiphotopickersample.instagram";
    private static final String INSTAGRAM_CLIENT_ID = "3e3b5ae048574c60ad7856ad33da9ba4";
    private static final String INSTAGRAM_REDIRECT_URI = "https://goodies.co.id/instaCallback";
    private static final int  REQUEST_CODE = 777;

    private UriAdapter mAdapter;
    private static List<Uri> mSelection;
    private static List<Uri> mFacebookSelection;
    private static List<Uri> mInstagramSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.botan).setOnClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter = new UriAdapter());

        if (savedInstanceState != null) {
            Log.d("MainActivity", "savedInstanceState");
            mSelection = (List<Uri>) savedInstanceState.getSerializable("gallery_selection");
            mFacebookSelection = (List<Uri>) savedInstanceState.getSerializable("facebook_selection");
            mInstagramSelection = (List<Uri>) savedInstanceState.getSerializable("instagram_selection");
            mAdapter.setData(mSelection, mFacebookSelection, mInstagramSelection);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("MainActivity", "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (mSelection != null) outState.putSerializable("gallery_selection", new LinkedList<>(mSelection));
        if (mFacebookSelection != null) outState.putSerializable("facebook_selection", new LinkedList<>(mFacebookSelection));
        if (mInstagramSelection != null) outState.putSerializable("instagram_selection", new LinkedList<>(mInstagramSelection));
    }

    @Override
    public void onClick(View view) {
        // The function below is for in case the notifyDataSetChanged() on the UriAdapter class
        // adds the mInstagramSelection and mFacebookSelection to the mSelection
        // I don't know what caused this bug, but this is the temporary fix
        // It doesn't have anything to do with the library, just the sample app
        if (mSelection != null) {
            if (mInstagramSelection!= null) mSelection.removeAll(mInstagramSelection);
            if (mFacebookSelection != null) mSelection.removeAll(mFacebookSelection);
        }
        GFIPhotoPicker.init(this)
                .setInstagramClientId(INSTAGRAM_CLIENT_ID)
                .setInstagramRedirectUri(INSTAGRAM_REDIRECT_URI)
                .setRequestCode(REQUEST_CODE)
                .setMaxSelection(10)
                .setSelection(mSelection)
                .setFacebookSelection(mFacebookSelection)
                .setInstagramSelection(mInstagramSelection)
                .setFacebookAuthority(FACEBOOK_AUTHORITY)
                .setInstagramAuthority(INSTAGRAM_AUTHORITY)
                .setMinImageResolution(700, 700)
                .setLowResolutionAlertText(getString(R.string.low_resolution_alert_text))
                .open();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult");
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mSelection = PhotoPickerActivity.getSelection(data);
            mFacebookSelection = PhotoPickerActivity.getFacebookSelection(data);
            mInstagramSelection = PhotoPickerActivity.getInstagramSelection(data);
            mAdapter.setData(mSelection, mFacebookSelection, mInstagramSelection);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static class UriAdapter extends RecyclerView.Adapter<UriAdapter.UriViewHolder> {

        private List<Uri> mUris;

        void setData(List<Uri> uris, List<Uri> facebookUris, List<Uri> instagramUris) {
            mUris = uris;
            if (facebookUris != null) mUris.addAll(facebookUris);
            if (instagramUris != null)mUris.addAll(instagramUris);
            notifyDataSetChanged();
        }

        @Override
        public UriViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UriViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.uri_item, parent, false));
        }

        @Override
        public void onBindViewHolder(UriViewHolder holder, int position) {
            holder.mUri.setText(mUris.get(position).toString());

            //holder.mUri.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
        }

        @Override
        public int getItemCount() {
            return mUris == null ? 0 : mUris.size();
        }

        static class UriViewHolder extends RecyclerView.ViewHolder {

            private TextView mUri;

            UriViewHolder(View contentView) {
                super(contentView);
                mUri = contentView.findViewById(R.id.uri);
            }
        }
    }
}
