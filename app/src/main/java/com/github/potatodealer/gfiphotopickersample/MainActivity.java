package com.github.potatodealer.gfiphotopickersample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.potatodealer.gfiphotopicker.GFIPhotoPicker;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String INSTAGRAM_CLIENT_ID = "3e3b5ae048574c60ad7856ad33da9ba4";
    private static final String INSTAGRAM_REDIRECT_URI = "https://goodies.co.id/instaCallback";
    private static final int  REQUEST_CODE = 777;

    private UriAdapter mAdapter;
    private static List<Uri> mSelection;
    private static List<Uri> mInstagramSelection;
    private static List<Uri> mFacebookSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.botan).setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter = new UriAdapter());
    }

    @Override
    public void onClick(View view) {
        // The function below is for in case the notifyDataSetChanged() on the UriAdapter class
        // adds the mInstagramSelection to the mSelection
        // I don't know what caused this bug, but this is the temporary fix
        // It doesn't have anything to do with the library, just the sample app
        if (mSelection != null) {
            mSelection.removeAll(mInstagramSelection);
            mSelection.removeAll(mFacebookSelection);
        }
        GFIPhotoPicker.init(this)
                .setInstagramClientId(INSTAGRAM_CLIENT_ID)
                .setInstagramRedirectUri(INSTAGRAM_REDIRECT_URI)
                .setRequestCode(REQUEST_CODE)
                .setMaxSelection(10)
                .setSelection(mSelection)
                .setFacebookSelection(mFacebookSelection)
                .setInstagramSelection(mInstagramSelection)
                .open();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            mUris.addAll(facebookUris);
            mUris.addAll(instagramUris);
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
