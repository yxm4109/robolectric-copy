package org.robolectric.shadows;

import android.os.Looper;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.annotation.HiddenApi;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.Scheduler;

import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.internal.Shadow.*;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

/**
 * Shadow for {@link android.os.Looper} that enqueues posted {@link Runnable}s to be run
 * (on this thread) later. {@code Runnable}s that are scheduled to run immediately can be
 * triggered by calling {@link #idle()}.
 *
 * @see ShadowMessageQueue
 */
@Implements(Looper.class)
public class ShadowLooper {
  private static final Thread MAIN_THREAD = Thread.currentThread();
  // Replaced SoftThreadLocal with a WeakHashMap, because ThreadLocal make it impossible to access their contents from other
  // threads, but we need to be able to access the loopers for all threads so that we can shut them down when resetThreadLoopers()
  // is called. This also allows us to implement the useful getLooperForThread() method.
  private static Map<Thread, Looper> loopingLoopers = Collections.synchronizedMap(new WeakHashMap<Thread, Looper>());

  private Scheduler scheduler = new Scheduler();
  private @RealObject Looper realObject;

  boolean quit;

  @Resetter
  public static synchronized void resetThreadLoopers() {
    // Blech. We need to keep the main looper because somebody might refer to it in a static
    // field. The other loopers need to be wrapped in WeakReferences so that they are not prevented from
    // being garbage collected.
    if (Thread.currentThread() != MAIN_THREAD) {
      throw new IllegalStateException("you should only be calling this from the main thread!");
    }
    synchronized (loopingLoopers) {
      for (Looper looper : loopingLoopers.values()) {
        synchronized (looper) {
          if (!shadowOf(looper).quit) {
            looper.quit();
          }
        }
      }
    }
    // Because resetStaticState() is called by ParallelUniverse on startup before prepareMainLooper() is
    // called, this might be null on that occasion.
    Looper mainLooper = Looper.getMainLooper();
    if (mainLooper != null) {
      shadowOf(mainLooper).reset();
    }
  }

  @Implementation
  public void __constructor__(boolean quitAllowed) {
    invokeConstructor(Looper.class, realObject, from(boolean.class, quitAllowed));
    if (Thread.currentThread() != MAIN_THREAD) {
      loopingLoopers.put(Thread.currentThread(), realObject);
    }
  }
    
  @Implementation
  public static void loop() {
    shadowOf(Looper.myLooper()).doLoop();
  }

  private void doLoop() {
    if (this != getShadowMainLooper()) {
      synchronized (realObject) {
        while (!quit) {
          try {
            realObject.wait();
          } catch (InterruptedException ignore) {
          }
        }
      }
    }
  }

  @Implementation
  public void quit() {
    if (this == getShadowMainLooper()) throw new RuntimeException("Main thread not allowed to quit");
    quitUnchecked();
  }

  public void quitUnchecked() {
    synchronized (realObject) {
      quit = true;
      realObject.notifyAll();
      scheduler.reset();
    }
  }
  
  @HiddenApi @Implementation
  public int postSyncBarrier() {
    return 1;
  }

  @HiddenApi @Implementation
  public void removeSyncBarrier(int token) {
  }

  public boolean hasQuit() {
    synchronized (realObject) {
      return quit;
    }
  }

  public static ShadowLooper getShadowMainLooper() {
    return shadowOf(Looper.getMainLooper());
  }
  
  public static Looper getLooperForThread(Thread thread) {
    return thread == MAIN_THREAD ? Looper.getMainLooper() : loopingLoopers.get(thread);
  }
  
  public static void pauseLooper(Looper looper) {
    shadowOf(looper).pause();
  }

  public static void unPauseLooper(Looper looper) {
    shadowOf(looper).unPause();
  }

  public static void pauseMainLooper() {
    pauseLooper(Looper.getMainLooper());
  }

  public static void unPauseMainLooper() {
    unPauseLooper(Looper.getMainLooper());
  }

  public static void idleMainLooper() {
    shadowOf(Looper.getMainLooper()).idle();
  }

  public static void idleMainLooper(long interval) {
    getShadowMainLooper().idle(interval);
  }

  public static void idleMainLooperConstantly(boolean shouldIdleConstantly) {
    getShadowMainLooper().idleConstantly(shouldIdleConstantly);
  }

  public static void runMainLooperOneTask() {
    getShadowMainLooper().runOneTask();
  }

  public static void runMainLooperToNextTask() {
    getShadowMainLooper().runToNextTask();
  }
    
