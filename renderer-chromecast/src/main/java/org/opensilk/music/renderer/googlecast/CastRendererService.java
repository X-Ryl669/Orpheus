/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.renderer.googlecast;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.model.Track;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.renderer.IMusicRenderer;
import org.opensilk.music.playback.renderer.PlaybackServiceAccessor;
import org.opensilk.music.renderer.googlecast.server.CastServer;
import org.opensilk.music.renderer.googlecast.server.CastServerUtil;
import org.opensilk.music.renderer.googlecast.server.NetworkUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.*;

/**
 * Created by drew on 10/27/15.
 */
public class CastRendererService extends Service implements IMusicRenderer, AudioManager.OnAudioFocusChangeListener {

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private Context mContext;
    @Inject CastRendererServiceBinder mBinder;
    @Inject MediaRouter mMediaRouter;
    @Inject AudioManager mAudioManager;
    @Inject CastServer mCastServer;
    private String mCastServerUrl;
    @Inject ConnectivityManager mConnectivityManager;
    @Inject WifiManager mWifiManager;
    @Inject TrackResCache mTrackResCache;
    private CastRendererComponent mComponent;
    private GoogleApiClient mApiClient;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private RemoteMediaPlayerCallbacks mRemoteMediaPlayerCallbacks;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private PlaybackServiceAccessor mAccessor;
    private long mCurrentPosition = PLAYBACK_POSITION_UNKNOWN;
    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private boolean mPlayerPrepared;
    private String mSessionId;
    private VolumeProviderCompat mVolumeProvider;
    private MediaRouter.RouteInfo mSelectedRoute;
    private volatile Handler mCallbackHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        CastComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        mComponent = CastRendererComponent.FACTORY.call(parent, this);
        mComponent.inject(this);

        mContext = this;
        mBinder = new CastRendererServiceBinder(this);
        mMediaRouter = MediaRouter.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mComponent = null;
        mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "route_selected".equals(intent.getAction())) {
            mSelectedRoute = mMediaRouter.getSelectedRoute();
        }
        return START_NOT_STICKY;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public PlaybackServiceAccessor getAccessor() {
        return mAccessor;
    }

    public void start() {
        int availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
        if (availability != ConnectionResult.SUCCESS) {
            stopWithError("Google services unavailable");
            return;
        }
        if (mSelectedRoute == null) {
            stopWithError("No route selected");
            return;
        }
        CastDevice device = CastDevice.getFromBundle(mSelectedRoute.getExtras());
        if (device == null || !device.isOnLocalNetwork()) {
            stopWithError("Cast device must be on local network");
            return;
        }
        Timber.d("Selected device %s {%s}", device.getFriendlyName(), device.getIpAddress().toString());
        InetAddress bindAddr = null;
        try {
            List<NetworkInterface> networkInterfaces = NetworkUtil.discoverNetworkInterfaces();
            List<InetAddress> bindAddresses = NetworkUtil.discoverBindAddresses(networkInterfaces);
            bindAddr = NetworkUtil.getBindAddressInSubnetOf(
                    networkInterfaces, bindAddresses, device.getIpAddress());
        } catch (IllegalArgumentException e) {
            Timber.e(e, "lookup network");
            bindAddr = null;
        }
        if (bindAddr != null) {
            mCastServerUrl = "http://" + bindAddr.getHostAddress() + ":" + CastServer.SERVER_PORT;
        } else if (CastServerUtil.IS_EMULATOR) {
            Timber.w("Running on emulator local streaming and artwork disabled");
            mCastServerUrl = "http://localhost/";//bogos not used but cant be null
        } else {
            Timber.w("Unable to locate suitable bind address falling back to wifi address");
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                stopWithError("Can only cast on wifi networks");
                return;
            }
            try {
                String myIp = CastServerUtil.getWifiIpAddress(mWifiManager);
                mCastServerUrl = "http://" + myIp + ":" + CastServer.SERVER_PORT;
            } catch (UnknownHostException e) {
                stopWithError("Unable to obtain our ip address");
                return;
            }
        }
        try {
            mCastServer.start();
        } catch (Exception e) {
            stopWithError(e.getMessage());
            return;
        }
        mCallbackHandler = new Handler(Looper.myLooper());//same looper as playback
        Timber.d("acquiring a connection to Google Play services for %s", device);
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(device, new CastListener())
                .setVerboseLoggingEnabled(true);
        mApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Cast.API, apiOptionsBuilder.build())
                        //XXX will deadlock, instead we repost callbacks to the handler
