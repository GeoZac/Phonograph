package com.kabouzeid.gramophone.appwidgets;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.kabouzeid.appthemehelper.util.TintHelper;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.appwidgets.base.BaseAppWidget;
import com.kabouzeid.gramophone.glide.SongGlideRequest;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteWrapper;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.activities.MainActivity;
import com.kabouzeid.gramophone.util.NotificationColorUtil;
import com.kabouzeid.gramophone.util.Util;

import java.util.List;


public class AppWidgetClassic extends BaseAppWidget {

    //google's code
    public static final String NAME = "app_widget_classic";
    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private static final float POPULATION_FRACTION_FOR_MORE_VIBRANT = 1; // 1f
    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two
     * colors
     */
    private static final float MIN_SATURATION_WHEN_DECIDING = 0.19f;
    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private static final double MINIMUM_IMAGE_FRACTION = 0.001;
    /**
     * The population fraction to select the dominant color as the text color over a the colored
     * ones.
     */
    //private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;
    private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;
    /**
     *
     */
    private static final float POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.4f;  //2.5f higher is less chance sets background white or black
    private static final float BLACK_MAX_LIGHTNESS = 0.08f;
    private static final float WHITE_MIN_LIGHTNESS = 0.90f;
    private static final int RESIZE_BITMAP_AREA = 150 * 150;  //150 * 150
    private static AppWidgetClassic mInstance;

    //end google's code
    private static int imageSize = 0;
    private static float cardRadius = 0f;
    private float[] mFilteredBackgroundHsl = null;
    private Palette.Filter mBlackWhiteFilter = (rgb, hsl) -> !isWhiteOrBlack(hsl);
    private Target<BitmapPaletteWrapper> target; // for cancellation

    public static synchronized AppWidgetClassic getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetClassic();
        }
        return mInstance;
    }


// fading Image code

    public static Drawable getVectorDrawable(@NonNull Resources res, @DrawableRes int resId, @Nullable Resources.Theme theme) {
        if (Build.VERSION.SDK_INT >= 21) {
            return res.getDrawable(resId, theme);
        }
        return VectorDrawableCompat.create(res, resId, theme);
    }

    //end fade image code

    public static Drawable getVibrantTintedVectorDrawables(@NonNull Context context, @DrawableRes int id, @ColorInt int color) {
        return TintHelper.createTintedDrawable(getVectorDrawable(context.getResources(), id, context.getTheme()), color);
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // Color is Light
        if (brightness >= 200) {
            rtnValue = true;
        }
        return rtnValue;
    }

    public Bitmap addGradient(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();

        int byFour = w / 4;
        int ByThird = w - byFour;

        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();

        LinearGradient shader = new LinearGradient(0, 0, ByThird + 47, 0, 0xFFFFFFFF, 0x00FFFFFF, TileMode.CLAMP);

        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

        canvas.drawRect(0, 0, w, h, paint);
        return overlay;
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    protected void defaultAppWidget(final Context context, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(context.getPackageName(), R.layout.app_widget_classic);


        appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art);  //sets the default album art on startup

        appWidgetView.setInt(R.id.content, "setBackgroundColor", Color.BLACK);
        appWidgetView.setInt(R.id.media_titles, "setBackgroundColor", Color.BLACK);
        appWidgetView.setInt(R.id.media_actions, "setBackgroundColor", Color.BLACK);

        appWidgetView.setImageViewBitmap(R.id.button_next, createBitmap(Util.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, false)), 1.3f));
        appWidgetView.setImageViewBitmap(R.id.button_prev, createBitmap(Util.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, false)), 1.3f));
        appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, createBitmap(Util.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, MaterialValueHelper.getSecondaryTextColor(context, false)), 1.3f));


        linkButtons(context, appWidgetView);
        pushUpdate(context, appWidgetIds, appWidgetView);
    }


