/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.test.espresso.base;

import android.support.test.runner.lifecycle.ActivityLifecycleCallback;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.espresso.InjectEventSecurityException;
import android.support.test.testapp.R;
import android.support.test.testapp.SendActivity;

import android.app.Activity;
import android.os.Build;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link EventInjector}.
 */
public class EventInjectorTest extends ActivityInstrumentationTestCase2<SendActivity> {
  private static final String TAG = EventInjectorTest.class.getSimpleName();
  private Activity sendActivity;
  private EventInjector injector;
  final AtomicBoolean injectEventWorked = new AtomicBoolean(false);
  final AtomicBoolean injectEventThrewSecurityException = new AtomicBoolean(false);
  final CountDownLatch latch = new CountDownLatch(1);

  @SuppressWarnings("deprecation")
  public EventInjectorTest() {
    // Supporting froyo.
    super("android.support.test.testapp", SendActivity.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (Build.VERSION.SDK_INT > 15) {
      InputManagerEventInjectionStrategy strat = new InputManagerEventInjectionStrategy();
      strat.initialize();
      injector = new EventInjector(strat);
    } else {
      WindowManagerEventInjectionStrategy strat = new WindowManagerEventInjectionStrategy();
      strat.initialize();
      injector = new EventInjector(strat);
    }
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @LargeTest
  public void testInjectKeyEventUpWithNoDown() throws Exception {
    sendActivity = getActivity();

    getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        View view = sendActivity.findViewById(R.id.send_data_edit_text);
        assertTrue(view.requestFocus());
        latch.countDown();
      }
    });

    assertTrue("Timed out!", latch.await(10, TimeUnit.SECONDS));
    KeyCharacterMap keyCharacterMap = UiControllerImpl.getKeyCharacterMap();
    KeyEvent[] events = keyCharacterMap.getEvents("a".toCharArray());
    assertTrue(injector.injectKeyEvent(events[1]));
  }

  @LargeTest
  public void testInjectStaleKeyEvent() throws Exception {
    sendActivity = getActivity();

    getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        View view = sendActivity.findViewById(R.id.send_data_edit_text);
        assertTrue(view.requestFocus());
        latch.countDown();
      }
    });

    assertTrue("Timed out!", latch.await(10, TimeUnit.SECONDS));
    assertFalse("SecurityException exception was thrown.", injectEventThrewSecurityException.get());

    KeyCharacterMap keyCharacterMap = UiControllerImpl.getKeyCharacterMap();
    KeyEvent[] events = keyCharacterMap.getEvents("a".toCharArray());
    KeyEvent event = KeyEvent.changeTimeRepeat(events[0], 1, 0);

    // Stale event does not fail for API < 13.
    if (Build.VERSION.SDK_INT < 13) {
      assertTrue(injector.injectKeyEvent(event));
    } else {
      assertFalse(injector.injectKeyEvent(event));
    }
  }

  @LargeTest
  public void testInjectKeyEvent_securityException() {
    KeyCharacterMap keyCharacterMap = UiControllerImpl.getKeyCharacterMap();
    KeyEvent[] events = keyCharacterMap.getEvents("a".toCharArray());
    try {
      injector.injectKeyEvent(events[0]);
      fail("Should have thrown a security exception!");
    } catch (InjectEventSecurityException expected) { }
  }

  @LargeTest
  public void testInjectMotionEvent_securityException() throws Exception {
    getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        MotionEvent down = MotionEvent.obtain(SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            0,
            0,
            0);
        try {
          injector.injectMotionEvent(down);
        } catch (InjectEventSecurityException expected) {
          injectEventThrewSecurityException.set(true);
        }
        latch.countDown();
      }
    });

    latch.await(10, TimeUnit.SECONDS);
    assertTrue(injectEventThrewSecurityException.get());
  }

  @LargeTest
  public void testInjectMotionEvent_upEventFailure() throws InterruptedException {
    final CountDownLatch activityStarted = new CountDownLatch(1);
    ActivityLifecycleCallback callback = new ActivityLifecycleCallback() {
      @Override
      public void onActivityLifecycleChanged(Activity activity, Stage stage) {
        if (Stage.RESUMED == stage && activity instanceof SendActivity) {
          activityStarted.countDown();
        }
      }
    };
    ActivityLifecycleMonitorRegistry
        .getInstance()
        .addLifecycleCallback(callback);
    try {
      getActivity();
      assertTrue(activityStarted.await(20, TimeUnit.SECONDS));
      final int[] xy = UiControllerImplIntegrationTest.getCoordinatesInMiddleOfSendButton(
          getActivity(), getInstrumentation());

      getInstrumentation().runOnMainSync(new Runnable() {
        @Override
        public void run() {
          MotionEvent up = MotionEvent.obtain(SystemClock.uptimeMillis(),
              SystemClock.uptimeMillis(),
              MotionEvent.ACTION_UP,
              xy[0],
              xy[1],
              0);

          try {
            injectEventWorked.set(injector.injectMotionEvent(up));
          } catch (InjectEventSecurityException e) {
            Log.e(TAG, "injectEvent threw a SecurityException");
          }
          up.recycle();
          latch.countDown();
        }
      });

      latch.await(10, TimeUnit.SECONDS);
      assertFalse(injectEventWorked.get());
    } finally {
      ActivityLifecycleMonitorRegistry
          .getInstance()
          .removeLifecycleCallback(callback);
    }

  }
}