//                .setHandler(mCallbackHandler)
                .build();
        ConnectionResult res = mApiClient.blockingConnect();
        if (!res.isSuccess()) {
            stopWithError(res.getErrorMessage());
            return;
        }
        String applicationId = mContext.getString(R.string.cast_id);
        LaunchOptions.Builder launchOptions = new LaunchOptions.Builder()
                .setRelaunchIfRunning(true);
        Cast.ApplicationConnectionResult castRes;
        int retries = 0;
        do {
            Timber.d("launching cast application try %d", retries);
            castRes = Cast.CastApi.launchApplication(mApiClient, applicationId,
                    launchOptions.build()).await(30, TimeUnit.SECONDS);
            launchOptions.setRelaunchIfRunning(false);
        } while (!castRes.getStatus().isSuccess() && retries++ < 3);
        if (!castRes.getStatus().isSuccess()) {
            stopWithError(castRes.getStatus().getStatusMessage());
            return;
        }
        mSessionId = castRes.getSessionId();
        attachMediaChannel();
        RemoteMediaPlayer.MediaChannelResult playerRes = mRemoteMediaPlayer.requestStatus(mApiClient).await();
        if (!playerRes.getStatus().isSuccess()) {
            stopWithError(playerRes.getStatus().getStatusMessage());
            return;
        }
    }

    private void reset() {
        mState = STATE_STOPPED;
        mCurrentPosition = 0;
        mPlayerPrepared = false;
        mPlayOnFocusGain = false;
        giveUpAudioFocus();
    }

    public void stop(boolean notifyListeners) {
        reset();
        // Relax all resources
        if (mApiClient != null && mApiClient.isConnected()) {
            Cast.CastApi.stopApplication(mApiClient, mSessionId);
            mApiClient.disconnect();
        }
        mRemoteMediaPlayer = null;
        mRemoteMediaPlayerCallbacks = null;
        mApiClient = null;
        mCallbackHandler = null;
        mVolumeProvider = null;
        //mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        if (mCastServer.isStarted()) {
            try {
                mCastServer.stop();
            } catch (Exception ignored) {
            }
        }
        if (notifyListeners) {
            notifyOnPlaybackStatusChanged(mState);
        }
    }

    private void stopWithError(String error) {
        stop(false);
        mState = STATE_ERROR;
        notifyOnError(error);
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (hasCurrent() && mRemoteMediaPlayer.getMediaStatus()
                .getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING);
    }

    public long getCurrentStreamPosition() {
        //we don't actually seek to the saved position until playback starts
        //so we only ask the player where it is if we are playing
        return (hasCurrent() && PlaybackStateHelper.isPlaying(mState)) ?
                mRemoteMediaPlayer.getApproximateStreamPosition() : mCurrentPosition;
    }

    public long getDuration() {
        return (hasCurrent() && mPlayerPrepared) ?
                mRemoteMediaPlayer.getStreamDuration() : PLAYBACK_POSITION_UNKNOWN;
    }

    public void prepareForTrack() {
        reset();
        if (hasCurrent()) {
            mRemoteMediaPlayer.stop(mApiClient);
        }
        mState = STATE_CONNECTING;
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public boolean loadTrack(final Bundle trackBundle) {
        if (mState == STATE_ERROR) {
            return false;
        }

        Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
        Track.Res trackRes = track.getResources().get(0);

        if (!CastServerUtil.verifyTrackResForEmulator(trackRes)) {
            return false;
        }

        mTrackResCache.put(trackRes.getUri(), trackRes);

        MediaMetadataCompat mmc = mAccessor.convertTrackToMediaMetadata(trackBundle);
        MediaInfo media = CastServerUtil.makeMediaInfo(track, mmc, mCastServerUrl);

        MediaQueueItem item = new MediaQueueItem.Builder(media).build();

        try {
            RemoteMediaPlayer.MediaChannelResult result =
                    mRemoteMediaPlayer.load(mApiClient, media, true).await();
//                    mRemoteMediaPlayer.queueLoad(mApiClient, new MediaQueueItem[]{item}, 0,
//                            MediaStatus.REPEAT_MODE_REPEAT_OFF, null).await();

            if (!result.getStatus().isSuccess()) {
                Timber.e("Error loading track %s", track.getUri());
                return false;
            }

        } catch (IllegalArgumentException e) {
            Timber.d(e, "loadTrack");
            return false;
        }

        mPlayerPrepared = true;

        mState = STATE_BUFFERING;
        notifyOnPlaybackStatusChanged(mState);

        return true;
    }

    public void prepareForNextTrack() {
        if (hasCurrent()) {
            MediaStatus status = mRemoteMediaPlayer.getMediaStatus();
            if (status.getQueueItemCount() > 1) {
                int current = status.getCurrentItemId();
                int[] ids = new int[status.getQueueItemCount()-1];
                int ii=0;
                for (MediaQueueItem item : status.getQueueItems()) {
                    if (item.getItemId() != current) {
                        ids[ii++] = item.getItemId();
                    }
                }
                try {
                    mRemoteMediaPlayer.queueRemoveItems(mApiClient, ids, null).await();
                } catch (IllegalArgumentException e) {
                    Timber.e(e, "prepareForNextTrack");
                }
            }
        }
    }

    @DebugLog
    public boolean loadNextTrack(final Bundle trackBundle) {
        if (mState == STATE_ERROR) {
            return false;
        }

        Track track = Track.BUNDLE_CREATOR.fromBundle(trackBundle);
        Track.Res trackRes = track.getResources().get(0);

        if (!CastServerUtil.verifyTrackResForEmulator(trackRes)) {
            return false;
        }

        mTrackResCache.put(trackRes.getUri(), trackRes);

        MediaMetadataCompat mmc = mAccessor.convertTrackToMediaMetadata(trackBundle);
        MediaInfo media = CastServerUtil.makeMediaInfo(track, mmc, mCastServerUrl);
        MediaQueueItem queueItem = new MediaQueueItem.Builder(media)
//                .setAutoplay(true)
//                .setPreloadTime(10.0f)
                .build();

        try {
            RemoteMediaPlayer.MediaChannelResult result = mRemoteMediaPlayer
                    .queueAppendItem(mApiClient, queueItem, null).await();
            if (!result.getStatus().isSuccess()) {
                Timber.e(result.getStatus().getStatusMessage());
                return false;
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "loadNextTrack");
            return false;
        }

        return true;
    }

    @DebugLog
    public void play() {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        if (mState == PlaybackStateCompat.STATE_PAUSED && hasCurrent() && mPlayerPrepared) {
            configMediaPlayerState();
        } //else wait for prepared
    }

    public boolean hasCurrent() {
        return mRemoteMediaPlayer != null &&
                mPlayerPrepared && mRemoteMediaPlayer.getMediaStatus().getQueueItemCount() > 0;
    }

    public boolean hasNext() {
        return mRemoteMediaPlayer != null &&
                mPlayerPrepared && mRemoteMediaPlayer.getMediaStatus().getQueueItemCount() > 1;
    }

    public void goToNext() {
        if (!hasNext()) {
            throw  new IllegalStateException("Next player not initialized");
        }
        mCurrentPosition = 0;
        mPlayOnFocusGain = true;
        RemoteMediaPlayer.MediaChannelResult result = mRemoteMediaPlayer.queueNext(mApiClient, null).await();
        if (result.getStatus().isSuccess()) {
            notifyOnWentToNext();
            configMediaPlayerState();
        } else {
            mState = STATE_ERROR;
            notifyOnError(result.getStatus().getStatusMessage());
        }
    }

    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (hasCurrent()) {
                RemoteMediaPlayer.MediaChannelResult result = mRemoteMediaPlayer
                        .pause(mApiClient, null).await();
                if (result.getStatus().isSuccess()) {
                    mCurrentPosition = mRemoteMediaPlayer.getApproximateStreamPosition();
                } else {
                    Timber.e("Failed to pause remote player: %s", result.getStatus().getStatusMessage());
                    //TODO
                }
            }
        }
        // while paused, retain the MediaPlayer but give up audio focus
        giveUpAudioFocus();
        mPlayOnFocusGain = false;
        mState = PlaybackStateCompat.STATE_PAUSED;
        notifyOnPlaybackStatusChanged(mState);
    }

    @DebugLog
    public void seekTo(long position) {
        if (mState == STATE_ERROR) {
            return;
        }
        if (!hasCurrent() || !mPlayerPrepared) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            mRemoteMediaPlayer.seek(mApiClient, position, RemoteMediaPlayer.RESUME_STATE_UNCHANGED);
//            mState = PlaybackStateCompat.STATE_BUFFERING;
//            notifyOnPlaybackStatusChanged(mState);
        }
    }

    @Override
    public boolean isRemotePlayback() {
        return true;
    }

    @Override
    public VolumeProviderCompat getVolumeProvider() {
        setupVolumeProvider();
        return mVolumeProvider;
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setAccessor(PlaybackServiceAccessor accessor) {
        mAccessor = accessor;
    }

    /**
     * Try to get the system audio focus.
     */
    @DebugLog
    private void tryToGetAudioFocus() {
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    @DebugLog
    private void giveUpAudioFocus() {
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    @DebugLog
    private void configMediaPlayerState() {
        Timber.d("configMediaPlayerState. mAudioFocus=%d", mAudioFocus);
        if (mState == STATE_ERROR) {
            return;
        }
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            pause();
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                if (hasCurrent()) {
                    mRemoteMediaPlayer.setStreamVolume(mApiClient, VOLUME_DUCK); // we'll be relatively quiet
                }
            } else {
                if (hasCurrent()) {
                    mRemoteMediaPlayer.setStreamVolume(mApiClient, VOLUME_NORMAL); // we can be loud again
                }
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (hasCurrent()) {
                    MediaStatus status = mRemoteMediaPlayer.getMediaStatus();
                    if (status != null && status.getPlayerState() != MediaStatus.PLAYER_STATE_PLAYING) {
                        if (mCurrentPosition == mRemoteMediaPlayer.getApproximateStreamPosition()) {
                            mRemoteMediaPlayer.play(mApiClient);
                            //mState = PlaybackStateCompat.STATE_PLAYING;
                        } else {
                            Timber.d("configMediaPlayerState startMediaPlayer. " +
                                    "seeking to %s", mCurrentPosition);
                            mRemoteMediaPlayer.seek(mApiClient, mCurrentPosition, RemoteMediaPlayer.RESUME_STATE_PLAY);
                            //mState = PlaybackStateCompat.STATE_BUFFERING;
                        }
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        notifyOnPlaybackStatusChanged(mState);
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    @DebugLog
    public void onAudioFocusChange(int focusChange) {
        Timber.d("onAudioFocusChange. focusChange=%d", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Timber.d("onAudioFocusChange: Ignoring unsupported focusChange: %d", focusChange);
        }
        configMediaPlayerState();
    }

    private void setupVolumeProvider() {
        int volume = 100;
        if (mApiClient != null) {
            double castVol = Cast.CastApi.getVolume(mApiClient);
            volume = (int) Math.min(100, Math.round(castVol * 100));
        }
        mVolumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 100, volume) {
            @Override
            public void onSetVolumeTo(int volume) {
                if (mApiClient != null) {
                    double castVol = Math.min(1.0, ((double) 100 / (double) volume));
                    try {
                        Cast.CastApi.setVolume(mApiClient, castVol);
                        setCurrentVolume(volume);
                    } catch (IOException e) {
                        Timber.e(e, "setVolume");
                    }
                }
            }

            @Override
            public void onAdjustVolume(int direction) {
                if (mApiClient != null) {
                    double castVol = Cast.CastApi.getVolume(mApiClient);
                    if (direction > 0) {
                        castVol = Math.min(1.0, castVol + 0.05);
                    } else if (direction < 0) {
                        castVol = Math.max(0.0, castVol - 0.05);
                    } else {
                        return;
                    }
                    try {
                        Cast.CastApi.setVolume(mApiClient, castVol);
                        int newVol = (int) Math.round(castVol * 100);
                        setCurrentVolume(Math.max(0, Math.min(100, newVol)));
                    } catch (IOException e) {
                        Timber.e(e, "onAdjustVolume");
                    }
                }
            }
        };
    }

    private void attachMediaChannel() {
        Timber.d("attachMediaChannel()");
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayerCallbacks = new RemoteMediaPlayerCallbacks();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(mRemoteMediaPlayerCallbacks);
        mRemoteMediaPlayer.setOnPreloadStatusUpdatedListener(mRemoteMediaPlayerCallbacks);
        mRemoteMediaPlayer.setOnMetadataUpdatedListener(mRemoteMediaPlayerCallbacks);
        mRemoteMediaPlayer.setOnQueueStatusUpdatedListener(mRemoteMediaPlayerCallbacks);
        try {
            Timber.d("Registering MediaChannel namespace");
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                    mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
        } catch (IOException | IllegalStateException e) {
            Timber.e(e, "attachMediaChannel()");
        }
    }

    class RemoteMediaPlayerCallbacks implements
            RemoteMediaPlayer.OnMetadataUpdatedListener,
            RemoteMediaPlayer.OnPreloadStatusUpdatedListener,
            RemoteMediaPlayer.OnQueueStatusUpdatedListener,
            RemoteMediaPlayer.OnStatusUpdatedListener {

        @Override
        public void onStatusUpdated() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                if (mApiClient == null || mRemoteMediaPlayer == null
                        || mRemoteMediaPlayer.getMediaStatus() == null) {
                    Timber.d("mApiClient or mRemoteMediaPlayer is null, so will not proceed");
                    return;
                }
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                int state = mediaStatus.getPlayerState();
                if (state == MediaStatus.PLAYER_STATE_PLAYING) {
                    Timber.d("RemoteMediaPlayer::onStatusUpdated() Player status = playing");
                    if (mState != STATE_PLAYING) {
                        mState = STATE_PLAYING;
                        notifyOnPlaybackStatusChanged(mState);
                    }
                    if (mediaStatus.getQueueItemCount() > 1) {
                        if (mediaStatus.getQueueItem(1).getItemId() == mediaStatus.getCurrentItemId()) {
                            notifyOnWentToNext();
                        }
                    }
                } else if (state == MediaStatus.PLAYER_STATE_PAUSED) {
                    Timber.d("RemoteMediaPlayer::onStatusUpdated() Player status = paused");
                    if (mState != STATE_PAUSED) {
                        mState = STATE_PAUSED;
                        notifyOnPlaybackStatusChanged(mState);
                    }
                } else if (state == MediaStatus.PLAYER_STATE_BUFFERING) {
                    Timber.d("RemoteMediaPlayer::onStatusUpdated() Player status = buffering");
                    if (mState != STATE_BUFFERING) {
                        mState = STATE_BUFFERING;
                        notifyOnPlaybackStatusChanged(mState);
                    }
                } else if (state == MediaStatus.PLAYER_STATE_IDLE) {
                    Timber.d("RemoteMediaPlayer::onStatusUpdated() Player status = idle");
                    int idleReason = mediaStatus.getIdleReason();
                    switch (idleReason) {
                        case MediaStatus.IDLE_REASON_FINISHED: {
                            Timber.d("RemoteMediaPlayer::onStatusUpdated() IDLE reason = FINISHED");
//                            reset();
//                            notifyOnCompletion();
                            break;
                        }
                        case MediaStatus.IDLE_REASON_ERROR: {
                            // something bad happened on the cast device
                            Timber.d("RemoteMediaPlayer::onStatusUpdated() IDLE reason = ERROR");
                            stopWithError("The remote cast player encountered an error");
                            break;
                        }
                        case MediaStatus.IDLE_REASON_CANCELED: {
                            Timber.d("RemoteMediaPlayer::onStatusUpdated() IDLE reason = CANCELLED");
                            break;
                        }
                        case MediaStatus.IDLE_REASON_INTERRUPTED: {
                            Timber.d("RemoteMediaPlayer::onStatusUpdated() IDLE reason = INTERRUPTED");
                            break;
                        }
                        default: {
                            Timber.d("onRemoteMediaPlayerStatusUpdated(): Unexpected Idle Reason %d", idleReason);
                            stopWithError("Unknown Idle reason");
                            break;
                        }
                    }
                } else {
                    Timber.d("RemoteMediaPlayer::onStatusUpdated() Player status = unknown");
                }
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onStatusUpdated();
                    }
                });
            }
        }

        @Override
        public void onPreloadStatusUpdated() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                Timber.d("RemoteMediaPlayer::onPreloadStatusUpdated() is reached");
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPreloadStatusUpdated();
                    }
                });
            }
        }

        @Override
        public void onMetadataUpdated() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                Timber.d("RemoteMediaPlayer::onMetadataUpdated() is reached");
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onMetadataUpdated();
                    }
                });
            }
        }

        @Override
        public void onQueueStatusUpdated() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                Timber.d("RemoteMediaPlayer::onQueueStatusUpdated() is reached");
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onQueueStatusUpdated();
                    }
                });
            }
        }
    }

    class CastListener extends Cast.Listener {
        @Override
        public void onApplicationStatusChanged() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                try {
                    String appStatus = Cast.CastApi.getApplicationStatus(mApiClient);
                    Timber.d("onApplicationStatusChanged() reached: " + appStatus);
                } catch (IllegalStateException e) {
                    Timber.d("onApplicationStatusChanged()", e);
                }
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onApplicationStatusChanged();
                    }
                });
            }
        }

        @Override
        public void onApplicationDisconnected(final int statusCode) {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                Timber.d("onApplicationDisconnected() reached with error code: %d", statusCode);
                stopWithError("Application disconnected code=" + statusCode);
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onApplicationDisconnected(statusCode);
                    }
                });
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mCallbackHandler == null) return;
            if (Looper.myLooper() == mCallbackHandler.getLooper()) {
                Timber.d("onVolumeChanged() reached");
                if (mState != STATE_ERROR) {
                    double volume = Cast.CastApi.getVolume(mApiClient);
                    Timber.d("new volume %f", volume);
                }
            } else {
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onVolumeChanged();
                    }
                });
            }
        }
    }

    private void notifyOnPlaybackStatusChanged(int state) {
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(state);
        }
    }

    private void notifyOnWentToNext() {
        if (mCallback != null) {
            mCallback.onWentToNext();
        }
    }

    private void notifyOnCompletion() {
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    private void notifyOnError(String error) {
        if (mCallback != null) {
            mCallback.onError(error);
        }
    }

    private void notifyOnAudioSessionId(int audioSessionId) {
        if (mCallback != null) {
            mCallback.onAudioSessionId(audioSessionId);
        }
    }

}
