package org.robolectric.shadows;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import org.robolectric.fakes.RoboIntentSender;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowPendingIntentTest {

  @Test
  public void shouldGetIntentSender() {
    Intent expectedIntent = new Intent();
    PendingIntent service = PendingIntent.getService(null, 0, expectedIntent, 0);

    IntentSender intentSender = service.getIntentSender();
    assertThat(expectedIntent).isEqualTo(((RoboIntentSender) intentSender).intent);
  }

  @Test
  public void getBroadcast_shouldCreateIntentForBroadcast() throws Exception {
    Intent intent = new Intent();
    PendingIntent pendingIntent = PendingIntent.getBroadcast(RuntimeEnvironment.application, 99, intent, 100);
    ShadowPendingIntent shadow = shadowOf(pendingIntent);
    assertThat(shadow.isActivityIntent()).isFalse();
    assertThat(shadow.isBroadcastIntent()).isTrue();
    assertThat(shadow.isServiceIntent()).isFalse();
    assertThat(intent).isEqualTo(shadow.getSavedIntent());
    assertThat((Context) RuntimeEnvironment.application).isEqualTo(shadow.getSavedContext());
    assertThat(shadow.getRequestCode()).isEqualTo(99);
    assertThat(shadow.getFlags()).isEqualTo(100);
  }

  @Test
  public void getActivity_shouldCreateIntentForBroadcast() throws Exception {
    Intent intent = new Intent();
    PendingIntent pendingIntent = PendingIntent.getActivity(RuntimeEnvironment.application, 99, intent, 100);
    ShadowPendingIntent shadow = shadowOf(pendingIntent);
    assertThat(shadow.isActivityIntent()).isTrue();
    assertThat(shadow.isBroadcastIntent()).isFalse();
    assertThat(shadow.isServiceIntent()).isFalse();
    assertThat(intent).isEqualTo(shadow.getSavedIntent());
    assertThat((Context) RuntimeEnvironment.application).isEqualTo(shadow.getSavedContext());
    assertThat(shadow.getRequestCode()).isEqualTo(99);
    assertThat(shadow.getFlags()).isEqualTo(100);
  }

  @Test
  public void getActivities_shouldCreateIntentForBroadcast() throws Exception {
    Intent[] intents = new Intent[] {new Intent(Intent.ACTION_VIEW), new Intent(Intent.ACTION_PICK)};
    PendingIntent pendingIntent = PendingIntent.getActivities(RuntimeEnvironment.application, 99, intents, 100);

    ShadowPendingIntent shadow = shadowOf(pendingIntent);
    assertThat(shadow.getSavedIntents()).isEqualTo(intents);

    pendingIntent.send();
    ShadowApplication application = shadowOf(RuntimeEnvironment.application);
    assertThat(application.getNextStartedActivity()).isEqualTo(intents[0]);
    assertThat(application.getNextStartedActivity()).isEqualTo(intents[1]);
  }

  @Test
  public void getActivities_withBundle_shouldCreateIntentForBroadcast() throws Exception {
    Intent[] intents = new Intent[] {new Intent(Intent.ACTION_VIEW), new Intent(Intent.ACTION_PICK)};
    PendingIntent pendingIntent = PendingIntent.getActivities(RuntimeEnvironment.application, 99, intents, 100, new Bundle());

    ShadowPendingIntent shadow = shadowOf(pendingIntent);
    assertThat(shadow.getSavedIntents()).isEqualTo(intents);

    pendingIntent.send();
    ShadowApplication application = shadowOf(RuntimeEnvironment.application);
    assertThat(application.getNextStartedActivity()).isEqualTo(intents[0]);
    assertThat(application.getNextStartedActivity()).isEqualTo(intents[1]);
  }

  @Test
  public void getService_shouldCreateIntentForBroadcast() throws Exception {
    Intent intent = new Intent();
    PendingIntent pendingIntent = PendingIntent.getService(RuntimeEnvironment.application, 99, intent, 100);
    ShadowPendingIntent shadow = shadowOf(pendingIntent);
    assertThat(shadow.isActivityIntent()).isFalse();
    assertThat(shadow.isBroadcastIntent()).isFalse();
    assertThat(shadow.isServiceIntent()).isTrue();
    assertThat(intent).isEqualTo(shadow.getSavedIntent());
    assertThat((Context) RuntimeEnvironment.application).isEqualTo(shadow.getSavedContext());
    assertThat(shadow.getRequestCode()).isEqualTo(99);
    assertThat(shadow.getFlags()).isEqualTo(100);
  }

  @Test
  public void send_shouldFillInIntentData() throws Exception {
    Intent intent = new Intent();
    Activity context = new Activity();
    PendingIntent forActivity = PendingIntent.getActivity(context, 99, intent, 100);

    Activity otherContext = new Activity();
    Intent fillIntent = new Intent();
    fillIntent.putExtra("TEST", 23);
    forActivity.send(otherContext, 0, fillIntent);

    Intent i = shadowOf(otherContext).getNextStartedActivity();
    assertThat(i).isNotNull();
    assertThat(i).isSameAs(intent);
    assertThat(i.getIntExtra("TEST", -1)).isEqualTo(23);
  }

  @Test
  public void getActivity_withFlagNoCreate_shouldReturnNullIfNoPendingIntentExists() {
    Intent intent = new Intent();

    PendingIntent pendingIntent = PendingIntent.getActivity(RuntimeEnvironment.application, 99, intent,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(pendingIntent).isNull();
  }

  @Test
  public void getActivity_withFlagNoCreate_shouldReturnExistingIntent() {
    Intent intent = new Intent();

    PendingIntent.getActivity(RuntimeEnvironment.application, 99, intent, 100);

    Intent identical = new Intent();
    PendingIntent saved = PendingIntent.getActivity(RuntimeEnvironment.application, 99, identical,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(saved).isNotNull();
    assertThat(intent).isEqualTo(shadowOf(saved).getSavedIntent());
  }

  @Test
  public void getActivities_withFlagNoCreate_shouldReturnNullIfNoPendingIntentExists() {
    Intent[] intents = new Intent[] { new Intent(Intent.ACTION_VIEW), new Intent(Intent.ACTION_PICK) };

    PendingIntent pendingIntent = PendingIntent.getActivities(RuntimeEnvironment.application, 99, intents,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(pendingIntent).isNull();
  }

  @Test
  public void getActivities_withFlagNoCreate_shouldReturnExistingIntent() {
    Intent[] intents = new Intent[] { new Intent(Intent.ACTION_VIEW), new Intent(Intent.ACTION_PICK) };

    PendingIntent.getActivities(RuntimeEnvironment.application, 99, intents, 100);

    Intent[] identicalIntents = new Intent[] { new Intent(Intent.ACTION_VIEW), new Intent(Intent.ACTION_PICK) };
    PendingIntent saved = PendingIntent.getActivities(RuntimeEnvironment.application, 99, identicalIntents,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(saved).isNotNull();
    assertThat(intents).isEqualTo(shadowOf(saved).getSavedIntents());
  }

  @Test
  public void getBroadcast_withFlagNoCreate_shouldReturnNullIfNoPendingIntentExists() {
    Intent intent = new Intent();

    PendingIntent pendingIntent = PendingIntent.getBroadcast(RuntimeEnvironment.application, 99, intent,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(pendingIntent).isNull();
  }

  @Test
  public void getBroadcast_withFlagNoCreate_shouldReturnExistingIntent() {
    Intent intent = new Intent();

    PendingIntent.getBroadcast(RuntimeEnvironment.application, 99, intent, 100);
    Intent identical = new Intent();
    PendingIntent saved = PendingIntent.getBroadcast(RuntimeEnvironment.application, 99, identical,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(saved).isNotNull();
    assertThat(intent).isEqualTo(shadowOf(saved).getSavedIntent());
  }

  @Test
  public void getService_withFlagNoCreate_shouldReturnNullIfNoPendingIntentExists() {
    Intent intent = new Intent();

    PendingIntent pendingIntent = PendingIntent.getService(RuntimeEnvironment.application, 99, intent,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(pendingIntent).isNull();
  }

  @Test
  public void getService_withFlagNoCreate_shouldReturnExistingIntent() {
    Intent intent = new Intent();

    PendingIntent.getService(RuntimeEnvironment.application, 99, intent, 100);

    Intent identical = new Intent();

    PendingIntent saved = PendingIntent.getService(RuntimeEnvironment.application, 99, identical,
        PendingIntent.FLAG_NO_CREATE);

    assertThat(saved).isNotNull();
    assertThat(intent).isEqualTo(shadowOf(saved).getSavedIntent());
  }
}
