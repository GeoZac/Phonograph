package com.kabouzeid.gramophone.glide.artistimage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.content.Context;

import androidx.annotation.NonNull;

import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCoverUtils;
import com.kabouzeid.gramophone.util.ImageUtil;
import com.bumptech.glide.load.model.GlideUrl;
import com.kabouzeid.gramophone.lastfm.rest.LastFMRestClient;
import com.kabouzeid.gramophone.lastfm.rest.model.LastFmArtist;
import com.kabouzeid.gramophone.util.LastFMUtil;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import java.io.InputStream;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistImageFetcher implements DataFetcher<InputStream> {
    private final ArtistImage model;
    private volatile boolean isCancelled;
    private Call<LastFmArtist> call;
    private OkHttpClient okhttp;
    private OkHttpStreamFetcher streamFetcher;

    private InputStream stream;
    private LastFMRestClient lastFMRestClient;
    private Context context;

    private boolean ignoreMediaStore;

    public ArtistImageFetcher(Context context, LastFMRestClient lastFMRestClient, OkHttpClient okhttp, ArtistImage model, int width, int height, boolean ignoreMediaStore) {
        this.context = context;
        this.lastFMRestClient = lastFMRestClient;
        this.okhttp = okhttp;
        this.model = model;
        this.ignoreMediaStore = ignoreMediaStore;
    }

    @NonNull
    public String getId() {
        Log.d("MOSAIC", "get id for" + model.artistName);
        // never return NULL here!
        // this id is used to determine whether the image is already cached
        // we use the artist name as well as the album years + file paths
        return model.toIdString() + "ignoremediastore:" + ignoreMediaStore;
    }

    @Override
    public void loadData(@NonNull Priority priority,@NonNull DataCallback<? super InputStream>callback) {
        try {
            if (!MusicUtil.isArtistNameUnknown(model.artistName) && PreferenceUtil.isAllowedToDownloadMetadata(context)) {
                call = lastFMRestClient.getApiService().getArtistInfo(model.artistName, null, model.skipOkHttpCache ? "no-cache" : null);
                call.enqueue(new Callback<LastFmArtist>() {
                    @Override
                    public void onResponse(@NonNull Call<LastFmArtist> call, @NonNull Response<LastFmArtist> response) {
                        if (isCancelled) {
                            callback.onDataReady(null);
                            return;
                        }
                        LastFmArtist lastFmArtist = response.body();
                        if (lastFmArtist == null || lastFmArtist.getArtist() == null || lastFmArtist.getArtist().getImage() == null) {
                            callback.onLoadFailed(new Exception("No artist image url found"));
                            return;
                        }

                        // Placeholder for now, will add custom image backend later
                        String url = LastFMUtil.getLargestArtistImageUrl(lastFmArtist.getArtist().getImage());
                        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(url.trim())) {
                            callback.onLoadFailed(new Exception("No artist image url found"));
                            return;
}
                        streamFetcher = new OkHttpStreamFetcher(okhttp, new GlideUrl(url));
                        streamFetcher.loadData(priority, callback);
                    }

                    @Override
                    public void onFailure(@NonNull Call<LastFmArtist> call, @NonNull Throwable throwable) {
                        try {
                            stream = getMosaic(model.albumCovers);
                            callback.onDataReady(stream);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        callback.onLoadFailed(new Exception(throwable));
                    }

                });
            }
        } catch (Exception e) {
            try { // web search couldn't return image, so we switch to
                stream = getMosaic(model.albumCovers);
                callback.onDataReady(stream);
            } catch (FileNotFoundException f) {
                f.printStackTrace();
            }
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {
    // already cleaned up in loadData and ByteArrayInputStream will be GC'd
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
                // can't do much about it
            }
        }
    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }

    private InputStream getMosaic(final List<AlbumCover> albumCovers) throws FileNotFoundException {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        int artistBitMapSize = 512;

        final Map<InputStream, Integer> images = new HashMap<>();

        InputStream result = null;
        List<InputStream> streams = new ArrayList<>();

        try {
            for (final AlbumCover cover : albumCovers) {
                byte[] picture = null;
                if (!ignoreMediaStore) {
                    retriever.setDataSource(cover.getFilePath());
                    picture = retriever.getEmbeddedPicture();
                }
                final InputStream stream;
                if (picture != null) {
                    stream = new ByteArrayInputStream(picture);
                } else {
                    stream = AudioFileCoverUtils.fallback(cover.getFilePath());
                }

                if (stream != null) {
                    images.put(stream, cover.getYear());
                }
            }

            int nbImages = images.size();

            if (nbImages > 3) {
                streams = new ArrayList<>(images.keySet());

                int divisor = 1;
                for (int i = 1; i < nbImages && Math.pow(i, 2) <= nbImages; ++i) {
                    divisor = i;
                }
                divisor += 1;
                double nbTiles = Math.pow(divisor, 2);

                if (nbImages < nbTiles) {
                    divisor -= 1;
                    nbTiles = Math.pow(divisor, 2);
                }
                final int resize = (artistBitMapSize / divisor) + 1;

                final Bitmap bitmap = Bitmap.createBitmap(artistBitMapSize, artistBitMapSize, Bitmap.Config.RGB_565);
                final Canvas canvas = new Canvas(bitmap);

                int x = 0;
                int y = 0;

                for (int i = 0; i < streams.size() && i < nbTiles; ++i) {
                    final Bitmap bitmap1 = ImageUtil.resize(streams.get(i), resize, resize);
                    canvas.drawBitmap(bitmap1, x, y, null);
                    x += resize;

                    if (x >= artistBitMapSize) {
                        x = 0;
                        y += resize;
                    }
                }

                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                result = new ByteArrayInputStream(bos.toByteArray());

            } else if (nbImages > 0) {
                // we return the last cover album of the artist
                Map.Entry<InputStream, Integer> maxEntryYear = null;

                for (final Map.Entry<InputStream, Integer> entry : images.entrySet()) {
                    if (maxEntryYear == null || entry.getValue()
                            .compareTo(maxEntryYear.getValue()) > 0) {
                        maxEntryYear = entry;
                    }
                }

                if (maxEntryYear != null) {
                    result = maxEntryYear.getKey();
                } else {
                    result = images.entrySet()
                            .iterator()
                            .next()
                            .getKey();
                }

            }
        } finally {
            retriever.release();
            try {
                for (final InputStream stream : streams) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return result;
    }



}







