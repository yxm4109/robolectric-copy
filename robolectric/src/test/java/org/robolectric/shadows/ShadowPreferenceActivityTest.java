package org.robolectric.shadows;

import android.preference.PreferenceActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.R;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowPreferenceActivityTest {

  private TestPreferenceActivity activity;
  private ShadowPreferenceActivity shadow;

  @Before
  public void setUp() throws Exception {
    activity = Robolectric.buildActivity(TestPreferenceActivity.class).create().get();
    shadow = Shadows.shadowOf(activity);
  }

  @Test
  public void shouldInitializeListViewInOnCreate() {
    assertThat(activity.getListView()).isNotNull();
  }

  @Test
  public void shouldNotInitializePreferenceScreen() {
    TestPreferenceActivity activity = Robolectric.buildActivity(TestPreferenceActivity.class).get();
    assertThat(activity.getPreferenceScreen()).isNull();
  }

  @Test
  public void shouldRecordPreferencesResourceId() {
    assertThat(shadow.getPreferencesResId()).isEqualTo(-1);
    activity.addPreferencesFromResource(R.xml.preferences);
    assertThat(shadow.getPreferencesResId()).isEqualTo(R.xml.preferences);
  }

  @Test
  public void shouldFindPreferences() {
    activity.addPreferencesFromResource(R.xml.preferences);
    assertNotNull(activity.findPreference("category"));
    assertNotNull(activity.findPreference("inside_category"));
    assertNotNull(activity.findPreference("screen"));
    assertNotNull(activity.findPreference("inside_screen"));
    assertNotNull(activity.findPreference("checkbox"));
    assertNotNull(activity.findPreference("edit_text"));
    assertNotNull(activity.findPreference("list"));
    assertNotNull(activity.findPreference("preference"));
    assertNotNull(activity.findPreference("ringtone"));
  }

  @Test
  public void shouldFindPreferencesWithStringResourceKeyValue() {
    activity.addPreferencesFromResource(R.xml.preferences);
    assertNotNull(activity.findPreference("preference_resource_key_value"));
  }

  private static class TestPreferenceActivity extends PreferenceActivity {
  }
}
