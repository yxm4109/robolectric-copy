package org.robolectric.shadows;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.Scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowLooperTest {

  // testName is used when creating background threads. Makes it
  // easier to debug exceptions on background threads when you
  // know what test they are associated with.
  @Rule
  public TestName testName = new TestName();

  // Helper method that starts the thread with the same name as the
  // current test, so that you will know which test invoked it if
  // it has an exception.
  private HandlerThread getHandlerThread() {
    HandlerThread ht = new HandlerThread(testName.getMethodName());
    ht.start();
    return ht;
  }

  // Useful class for checking that a thread's loop() has exited.
  private class QuitThread extends Thread {
    private boolean hasContinued = false;
    private Looper looper;
    private CountDownLatch started = new CountDownLatch(1);
    
    public QuitThread() {
      super(testName.getMethodName());
    }
    
    @Override
    public void run() {
      Looper.prepare();
      looper = Looper.myLooper();
      started.countDown();
      Looper.loop();
      hasContinued = true;
    }
  }
  
  private QuitThread getQuitThread() throws InterruptedException {
    QuitThread qt = new QuitThread();
    qt.start();
    qt.started.await();
    return qt;
  }
  
  @Test
  public void mainLooper_andMyLooper_shouldBeSame_onMainThread() {
    assertThat(Looper.myLooper()).isSameAs(Looper.getMainLooper());
  }

  @Test
  public void differentThreads_getDifferentLoopers() {
    HandlerThread ht = getHandlerThread();
    assertThat(ht.getLooper()).isNotSameAs(Looper.getMainLooper());
  }

  @Test
  public void mainLooperThread_shouldBeTestThread() {
    assertThat(Looper.getMainLooper().getThread()).isSameAs(Thread.currentThread());
  }

  @Test
  public void shadowMainLooper_shouldBeShadowOfMainLooper() {
    assertThat(ShadowLooper.getShadowMainLooper()).isSameAs(shadowOf(Looper.getMainLooper()));
  }
  
  @Test
  public void getLooperForThread_returnsLooperForAThreadThatHasOne() throws InterruptedException {
    QuitThread qt = getQuitThread();
    assertThat(ShadowLooper.getLooperForThread(qt)).isSameAs(qt.looper);
  }
  
  @Test
  public void getLooperForThread_returnsLooperForMainThread() {
    assertThat(ShadowLooper.getLooperForThread(Thread.currentThread())).isSameAs(Looper.getMainLooper());
  }
  
  @Test
  public void idleMainLooper_executesScheduledTasks() {
    final boolean[] wasRun = new boolean[]{false};
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        wasRun[0] = true;
      }
    }, 2000);

    assertThat(wasRun[0]).as("first").isFalse();
    ShadowLooper.idleMainLooper(1999);
    assertThat(wasRun[0]).as("second").isFalse();
    ShadowLooper.idleMainLooper(1);
    assertThat(wasRun[0]).as("last").isTrue();
  }

  @Test
  public void idleConstantly_runsPostDelayedTasksImmediately() {
    ShadowLooper.idleMainLooperConstantly(true);
    final boolean[] wasRun = new boolean[]{false};
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        wasRun[0] = true;
      }
    }, 2000);

    assertThat(wasRun[0]).isTrue();
  }

  @Test(expected = RuntimeException.class)
  public void shouldThrowRuntimeExceptionIfTryingToQuitMainLooper() throws Exception {
    Looper.getMainLooper().quit();
  }

  @Test
  public void shouldNotQueueMessagesIfLooperIsQuit() throws Exception {
    HandlerThread ht = getHandlerThread();
    Looper looper = ht.getLooper();
    looper.quit();
    assertThat(shadowOf(looper).hasQuit()).as("hasQuit").isTrue();
    assertThat(shadowOf(looper).post(new Runnable() {
      @Override public void run() { }
    }, 0)).as("post").isFalse();

    assertThat(shadowOf(looper).postAtFrontOfQueue(new Runnable() {
      @Override
      public void run() {
      }
    })).as("postAtFrontOfQueue").isFalse();
    assertThat(shadowOf(looper).getScheduler().areAnyRunnable()).as("areAnyRunnable").isFalse();
  }

  @Test
  public void shouldThrowawayRunnableQueueIfLooperQuits() throws Exception {
    HandlerThread ht = getHandlerThread();
    Looper looper = ht.getLooper();
    shadowOf(looper).pause();
    shadowOf(looper).post(new Runnable() {
      @Override
      public void run() {
      }
    }, 0);
    looper.quit();
    assertThat(shadowOf(looper).hasQuit()).as("hasQuit").isTrue();
    assertThat(shadowOf(looper).getScheduler().areAnyRunnable()).as("areAnyRunnable").isFalse();
    assertThat(shadowOf(looper.getQueue()).getHead()).as("queue").isNull();
  }

  @Test
  public void threadShouldContinue_whenLooperQuits() throws InterruptedException {
    QuitThread test = getQuitThread();
    assertThat(test.hasContinued).as("beforeJoin").isFalse();
    test.looper.quit();
    test.join(5000);
    assertThat(test.hasContinued).as("afterJoin").isTrue();
  }

  @Test
  public void shouldResetQueue_whenLooperIsReset() {
    HandlerThread ht = getHandlerThread();
    Looper looper = ht.getLooper();
    Handler h = new Handler(looper);
    ShadowLooper sLooper = shadowOf(looper);
    sLooper.pause();
    h.post(new Runnable() {
      @Override
      public void run() {
      }
    });
    assertThat(shadowOf(looper.getQueue()).getHead()).as("queue").isNotNull();
    sLooper.reset();
    assertThat(sLooper.getScheduler().areAnyRunnable()).as("areAnyRunnable").isFalse();
    assertThat(shadowOf(looper.getQueue()).getHead()).as("queue").isNull();
  }

  @Test
  public void resetThreadLoopers_shouldQuitAllNonMainLoopers() throws InterruptedException {
    QuitThread test = getQuitThread();
    assertThat(test.hasContinued).isFalse();
    ShadowLooper.resetThreadLoopers();
    test.join(5000);
    assertThat(test.hasContinued).isTrue();
  }
 
  @Ignore("Not yet implemented (ref #1407)") 
  @Test(timeout = 1000)
  public void whenTestHarnessUsesDifferentThread_shouldStillHaveMainLooper() {
    assertThat(Looper.myLooper()).isNotNull();
  }
  
  @Test
  public void resetThreadLoopers_fromNonMainThread_shouldThrowISE() throws InterruptedException {
    final AtomicReference<Throwable> ex = new AtomicReference<>();
    Thread t = new Thread() {
      @Override
      public void run() {
        try {
          ShadowLooper.resetThreadLoopers();
        } catch (Throwable t) {
          ex.set(t);
        }
      }
    };
    t.start();
    t.join();
    assertThat(ex.get()).isInstanceOf(IllegalStateException.class);
  }
  
  @Test
  public void soStaticRefsToLoopersInAppWorksAcrossTests_shouldRetainSameLooperForMainThreadBetweenResetsButGiveItAFreshScheduler() throws Exception {
    Looper mainLooper = Looper.getMainLooper();
    Scheduler scheduler = shadowOf(mainLooper).getScheduler();
    shadowOf(mainLooper).quit = true;
    assertThat(RuntimeEnvironment.application.getMainLooper()).isSameAs(mainLooper);

    ShadowLooper.resetThreadLoopers();
    Application application = new Application();
    ReflectionHelpers.callInstanceMethod(application, "attach", ReflectionHelpers.ClassParameter.from(Context.class, RuntimeEnvironment.application.getBaseContext()));

    assertThat(Looper.getMainLooper()).as("Looper.getMainLooper()").isSameAs(mainLooper);
    assertThat(application.getMainLooper()).as("app.getMainLooper()").isSameAs(mainLooper);
    assertThat(shadowOf(mainLooper).getScheduler()).as("scheduler").isNotSameAs(scheduler);
    assertThat(shadowOf(mainLooper).hasQuit()).as("quit").isFalse();
  }

  @Test
  public void getMainLooperReturnsNonNullOnMainThreadWhenRobolectricApplicationIsNull() {
      RuntimeEnvironment.application = null;
      assertThat(Looper.getMainLooper()).isNotNull();
  }

  @Test
  public void getMainLooper_shouldBeInitialized_onBackgroundThread_evenWhenRobolectricApplicationIsNull() throws Exception {
    RuntimeEnvironment.application = null;
    final AtomicReference<Looper> mainLooperAtomicReference = new AtomicReference<>();

    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        Looper mainLooper = Looper.getMainLooper();
        mainLooperAtomicReference.set(mainLooper);
      }
    }, testName.getMethodName());
    backgroundThread.start();
    backgroundThread.join();

    assertThat(mainLooperAtomicReference.get()).as("mainLooper").isSameAs(Looper.getMainLooper());
  }
}
