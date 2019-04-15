package com.voxeet.toolkit.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.voxeet.android.media.MediaStream;
import com.voxeet.sdk.core.VoxeetSdk;
import com.voxeet.sdk.core.preferences.VoxeetPreferences;
import com.voxeet.toolkit.implementation.VoxeetConferenceView;
import com.voxeet.toolkit.views.VideoView;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import eu.codlab.simplepromise.solve.ErrorPromise;
import eu.codlab.simplepromise.solve.PromiseExec;
import eu.codlab.simplepromise.solve.Solver;

public class ConferenceViewRendererControl {

    @NonNull
    private WeakReference<VoxeetConferenceView> parent;

    @NonNull
    private WeakReference<VideoView> selfVideoView;

    @NonNull
    private WeakReference<VideoView> otherVideoView;

    private View.OnClickListener selectedFromSelf = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            VideoView view = otherVideoView.get();

            if (null != view) {
                switchCamera();
            }
        }
    };


    private ConferenceViewRendererControl() {
        parent = new WeakReference<>(null);
        selfVideoView = new WeakReference<>(null);
        otherVideoView = new WeakReference<>(null);
    }

    public ConferenceViewRendererControl(@NonNull VoxeetConferenceView parent,
                                         @NonNull VideoView selfVideoView,
                                         @NonNull VideoView otherVideoView) {
        this.parent = new WeakReference<>(parent);
        this.selfVideoView = new WeakReference<>(selfVideoView);
        this.otherVideoView = new WeakReference<>(otherVideoView);
    }

    @Nullable
    private VoxeetConferenceView getParent() {
        return parent.get();
    }

    @NonNull
    private VideoView getSelfVideoView() {
        return selfVideoView.get();
    }

    @NonNull
    private VideoView getOtherVideoView() {
        return otherVideoView.get();
    }

    public void attachStreamToSelected(@NonNull String peerId,
                                       @NonNull MediaStream stream) {
        VideoView selectedView = getOtherVideoView();

        String ownUserId = VoxeetPreferences.id();
        if (null == ownUserId) ownUserId = "";

        if (ownUserId.equals(peerId)) {
            attachStreamToSelf(stream);
        } else if (selectedView.isAttached() && ownUserId.equals(selectedView.getPeerId())) {
            attachStreamToSelf(stream);

            if (!ownUserId.equals(peerId)) {
                selectedView.setOnClickListener(null);
                selectedView.setClickable(false);
                selectedView.setMirror(false);
                selectedView.unAttach();
                selectedView.setVisibility(View.VISIBLE);
                selectedView.attach(peerId, stream, true);
            } else {
                selectedView.unAttach();
                selectedView.setVisibility(View.GONE);
            }
        } else {
            selectedView.setOnClickListener(null);
            selectedView.setClickable(false);
            selectedView.setMirror(false);
            selectedView.unAttach();
            selectedView.setVisibility(View.VISIBLE);
            selectedView.attach(peerId, stream, true);
        }
    }

    public void detachStreamFromSelected() {
        VideoView selectedView = getOtherVideoView();
        VideoView selfVideoView = getSelfVideoView();

        String ownUserId = VoxeetPreferences.id();

        selectedView.unAttach();

        MediaStream stream = VoxeetSdk.getInstance().getConferenceService()
                .getMapOfStreams().get(ownUserId);

        if (!ToolkitUtils.hasParticipants() && null != stream && stream.videoTracks().size() > 0) {
            attachStreamToSelf(stream);
        } else {
            selectedView.setVisibility(View.GONE);
            getParent().showSpeakerView();
        }
    }

    public void attachStreamToSelf(@android.support.annotation.Nullable MediaStream stream) {
        VideoView selectedView = getOtherVideoView();
        VideoView selfView = getSelfVideoView();

        if (null != stream && stream.videoTracks().size() > 0) {
            String ownUserId = VoxeetPreferences.id();
            if (!ToolkitUtils.hasParticipants()) {
                selfView.unAttach();
                selfView.setVisibility(View.GONE);

                selectedView.setVideoFill();
                selectedView.setOnClickListener(selectedFromSelf);
                selectedView.setMirror(true);
                selectedView.setVisibility(View.VISIBLE);
                selectedView.attach(ownUserId, stream, true);
                getParent().hideSpeakerView();
            } else {
                if (selectedView.isAttached() && ownUserId.equals(selectedView.getPeerId())) {
                    selectedView.setOnClickListener(null);
                    selectedView.setClickable(false);
                    selectedView.unAttach();
                    selectedView.setVisibility(View.GONE);
                    getParent().showSpeakerView();
                }
                selfView.setMirror(true);
                selfView.attach(VoxeetPreferences.id(), stream, true);
                selfView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void detachStreamFromSelf() {
        VideoView selectedView = getOtherVideoView();
        VideoView selfView = getSelfVideoView();

        if (selfView.isAttached()) {
            selfView.unAttach();
            selfView.setVisibility(View.GONE);
        }

        String ownUserId = VoxeetPreferences.id();
        if (selectedView.isAttached() && ownUserId.equals(selectedView.getPeerId())) {
            selectedView.setOnClickListener(null);
            selectedView.setClickable(false);
            selectedView.unAttach();
            selectedView.setVisibility(View.GONE);
            getParent().showSpeakerView();
        }
    }

    public void switchCamera() {
        String ownUserId = VoxeetPreferences.id();
        if (null == ownUserId) ownUserId = "";

        VideoView self = selfVideoView.get();
        VideoView other = otherVideoView.get();

        VideoView finalVideoView = null;

        if (null != self && ownUserId.equals(self.getPeerId())) {
            finalVideoView = self;
        } else if (null != other && ownUserId.equals(other.getPeerId())) {
            finalVideoView = other;
        }

        if (null != finalVideoView && finalVideoView == self) { //force for now the effect only in the SELF VIDEO VIEW
            //until a proper animation is found

            //switchCamera should not trigger crash since it is only possible
            //to click when already capturing and ... rendering the camera

            ObjectAnimator animationFlip = ObjectAnimator.ofFloat(finalVideoView, View.ROTATION_Y, -180f, -360f);
            animationFlip.setInterpolator(new AccelerateDecelerateInterpolator());

            ObjectAnimator animationGrow = ObjectAnimator.ofFloat(finalVideoView, View.SCALE_Y, 1f, 1.15f, 1f);
            animationGrow.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(450).setStartDelay(450);
            animatorSet.playTogether(animationFlip, animationGrow);
            animatorSet.start();
        }

        VoxeetSdk.getInstance()
                .getConferenceService().switchCamera()
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(@android.support.annotation.Nullable Boolean result, @NonNull Solver<Object> solver) {

                    }
                })
                .error(new ErrorPromise() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        error.printStackTrace();
                    }
                });
    }

}