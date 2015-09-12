package org.robolectric.shadows;

import android.app.TimePickerDialog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowTimePickerDialogTest {

  @Test
  public void returnsTheIntialHourAndMinutePassedIntoTheTimePickerDialog() throws Exception {
    TimePickerDialog timePickerDialog = new TimePickerDialog(RuntimeEnvironment.application, 0, null, 6, 55, false);
    ShadowTimePickerDialog shadow = shadowOf(timePickerDialog);
    assertThat(shadow.getHourOfDay()).isEqualTo(6);
    assertThat(shadow.getMinute()).isEqualTo(55);
  }
}
