package org.robolectric.manifest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;
import org.robolectric.res.FsFile;
import org.robolectric.res.ResourcePath;
import org.robolectric.test.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP;
import static android.content.pm.ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA;
import static android.content.pm.ApplicationInfo.FLAG_ALLOW_TASK_REPARENTING;
import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static android.content.pm.ApplicationInfo.FLAG_HAS_CODE;
import static android.content.pm.ApplicationInfo.FLAG_KILL_AFTER_RESTORE;
import static android.content.pm.ApplicationInfo.FLAG_PERSISTENT;
import static android.content.pm.ApplicationInfo.FLAG_RESIZEABLE_FOR_SCREENS;
import static android.content.pm.ApplicationInfo.FLAG_RESTORE_ANY_VERSION;
import static android.content.pm.ApplicationInfo.FLAG_SUPPORTS_LARGE_SCREENS;
import static android.content.pm.ApplicationInfo.FLAG_SUPPORTS_NORMAL_SCREENS;
import static android.content.pm.ApplicationInfo.FLAG_SUPPORTS_SCREEN_DENSITIES;
import static android.content.pm.ApplicationInfo.FLAG_SUPPORTS_SMALL_SCREENS;
import static android.content.pm.ApplicationInfo.FLAG_TEST_ONLY;
import static android.content.pm.ApplicationInfo.FLAG_VM_SAFE_MODE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.util.TestUtil.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AndroidManifestTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void parseManifest_shouldReadContentProviders() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithContentProviders.xml");
    assertThat(config.getContentProviders()).hasSize(2);

    assertThat(config.getContentProviders().get(0).getClassName()).isEqualTo("org.robolectric.tester.FullyQualifiedClassName");
    assertThat(config.getContentProviders().get(0).getAuthority()).isEqualTo("org.robolectric");

    assertThat(config.getContentProviders().get(1).getClassName()).isEqualTo("org.robolectric.tester.PartiallyQualifiedClassName");
    assertThat(config.getContentProviders().get(1).getAuthority()).isEqualTo("org.robolectric");
  }

  @Test
  public void parseManifest_shouldReadBroadcastReceivers() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithReceivers.xml");
    assertThat(config.getBroadcastReceivers()).hasSize(8);

    assertThat(config.getBroadcastReceivers().get(0).getClassName()).isEqualTo("org.robolectric.manifest.AndroidManifestTest.ConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(0).getActions()).contains("org.robolectric.ACTION1", "org.robolectric.ACTION2");

    assertThat(config.getBroadcastReceivers().get(1).getClassName()).isEqualTo("org.robolectric.fakes.ConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(1).getActions()).contains("org.robolectric.ACTION_SUPERSET_PACKAGE");

    assertThat(config.getBroadcastReceivers().get(2).getClassName()).isEqualTo("org.robolectric.ConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(2).getActions()).contains("org.robolectric.ACTION_SUBSET_PACKAGE");

    assertThat(config.getBroadcastReceivers().get(3).getClassName()).isEqualTo("org.robolectric.DotConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(3).getActions()).contains("org.robolectric.ACTION_DOT_PACKAGE");

    assertThat(config.getBroadcastReceivers().get(4).getClassName()).isEqualTo("org.robolectric.test.ConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(4).getActions()).contains("org.robolectric.ACTION_DOT_SUBPACKAGE");

    assertThat(config.getBroadcastReceivers().get(5).getClassName()).isEqualTo("com.foo.Receiver");
    assertThat(config.getBroadcastReceivers().get(5).getActions()).contains("org.robolectric.ACTION_DIFFERENT_PACKAGE");

    assertThat(config.getBroadcastReceivers().get(6).getClassName()).isEqualTo("com.bar.ReceiverWithoutIntentFilter");
    assertThat(config.getBroadcastReceivers().get(6).getActions()).isEmpty();

    assertThat(config.getBroadcastReceivers().get(7).getClassName()).isEqualTo("org.robolectric.ConfigTestReceiverPermissionsAndActions");
    assertThat(config.getBroadcastReceivers().get(7).getActions()).contains("org.robolectric.ACTION_RECEIVER_PERMISSION_PACKAGE");
  }

  @Test
  public void parseManifest_shouldReadServices() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithServices.xml");
    assertThat(config.getServices()).hasSize(2);

    assertThat(config.getServices().get(0).getClassName()).isEqualTo("com.foo.Service");
    assertThat(config.getServices().get(0).getActions()).contains("org.robolectric.ACTION_DIFFERENT_PACKAGE");

    assertThat(config.getServices().get(1).getClassName()).isEqualTo("com.bar.ServiceWithoutIntentFilter");
    assertThat(config.getServices().get(1).getActions()).isEmpty();
  }

  @Test(expected = IllegalAccessError.class)
  public void testManifestWithNoApplicationElement() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestNoApplicationElement.xml");
    config.parseAndroidManifest();
  }

  @Test
  public void parseManifest_shouldReadBroadcastReceiversWithMetaData() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithReceivers.xml");

    assertThat(config.getBroadcastReceivers().get(4).getClassName()).isEqualTo("org.robolectric.test.ConfigTestReceiver");
    assertThat(config.getBroadcastReceivers().get(4).getActions()).contains("org.robolectric.ACTION_DOT_SUBPACKAGE");

    Map<String, Object> meta = config.getBroadcastReceivers().get(4).getMetaData().getValueMap();
    Object metaValue = meta.get("org.robolectric.metaName1");
    assertEquals("metaValue1", metaValue);

    metaValue = meta.get("org.robolectric.metaName2");
    assertEquals("metaValue2", metaValue);

    metaValue = meta.get("org.robolectric.metaFalse");
    assertEquals("false", metaValue);

    metaValue = meta.get("org.robolectric.metaTrue");
    assertEquals("true", metaValue);

    metaValue = meta.get("org.robolectric.metaInt");
    assertEquals("123", metaValue);

    metaValue = meta.get("org.robolectric.metaFloat");
    assertEquals("1.23", metaValue);

    metaValue = meta.get("org.robolectric.metaColor");
    assertEquals("#FFFFFF", metaValue);

    metaValue = meta.get("org.robolectric.metaBooleanFromRes");
    assertEquals("@bool/false_bool_value", metaValue);

    metaValue = meta.get("org.robolectric.metaIntFromRes");
    assertEquals("@integer/test_integer1", metaValue);

    metaValue = meta.get("org.robolectric.metaColorFromRes");
    assertEquals("@color/clear", metaValue);

    metaValue = meta.get("org.robolectric.metaStringFromRes");
    assertEquals("@string/app_name", metaValue);

    metaValue = meta.get("org.robolectric.metaStringOfIntFromRes");
    assertEquals("@string/str_int", metaValue);

    metaValue = meta.get("org.robolectric.metaStringRes");
    assertEquals("@string/app_name", metaValue);
  }

  @Test
  public void shouldReadBroadcastReceiverPermissions() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithReceivers.xml");

    assertThat(config.getBroadcastReceivers().get(7).getClassName()).isEqualTo("org.robolectric.ConfigTestReceiverPermissionsAndActions");
    assertThat(config.getBroadcastReceivers().get(7).getActions()).contains("org.robolectric.ACTION_RECEIVER_PERMISSION_PACKAGE");

    assertEquals("org.robolectric.CUSTOM_PERM", config.getBroadcastReceivers().get(7).getPermission());
  }

  @Test
  public void shouldReadTargetSdkVersionFromAndroidManifestOrDefaultToMin() throws Exception {
    assertEquals(42, newConfigWith("android:targetSdkVersion=\"42\" android:minSdkVersion=\"7\"").getTargetSdkVersion());
    assertEquals(7, newConfigWith("android:minSdkVersion=\"7\"").getTargetSdkVersion());
    assertEquals(1, newConfigWith("").getTargetSdkVersion());
  }

  @Test
  public void shouldReadMinSdkVersionFromAndroidManifestOrDefaultToOne() throws Exception {
    assertEquals(17, newConfigWith("android:minSdkVersion=\"17\"").getMinSdkVersion());
    assertEquals(1, newConfigWith("").getMinSdkVersion());
  }

  @Test
  public void shouldReadProcessFromAndroidManifest() throws Exception {
    assertEquals("robolectricprocess", newConfig("TestAndroidManifestWithProcess.xml").getProcessName());
  }

  @Test
  public void shouldReturnPackageNameWhenNoProcessIsSpecifiedInTheManifest() {
    assertEquals("org.robolectric", newConfig("TestAndroidManifestWithNoProcess.xml").getProcessName());
  }

  @Test
  @Config(manifest = "src/test/resources/TestAndroidManifestWithAppMetaData.xml")
  public void shouldReturnApplicationMetaData() throws PackageManager.NameNotFoundException {
    Map<String, Object> meta = newConfig("TestAndroidManifestWithAppMetaData.xml").getApplicationMetaData();

    Object metaValue = meta.get("org.robolectric.metaName1");
    assertEquals("metaValue1", metaValue);

    metaValue = meta.get("org.robolectric.metaName2");
    assertEquals("metaValue2", metaValue);

    metaValue = meta.get("org.robolectric.metaFalse");
    assertEquals("false", metaValue);

    metaValue = meta.get("org.robolectric.metaTrue");
    assertEquals("true", metaValue);

    metaValue = meta.get("org.robolectric.metaInt");
    assertEquals("123", metaValue);

    metaValue = meta.get("org.robolectric.metaFloat");
    assertEquals("1.23", metaValue);

    metaValue = meta.get("org.robolectric.metaColor");
    assertEquals("#FFFFFF", metaValue);

    metaValue = meta.get("org.robolectric.metaBooleanFromRes");
    assertEquals("@bool/false_bool_value", metaValue);

    metaValue = meta.get("org.robolectric.metaIntFromRes");
    assertEquals("@integer/test_integer1", metaValue);

    metaValue = meta.get("org.robolectric.metaColorFromRes");
    assertEquals("@color/clear", metaValue);

    metaValue = meta.get("org.robolectric.metaStringFromRes");
    assertEquals("@string/app_name", metaValue);

    metaValue = meta.get("org.robolectric.metaStringOfIntFromRes");
    assertEquals("@string/str_int", metaValue);

    metaValue = meta.get("org.robolectric.metaStringRes");
    assertEquals("@string/app_name", metaValue);
  }

  @Test
  public void shouldLoadAllResourcesForExistingLibraries() {
    AndroidManifest appManifest = new AndroidManifest(resourceFile("TestAndroidManifest.xml"), resourceFile("res"), resourceFile("assets"));

    // This intentionally loads from the non standard resources/project.properties
    List<String> resourcePaths = stringify(appManifest.getIncludedResourcePaths());
    assertEquals(asList(
        joinPath(".", "src", "test", "resources", "res"),
        joinPath(".", "src", "test", "resources", "lib1", "res"),
        joinPath(".", "src", "test", "resources", "lib1", "..", "lib3", "res"),
        joinPath(".", "src", "test", "resources", "lib1", "..", "lib2", "res")),
        resourcePaths);
  }

  @Test
  public void shouldTolerateMissingRFile() throws Exception {
    AndroidManifest appManifest = new AndroidManifest(resourceFile("TestAndroidManifestWithNoRFile.xml"), resourceFile("res"), resourceFile("assets"));
    assertEquals(appManifest.getPackageName(), "org.no.resources.for.me");
    assertThat(appManifest.getRClass()).isNull();
    assertEquals(appManifest.getResourcePath().getPackageName(), "org.no.resources.for.me");
  }

  @Test
  public void shouldRead1IntentFilter() {
    AndroidManifest appManifest = newConfig("TestAndroidManifestForActivitiesWithIntentFilter.xml");
    appManifest.getMinSdkVersion(); // Force parsing

    ActivityData activityData = appManifest.getActivityData("org.robolectric.shadows.TestActivity");
    final List<IntentFilterData> ifd = activityData.getIntentFilters();
    assertThat(ifd).isNotNull();
    assertThat(ifd.size()).isEqualTo(1);

    final IntentFilterData data = ifd.get(0);
    assertThat(data.getActions().size()).isEqualTo(1);
    assertThat(data.getActions().get(0)).isEqualTo(Intent.ACTION_MAIN);
    assertThat(data.getCategories().size()).isEqualTo(1);
    assertThat(data.getCategories().get(0)).isEqualTo(Intent.CATEGORY_LAUNCHER);
  }

  @Test
  public void shouldReadMultipleIntentFilters() {
    AndroidManifest appManifest = newConfig("TestAndroidManifestForActivitiesWithMultipleIntentFilters.xml");
    appManifest.getMinSdkVersion(); // Force parsing

    ActivityData activityData = appManifest.getActivityData("org.robolectric.shadows.TestActivity");
    final List<IntentFilterData> ifd = activityData.getIntentFilters();
    assertThat(ifd).isNotNull();
    assertThat(ifd.size()).isEqualTo(2);

    IntentFilterData data = ifd.get(0);
    assertThat(data.getActions().size()).isEqualTo(1);
    assertThat(data.getActions().get(0)).isEqualTo(Intent.ACTION_MAIN);
    assertThat(data.getCategories().size()).isEqualTo(1);
    assertThat(data.getCategories().get(0)).isEqualTo(Intent.CATEGORY_LAUNCHER);

    data = ifd.get(1);
    assertThat(data.getActions().size()).isEqualTo(3);
    assertThat(data.getActions().get(0)).isEqualTo(Intent.ACTION_VIEW);
    assertThat(data.getActions().get(1)).isEqualTo(Intent.ACTION_EDIT);
    assertThat(data.getActions().get(2)).isEqualTo(Intent.ACTION_PICK);

    assertThat(data.getCategories().size()).isEqualTo(3);
    assertThat(data.getCategories().get(0)).isEqualTo(Intent.CATEGORY_DEFAULT);
    assertThat(data.getCategories().get(1)).isEqualTo(Intent.CATEGORY_ALTERNATIVE);
    assertThat(data.getCategories().get(2)).isEqualTo(Intent.CATEGORY_SELECTED_ALTERNATIVE);
  }

  @Test
  public void shouldReadTaskAffinity() {
    AndroidManifest appManifest = newConfig("TestAndroidManifestForActivitiesWithTaskAffinity.xml");
    assertThat(appManifest.getTargetSdkVersion()).isEqualTo(16);

    ActivityData activityData = appManifest.getActivityData("org.robolectric.shadows.TestTaskAffinityActivity");
    assertThat(activityData).isNotNull();
    assertThat(activityData.getTaskAffinity()).isEqualTo("org.robolectric.shadows.TestTaskAffinity");
  }

  @Test
  public void shouldReadPermissions() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithPermissions.xml");

    assertThat(config.getUsedPermissions()).hasSize(3);
    assertThat(config.getUsedPermissions().get(0)).isEqualTo(Manifest.permission.INTERNET);
    assertThat(config.getUsedPermissions().get(1)).isEqualTo(Manifest.permission.SYSTEM_ALERT_WINDOW);
    assertThat(config.getUsedPermissions().get(2)).isEqualTo(Manifest.permission.GET_TASKS);
  }

  @Test
  public void shouldReadPartiallyQualifiedActivities() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestForActivities.xml");
    assertThat(config.getActivityDatas()).hasSize(2);
    assertThat(config.getActivityDatas()).containsKey("org.robolectric.shadows.TestActivity");
    assertThat(config.getActivityDatas()).containsKey("org.robolectric.shadows.TestActivity2");
  }

  @Test
  public void shouldReadActivityAliases() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestForActivityAliases.xml");
    assertThat(config.getActivityDatas()).hasSize(2);
    assertThat(config.getActivityDatas()).containsKey("org.robolectric.shadows.TestActivity");
    assertThat(config.getActivityDatas()).containsKey("org.robolectric.shadows.TestActivityAlias");
  }

  @Test
  public void shouldReadIntentFilterWithData() {
    AndroidManifest appManifest = newConfig("TestAndroidManifestForActivitiesWithIntentFilterWithData.xml");
    appManifest.getMinSdkVersion(); // Force parsing

    ActivityData activityData = appManifest.getActivityData("org.robolectric.shadows.TestActivity");
    final List<IntentFilterData> ifd = activityData.getIntentFilters();
    assertThat(ifd).isNotNull();
    assertThat(ifd.size()).isEqualTo(1);

    final IntentFilterData intentFilterData = ifd.get(0);
    assertThat(intentFilterData.getActions().size()).isEqualTo(1);
    assertThat(intentFilterData.getActions().get(0)).isEqualTo(Intent.ACTION_VIEW);
    assertThat(intentFilterData.getCategories().size()).isEqualTo(1);
    assertThat(intentFilterData.getCategories().get(0)).isEqualTo(Intent.CATEGORY_DEFAULT);

    assertThat(intentFilterData.getSchemes().size()).isEqualTo(3);
    assertThat(intentFilterData.getAuthorities().size()).isEqualTo(3);
    assertThat(intentFilterData.getMimeTypes().size()).isEqualTo(3);
    assertThat(intentFilterData.getPaths().size()).isEqualTo(1);
    assertThat(intentFilterData.getPathPatterns().size()).isEqualTo(1);
    assertThat(intentFilterData.getPathPrefixes().size()).isEqualTo(1);


    assertThat(intentFilterData.getSchemes().get(0)).isEqualTo("content");
    assertThat(intentFilterData.getPaths().get(0).toString()).isEqualTo("/testPath");
    assertThat(intentFilterData.getMimeTypes().get(0)).isEqualTo("video/mpeg");
    assertThat(intentFilterData.getAuthorities().get(0).getHost()).isEqualTo("testhost1.com");
    assertThat(intentFilterData.getAuthorities().get(0).getPort()).isEqualTo("1");

    assertThat(intentFilterData.getSchemes().get(1)).isEqualTo("http");
    assertThat(intentFilterData.getPathPrefixes().get(0).toString()).isEqualTo("/testPrefix");
    assertThat(intentFilterData.getMimeTypes().get(1)).isEqualTo("image/jpeg");
    assertThat(intentFilterData.getAuthorities().get(1).getHost()).isEqualTo("testhost2.com");
    assertThat(intentFilterData.getAuthorities().get(1).getPort()).isEqualTo("2");

    assertThat(intentFilterData.getSchemes().get(2)).isEqualTo("https");
    assertThat(intentFilterData.getPathPatterns().get(0).toString()).isEqualTo("/.*testPattern");
    assertThat(intentFilterData.getMimeTypes().get(2)).isEqualTo("image/*");
    assertThat(intentFilterData.getAuthorities().get(2).getHost()).isEqualTo("testhost3.com");
    assertThat(intentFilterData.getAuthorities().get(2).getPort()).isEqualTo("3");
  }

  /////////////////////////////

  public AndroidManifest newConfigWith(String usesSdkAttrs) throws IOException {
    File f = temporaryFolder.newFile("whatever.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "          package=\"org.robolectric\">\n" +
            "    <uses-sdk " + usesSdkAttrs + "/>\n" +
            "</manifest>\n");
    return new AndroidManifest(Fs.newFile(f), null, null);
  }

  private List<String> stringify(Collection<ResourcePath> resourcePaths) {
    List<String> resourcePathBases = new ArrayList<>();
    for (ResourcePath resourcePath : resourcePaths) {
      resourcePathBases.add(resourcePath.resourceBase.toString());
    }
    return resourcePathBases;
  }

  @Test
  public void shouldReadFlagsFromAndroidManifest() throws Exception {
    AndroidManifest config = newConfig("TestAndroidManifestWithFlags.xml");
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_ALLOW_BACKUP));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_ALLOW_CLEAR_USER_DATA));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_ALLOW_TASK_REPARENTING));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_DEBUGGABLE));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_HAS_CODE));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_KILL_AFTER_RESTORE));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_PERSISTENT));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_RESIZEABLE_FOR_SCREENS));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_RESTORE_ANY_VERSION));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_SUPPORTS_LARGE_SCREENS));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_SUPPORTS_NORMAL_SCREENS));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_SUPPORTS_SCREEN_DENSITIES));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_SUPPORTS_SMALL_SCREENS));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_TEST_ONLY));
    assertTrue(hasFlag(config.getApplicationFlags(), FLAG_VM_SAFE_MODE));
  }

  private boolean hasFlag(final int flags, final int flag) {
    return (flags & flag) != 0;
  }

  @SuppressWarnings("unused")
  public static class ConfigTestReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    }
  }

  @Test
  public void shouldLoadLibraryManifests() throws Exception {
    AndroidManifest manifest = newConfig("TestAndroidManifest.xml");
    List<FsFile> libraries = new ArrayList<>();
    libraries.add(resourceFile("lib1"));
    manifest.setLibraryDirectories(libraries);

    List<AndroidManifest> libraryManifests = manifest.getLibraryManifests();
    assertEquals(1, libraryManifests.size());
    assertEquals("org.robolectric.lib1", libraryManifests.get(0).getPackageName());
  }
}
