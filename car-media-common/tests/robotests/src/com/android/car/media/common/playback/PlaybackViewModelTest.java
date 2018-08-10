/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.media.common.playback;

import static com.android.car.arch.common.LiveDataFunctions.dataOf;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.robolectric.RuntimeEnvironment.application;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.support.annotation.NonNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.TestLifecycleOwner;
import com.android.car.media.common.MediaItemMetadata;
import com.android.car.media.common.TestConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PlaybackViewModelTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Mock
    public MediaController mMediaController;
    @Mock
    public MediaMetadata mMediaMetadata;
    @Mock
    public PlaybackState mPlaybackState;
    @Captor
    private ArgumentCaptor<MediaController.Callback> mCapturedCallback;

    private PlaybackViewModel mPlaybackViewModel;

    @Before
    public void setUp() {
        doNothing().when(mMediaController).registerCallback(mCapturedCallback.capture());
        mPlaybackViewModel = new PlaybackViewModel(application);
        mPlaybackViewModel.setMediaController(dataOf(mMediaController));
    }

    @Test
    public void testGetMediaController() {
        CaptureObserver<MediaController> observer = new CaptureObserver<>();
        mPlaybackViewModel.getMediaController().observe(mLifecycleOwner, observer);

        assertThat(observer.getObservedValue()).isEqualTo(mMediaController);
    }

    @Test
    public void testGetMetadata() {
        CaptureObserver<MediaItemMetadata> observer = new CaptureObserver<>();
        mPlaybackViewModel.getMetadata().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onMetadataChanged(mMediaMetadata);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(mMediaMetadata);
    }

    @Test
    public void testGetPlaybackState() {
        CaptureObserver<PlaybackState> observer = new CaptureObserver<>();
        mPlaybackViewModel.getPlaybackState().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onPlaybackStateChanged(mPlaybackState);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(mPlaybackState);
    }

    @Test
    public void testGetSanitizedQueue() {
        String title = "title";
        int queueId = 1;
        MediaSession.QueueItem queueItem = createQueueItem(title, queueId);
        List<MediaSession.QueueItem> queue = Collections.singletonList(queueItem);
        CaptureObserver<List<MediaItemMetadata>> observer = new CaptureObserver<>();
        mPlaybackViewModel.getQueue().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onQueueChanged(queue);

        assertThat(observer.hasBeenNotified()).isTrue();
        List<MediaItemMetadata> observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue).hasSize(1);
        MediaItemMetadata observedItem = observedValue.get(0);
        assertThat(observedItem).isNotNull();
        assertThat(observedItem.getTitle()).isEqualTo(title);
        assertThat(observedItem.getQueueId()).isEqualTo(queueId);
    }

    @Test
    public void testGetHasQueue_null() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        mPlaybackViewModel.hasQueue().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onQueueChanged(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(false);
    }

    @Test
    public void testGetHasQueue_empty() {
        List<MediaSession.QueueItem> queue = Collections.emptyList();
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        mPlaybackViewModel.hasQueue().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onQueueChanged(queue);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(false);
    }

    @Test
    public void testGetHasQueue_true() {
        List<MediaSession.QueueItem> queue = Collections.singletonList(createQueueItem("title", 1));
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        mPlaybackViewModel.hasQueue().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(mCapturedCallback.getValue()).isNotNull();
        mCapturedCallback.getValue().onQueueChanged(queue);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(true);
    }

    @NonNull
    private MediaSession.QueueItem createQueueItem(String title, int queueId) {
        MediaDescription description = new MediaDescription.Builder().setTitle(title).build();
        return new MediaSession.QueueItem(description, queueId);
    }
}