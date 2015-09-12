package org.robolectric.shadows;

import android.widget.CheckBox;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowCheckBoxTest {
  @Test
  public void testWorks() throws Exception {
    CheckBox checkBox = new CheckBox(RuntimeEnvironment.application);
    assertThat(checkBox.isChecked()).isFalse();

    checkBox.setChecked(true);
    assertThat(checkBox.isChecked()).isTrue();

    checkBox.toggle();
    assertThat(checkBox.isChecked()).isFalse();

    checkBox.performClick();  // Used to support performClick(), but Android doesn't. Sigh.
//        assertThat(checkBox.isChecked()).isFalse();
  }
}
