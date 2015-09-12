package org.robolectric.shadows;

import android.app.WallpaperManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.MultiApiWithDefaults.class)
public class ShadowWallpaperManagerTest {

  @Test
  public void getInstance_shouldCreateInstance() {
    WallpaperManager manager = WallpaperManager.getInstance(RuntimeEnvironment.application);
    assertThat(manager).isNotNull();
  }

  @Test
  public void sendWallpaperCommand_shouldNotThrowException() {
    WallpaperManager manager = WallpaperManager.getInstance(RuntimeEnvironment.application);
    manager.sendWallpaperCommand(null, null, 0, 0, 0, null);
  }
}
