package propulsar.yonayarit.PresentationLayer.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.microsoft.windowsazure.notifications.NotificationsHandler;
import android.app.NotificationManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import propulsar.yonayarit.DomainLayer.Objects.AnalyticsApplication;
import propulsar.yonayarit.DomainLayer.Services.MyHandler;
import propulsar.yonayarit.DomainLayer.Services.NotificationSettings;
import propulsar.yonayarit.DomainLayer.Services.RegistrationIntentService;
import propulsar.yonayarit.PresentationLayer.CustomViews.GifView;
import propulsar.yonayarit.PresentationLayer.CustomViews.PlayGifView;
import propulsar.yonayarit.R;

public class Splash extends AppCompatActivity {

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ImageView gifDrawable;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Splash.this.NOTIFICATION_SERVICE);
        notificationManager.cancel(MyHandler.NOTIFICATION_ID);

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        Log.i("AnalyticsDeb", "Setting screen name: " + "Splash");
        mTracker.setScreenName("Splash");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());



        gifDrawable = (ImageView) findViewById(R.id.videoView);
        Glide.with(this).load(R.drawable.yonayarithd).diskCacheStrategy(DiskCacheStrategy.SOURCE)/*.listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                animationListener.onException();
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                Handler handler = new Handler();
                if (resource instanceof GifDrawable) {
                    gifDrawable = (GifDrawable) resource;

                    int duration = 0;
                    GifDecoder decoder = gifDrawable.getDecoder();
                    for (int i = 0; i < gifDrawable.getFrameCount(); i++) {
                        duration += decoder.getDelay(i);
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animationListener.onAnimationCompleted();
                            cancelGifAnimation();
                        }
                    }, duration);

                    setGifViewVisable(true);
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animationListener.onAnimationCompleted();
                            cancelGifAnimation();
                        }
                    }, TimeUnit.SECONDS.toMillis(2));
                }
                return false;
            }
        })*/.into(new GlideDrawableImageViewTarget(gifDrawable));





        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //WS.loginFb();
                SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), 0);
                if(sharedPreferences.getBoolean("loggedIn",false)){
                    Intent intent = new Intent(getApplicationContext(), TabActivity.class);
                    startActivity(intent);
                    finish();
                }else {
                    Intent intent = new Intent(getApplicationContext(), Login.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 3600);

    }

}