//googles code

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(final MusicService service, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(service.getPackageName(), R.layout.app_widget_classic);


        final boolean isPlaying = service.isPlaying();
        final Song song = service.getCurrentSong();


        // Set the titles and artwork
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {

            // this is setting the default background colors black and text colors white ok
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE);
            appWidgetView.setTextViewText(R.id.title, song.title);
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song));
            appWidgetView.setTextColor(R.id.title, Color.WHITE);
            appWidgetView.setTextColor(R.id.text, Color.WHITE);

        }

        // Link actions buttons to intents
        linkButtons(service, appWidgetView);

        if (imageSize == 0)
            //  imageSize = service.getResources().getDimensionPixelSize(R.dimen.app_widget_classic_image_size);
            imageSize = service.getResources().getDimensionPixelSize(R.dimen.app_widget_classic_image_size) * 2;
        if (cardRadius == 0f)
            cardRadius = 0f;


        final Context appContext = service.getApplicationContext();


        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (target != null) {
                    Glide.clear(target);
                }
                target = SongGlideRequest.Builder.from(Glide.with(service), song)
                        .checkIgnoreMediaStore(service)

                        .generatePalette(service).build()

                        .into(new SimpleTarget<BitmapPaletteWrapper>(imageSize, imageSize) {
                            @Override
                            public void onResourceReady(BitmapPaletteWrapper resource, GlideAnimation<? super BitmapPaletteWrapper> glideAnimation) {
                                Palette palette = resource.getPalette();

                                update(resource.getBitmap(), palette.getVibrantColor(palette.getMutedColor(MaterialValueHelper.getSecondaryTextColor(appContext, true))));

                            }


                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                // do all bitmap is null stuff here gets rid of flickering
                                int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
                                appWidgetView.setInt(R.id.content, "setBackgroundColor", Color.BLACK);
                                appWidgetView.setInt(R.id.media_titles, "setBackgroundColor", Color.BLACK);
                                appWidgetView.setInt(R.id.media_actions, "setBackgroundColor", Color.BLACK);
                                appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, createBitmap(Util.getTintedVectorDrawable(service, playPauseRes, Color.WHITE), 1.4f));
                                appWidgetView.setImageViewBitmap(R.id.button_next, createBitmap(Util.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, Color.WHITE), 1.5f));
                                appWidgetView.setImageViewBitmap(R.id.button_prev, createBitmap(Util.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, Color.WHITE), 1.4f));
                                appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art);
                                appWidgetView.setTextColor(R.id.title, Color.WHITE);
                                appWidgetView.setTextColor(R.id.text, Color.WHITE);
                                appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art);  //sets the default album art on startup

                                Bitmap myBmp = createBitmap(Util.getVectorDrawable(service, R.drawable.default_album_art), 1f);

                                final Bitmap myBitmapsImage = addGradient(myBmp);
                                appWidgetView.setImageViewBitmap(R.id.image, myBitmapsImage);

                                pushUpdate(appContext, appWidgetIds, appWidgetView);
                            }

                            private void update(@Nullable Bitmap bitmap, int color) {
                                // Set correct drawable for pause state


                                final Drawable image = getAlbumArtDrawable(service.getResources(), bitmap);
                                final Bitmap roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, cardRadius, 0, cardRadius, 0);


                                // get some colors for text and background
                                if (bitmap != null) {


                                    int backgroundColor;

                                    int byFour = roundedBitmap.getWidth() / 2;
                                    int myLeft = roundedBitmap.getWidth() - byFour;


                                    //Get some colors :)
                                    //left top right bottom   //maximumColorCount()
                                    // for the background we only take the left (changed to right) side of the image to ensure
                                    // a smooth transition

                                    // googles .setRegion(0, 0, bitmap.getWidth() / 2, bitmap.getHeight())
                                    // mine .setRegion(myLeft, 0, roundedBitmap.getWidth(), bitmap.getHeight())
                                    Palette.Builder paletteBuilder = Palette.from(roundedBitmap)//       Palette palette = new Palette.Builder(roundedBitmap).setRegion(myLeft, 5, roundedBitmap.getWidth(), bitmap.getHeight())
                                            .setRegion(myLeft, 0, roundedBitmap.getWidth(), bitmap.getHeight())

                                            .clearFilters();// we want all colors, red / white / black ones too!
                                    Palette palette = paletteBuilder.generate();
                                    backgroundColor = findBackgroundColorAndFilter(palette);

                                    //try to shift background lighter or darker ??
                                    int myShiftedBackgroundColor = NotificationColorUtil.getShiftedColor(backgroundColor, 1);


                                    setBackgroundColor(myShiftedBackgroundColor);


                                    // we want most of the full region again, slightly shifted to the right
                                    float textColorStartWidthFraction = 0.0f;


                                    //  Palette.Builder textPaletteBuilder = Palette.from(roundedBitmap)
                                    paletteBuilder.setRegion((int) (bitmap.getWidth() * textColorStartWidthFraction), 0,
                                            bitmap.getWidth(),
                                            bitmap.getHeight());
                                    if (mFilteredBackgroundHsl != null) {
                                        paletteBuilder.addFilter((rgb, hsl) -> {
                                            // at least 10 degrees hue difference
                                            float diff = Math.abs(hsl[0] - mFilteredBackgroundHsl[0]);
                                            return diff > 10 && diff < 350;//  return diff > 10 && diff < 350;
                                        });
                                    }

                                    paletteBuilder.addFilter(mBlackWhiteFilter);


                                    Palette TextPalette = paletteBuilder.generate();
                                    int foregroundColor = selectForegroundColor(backgroundColor, TextPalette);


                                    if (NotificationColorUtil.isColorBackgroundLight(backgroundColor)) {
                                        //background was lighter color ??

                                        int myContrastColor;
                                        myContrastColor = NotificationColorUtil.findContrastColor(foregroundColor, backgroundColor, true, 3);  //5 is good so far current is 3


                                        int myShiftedSongTextColor = NotificationColorUtil.getShiftedColor(myContrastColor, 20);
                                        int myShiftedArtistTextColor = NotificationColorUtil.getShiftedColor(myContrastColor, 1);


                                        appWidgetView.setTextColor(R.id.title, myShiftedSongTextColor);
                                        appWidgetView.setTextColor(R.id.text, myShiftedArtistTextColor);


                                        // below sets controls only
                                        setForegroundColor(myShiftedSongTextColor);


                                    } else {

                                        //background was determed to be dark

                                        int myContrastColor;
                                        myContrastColor = NotificationColorUtil.findContrastColorAgainstDark(foregroundColor, backgroundColor, true, 3.5);  //5 is good so far  3.5


                                        int myShiftedSongTextColor = NotificationColorUtil.getShiftedColor(myContrastColor, -1);  //0
                                        int myShiftedArtistTextColor = NotificationColorUtil.getShiftedColor(myContrastColor, 12); //10


                                        appWidgetView.setTextColor(R.id.title, myShiftedSongTextColor);
                                        appWidgetView.setTextColor(R.id.text, myShiftedArtistTextColor);
                                        // below sets controls only
                                        setForegroundColor(myShiftedSongTextColor);

                                    }


                                }

                                final Bitmap myBitmapsImage = addGradient(roundedBitmap);
                                appWidgetView.setImageViewBitmap(R.id.image, myBitmapsImage);


                                pushUpdate(appContext, appWidgetIds, appWidgetView);


                            }


                            private int selectForegroundColor(int backgroundColor, Palette palette) {
                                if (NotificationColorUtil.isColorLight(backgroundColor)) {
                                    return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(),
                                            palette.getVibrantSwatch(),
                                            palette.getDarkMutedSwatch(),
                                            palette.getMutedSwatch(),
                                            palette.getDominantSwatch(),
                                            Color.BLACK);
                                } else {
                                    return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(),
                                            palette.getVibrantSwatch(),
                                            palette.getLightMutedSwatch(),
                                            palette.getMutedSwatch(),
                                            palette.getDominantSwatch(),
                                            Color.WHITE);
                                }
                            }

                            private int selectForegroundColorForSwatches(Palette.Swatch moreVibrant,
                                                                         Palette.Swatch vibrant, Palette.Swatch moreMutedSwatch, Palette.Swatch mutedSwatch,
                                                                         Palette.Swatch dominantSwatch, int fallbackColor) {
                                Palette.Swatch coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant);
                                if (coloredCandidate == null) {
                                    coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch);
                                }
                                if (coloredCandidate != null) {
                                    if (dominantSwatch == coloredCandidate) {
                                        return coloredCandidate.getRgb();
                                    } else if ((float) coloredCandidate.getPopulation() / dominantSwatch.getPopulation()
                                            < POPULATION_FRACTION_FOR_DOMINANT
                                            && dominantSwatch.getHsl()[1] > MIN_SATURATION_WHEN_DECIDING) {
                                        return dominantSwatch.getRgb();
                                    } else {
                                        return coloredCandidate.getRgb();
                                    }
                                } else if (hasEnoughPopulation(dominantSwatch)) {
                                    return dominantSwatch.getRgb();
                                } else {
                                    return fallbackColor;
                                }
                            }

                            private Palette.Swatch selectMutedCandidate(Palette.Swatch first,
                                                                        Palette.Swatch second) {
                                boolean firstValid = hasEnoughPopulation(first);
                                boolean secondValid = hasEnoughPopulation(second);
                                if (firstValid && secondValid) {
                                    float firstSaturation = first.getHsl()[1];
                                    float secondSaturation = second.getHsl()[1];
                                    float populationFraction = first.getPopulation() / (float) second.getPopulation();
                                    if (firstSaturation * populationFraction > secondSaturation) {
                                        return first;
                                    } else {
                                        return second;
                                    }
                                } else if (firstValid) {
                                    return first;
                                } else if (secondValid) {
                                    return second;
                                }
                                return null;
                            }

                            private Palette.Swatch selectVibrantCandidate(Palette.Swatch first, Palette.Swatch second) {
                                boolean firstValid = hasEnoughPopulation(first);
                                boolean secondValid = hasEnoughPopulation(second);
                                if (firstValid && secondValid) {
                                    int firstPopulation = first.getPopulation();
                                    int secondPopulation = second.getPopulation();
                                    if (firstPopulation / (float) secondPopulation
                                            < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
                                        return second;
                                    } else {
                                        return first;
                                    }
                                } else if (firstValid) {
                                    return first;
                                } else if (secondValid) {
                                    return second;
                                }
                                return null;
                            }

                            private boolean hasEnoughPopulation(Palette.Swatch swatch) {
                                // We want a fraction that is at least 1% of the image
                                return swatch != null
                                        && (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION); //    && (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION);
                            }

                            private int findBackgroundColorAndFilter(Palette palette) {
                                // by default we use the dominant palette
                                Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                                if (dominantSwatch == null) {
                                    // We're not filtering on white or black
                                    mFilteredBackgroundHsl = null;
                                    return Color.WHITE;
                                }
                                if (!isWhiteOrBlack(dominantSwatch.getHsl())) {
                                    mFilteredBackgroundHsl = dominantSwatch.getHsl();
                                    return dominantSwatch.getRgb();
                                }
                                // Oh well, we selected black or white. Lets look at the second color!
                                List<Palette.Swatch> swatches = palette.getSwatches();
                                float highestNonWhitePopulation = -1;
                                Palette.Swatch second = null;
                                for (Palette.Swatch swatch : swatches) {
                                    if (swatch != dominantSwatch
                                            && swatch.getPopulation() > highestNonWhitePopulation
                                            && !isWhiteOrBlack(swatch.getHsl())) {
                                        second = swatch;
                                        highestNonWhitePopulation = swatch.getPopulation();
                                    }
                                }
                                if (second == null) {
                                    // We're not filtering on white or black
                                    mFilteredBackgroundHsl = null;
                                    return dominantSwatch.getRgb();
                                }
                                if (dominantSwatch.getPopulation() / highestNonWhitePopulation
                                        > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
                                    // The dominant swatch is very dominant, lets take it!
                                    // We're not filtering on white or black
                                    mFilteredBackgroundHsl = null;
                                    return dominantSwatch.getRgb();
                                } else {
                                    mFilteredBackgroundHsl = second.getHsl();
                                    return second.getRgb();
                                }
                            }

                            private boolean isWhiteOrBlack(float[] hsl) {
                                return isBlack(hsl) || isWhite(hsl);
                            }

                            /**
                             * @return true if the color represents a color which is close to black.
                             */
                            private boolean isBlack(float[] hslColor) {
                                return hslColor[2] <= BLACK_MAX_LIGHTNESS;
                            }

                            /**
                             * @return true if the color represents a color which is close to white.
                             */
                            private boolean isWhite(float[] hslColor) {
                                return hslColor[2] >= WHITE_MIN_LIGHTNESS;
                            }


                        });


            }


            private void setBackgroundColor(Integer color) {
                appWidgetView.setInt(R.id.content, "setBackgroundColor", color);
                appWidgetView.setInt(R.id.media_titles, "setBackgroundColor", color);
                appWidgetView.setInt(R.id.media_titles, "setBackgroundColor", color);
                appWidgetView.setInt(R.id.media_actions, "setBackgroundColor", color);
            }

            private void setForegroundColor(Integer color) {

                int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
                appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, createBitmap(getVibrantTintedVectorDrawables(service, playPauseRes, color), 1.4f));
                appWidgetView.setImageViewBitmap(R.id.button_next, createBitmap(getVibrantTintedVectorDrawables(service, R.drawable.ic_skip_next_white_24dp, color), 1.5f));
                appWidgetView.setImageViewBitmap(R.id.button_prev, createBitmap(getVibrantTintedVectorDrawables(service, R.drawable.ic_skip_previous_white_24dp, color), 1.4f));


                pushUpdate(appContext, appWidgetIds, appWidgetView);
            }


        });


    }

    private boolean isWhiteOrBlack(float[] hsl) {
        return isBlack(hsl) || isWhite(hsl);
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private boolean isBlack(float[] hslColor) {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS;
    }


    //emd googles code

    /**
     * @return true if the color represents a color which is close to white.
     */
    private boolean isWhite(float[] hslColor) {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS;
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    private void linkButtons(final Context context, final RemoteViews views) {
        Intent action;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        // Home
        action = new Intent(context, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, action, 0);
        views.setOnClickPendingIntent(R.id.image, pendingIntent);
        views.setOnClickPendingIntent(R.id.media_titles, pendingIntent);
        // views.setOnClickPendingIntent(R.id.media_actionsz, pendingIntent);
        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_REWIND, serviceName);
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent);

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName);
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent);

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_SKIP, serviceName);
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent);

    }
}