  /**
   * Runs any immediately runnable tasks previously queued on the UI thread,
   * e.g. by {@link android.app.Activity#runOnUiThread(Runnable)} or {@link android.os.AsyncTask#onPostExecute(Object)}.
   *
   * <p>Note: calling this method does not pause or un-pause the scheduler.</p>
   
   * @see #runUiThreadTasksIncludingDelayedTasks
   */
  public static void runUiThreadTasks() {
    getShadowMainLooper().idle();
  }

  /**
   * Runs all runnable tasks (pending and future) that have been queued on the UI thread. Such tasks may be queued by
   * e.g. {@link android.app.Activity#runOnUiThread(Runnable)} or {@link android.os.AsyncTask#onPostExecute(Object)}.
   *
   * <p>Note: calling this method does not pause or un-pause the scheduler, however the clock is advanced as
   * future tasks are run.</p>
   * 
   * @see #runUiThreadTasks
   */
  public static void runUiThreadTasksIncludingDelayedTasks() {
    getShadowMainLooper().runToEndOfTasks();
  }

  /**
   * Causes {@link Runnable}s that have been scheduled to run immediately to actually run. Does not advance the
   * scheduler's clock;
   */
  public void idle() {
    scheduler.advanceBy(0);
  }

  /**
   * Causes {@link Runnable}s that have been scheduled to run within the next {@code intervalMillis} milliseconds to
   * run while advancing the scheduler's clock.
   *
   * @param intervalMillis milliseconds to advance
   */
  public void idle(long intervalMillis) {
    scheduler.advanceBy(intervalMillis);
  }

  public void idleConstantly(boolean shouldIdleConstantly) {
    scheduler.idleConstantly(shouldIdleConstantly);
  }

  /**
   * Causes all of the {@link Runnable}s that have been scheduled to run while advancing the scheduler's clock to the
   * start time of the last scheduled {@link Runnable}.
   */
  public void runToEndOfTasks() {
    scheduler.advanceToLastPostedRunnable();
  }

  /**
   * Causes the next {@link Runnable}(s) that have been scheduled to run while advancing the scheduler's clock to its
   * start time. If more than one {@link Runnable} is scheduled to run at this time then they will all be run.
   */
  public void runToNextTask() {
    scheduler.advanceToNextPostedRunnable();
  }

  /**
   * Causes only one of the next {@link Runnable}s that have been scheduled to run while advancing the scheduler's
   * clock to its start time. Only one {@link Runnable} will run even if more than one has ben scheduled to run at the
   * same time.
   */
  public void runOneTask() {
    scheduler.runOneTask();
  }

  /**
   * Enqueue a task to be run later.
   *
   * @param runnable    the task to be run
   * @param delayMillis how many milliseconds into the (virtual) future to run it
   * @return true if the runnable is enqueued
   * @see android.os.Handler#postDelayed(Runnable,long)
   * @deprecated Use a {@link android.os.Handler} instance to post to a looper.
   */
  @Deprecated
  public boolean post(Runnable runnable, long delayMillis) {
    if (!quit) {
      scheduler.postDelayed(runnable, delayMillis);
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Enqueue a task to be run ahead of all other delayed tasks.
   *
   * @param runnable    the task to be run
   * @return true if the runnable is enqueued
   * @see android.os.Handler#postAtFrontOfQueue(Runnable)
   * @deprecated Use a {@link android.os.Handler} instance to post to a looper.
   */
  @Deprecated
  public boolean postAtFrontOfQueue(Runnable runnable) {
    if (!quit) {
      scheduler.postAtFrontOfQueue(runnable);
      return true;
    } else {
      return false;
    }
  }

  public void pause() {
    scheduler.pause();
  }

  public void unPause() {
    scheduler.unPause();
  }

  public boolean isPaused() {
    return scheduler.isPaused();
  }

  public boolean setPaused(boolean shouldPause) {
    boolean wasPaused = isPaused();
    if (shouldPause) {
      pause();
    } else {
      unPause();
    }
    return wasPaused;
  }

  /**
   * Causes all enqueued tasks to be discarded, and pause state to be reset
   */
  public void reset() {
    scheduler = new Scheduler();
    shadowOf(realObject.getQueue()).reset();
    quit = false;
  }

  /**
   * Returns the {@link org.robolectric.util.Scheduler} that is being used to manage the enqueued tasks.
   *
   * @return the {@link org.robolectric.util.Scheduler} that is being used to manage the enqueued tasks.
   */
  public Scheduler getScheduler() {
    return scheduler;
  }

  public void runPaused(Runnable r) {
    boolean wasPaused = setPaused(true);
    try {
      r.run();
    } finally {
      if (!wasPaused) unPause();
    }
  }
}
