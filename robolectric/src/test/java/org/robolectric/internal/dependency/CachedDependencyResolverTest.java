package org.robolectric.internal.dependency;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import org.robolectric.internal.dependency.CachedDependencyResolver.Cache;
import org.robolectric.internal.dependency.CachedDependencyResolver.CacheNamingStrategy;
import org.robolectric.internal.dependency.CachedDependencyResolver.CacheValidationStrategy;
import org.robolectric.test.TemporaryFolder;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class CachedDependencyResolverTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static final String CACHE_NAME = "someName";
  private DependencyResolver internalResolver = mock(DependencyResolver.class);
  private CacheNamingStrategy cacheNamingStrategy = new CacheNamingStrategy() {
    @Override
    public String getName(String prefix, DependencyJar... dependencies) {
      return CACHE_NAME;
    }
  };
  private CacheValidationStrategy cacheValidationStrategy = new CacheValidationStrategy() {
    @Override
    public boolean isValid(URL url) {
      return true;
    }

    @Override
    public boolean isValid(URL[] urls) {
      return true;
    }
  };

  private URL url;
  private URL[] urls;
  private Cache cache = new CacheStub();
  private DependencyJar[] dependencies = new DependencyJar[]{
      createDependency("group1", "artifact1"),
      createDependency("group2", "artifact2"),
  };
  private DependencyJar dependency = dependencies[0];

  @Before
  public void setUp() throws InitializationError, MalformedURLException {
    urls = new URL[] { new URL("http://localhost") };
    url = new URL("http://localhost");
  }

  @Test
  public void getLocalArtifactUrls_shouldWriteLocalArtifactsUrlsWhenCacheMiss() throws Exception {
    DependencyResolver res = createResolver();

    when(internalResolver.getLocalArtifactUrls(dependencies)).thenReturn(urls);

    URL[] urls = res.getLocalArtifactUrls(dependencies);

    assertArrayEquals(this.urls, urls);
    assertCacheContents(urls);
  }

  @Test
  public void getLocalArtifactUrls_shouldReadLocalArtifactUrlsFromCacheIfExists() throws Exception {
    DependencyResolver res = createResolver();
    cache.write(CACHE_NAME, urls);

    URL[] urls = res.getLocalArtifactUrls(dependencies);

    verify(internalResolver, never()).getLocalArtifactUrls(dependencies);

    assertArrayEquals(this.urls, urls);
  }

  @Test
  public void getLocalArtifactUrls_whenCacheInvalid_shouldFetchDependencyInformation() {
    CacheValidationStrategy failStrategy = mock(CacheValidationStrategy.class);
    when(failStrategy.isValid(any(URL[].class))).thenReturn(false);

    DependencyResolver res = new CachedDependencyResolver(internalResolver, cache, cacheNamingStrategy, failStrategy);
    cache.write(CACHE_NAME, this.urls);

    res.getLocalArtifactUrls(dependencies);

    verify(internalResolver).getLocalArtifactUrls(dependencies);
  }

  @Test
  public void getLocalArtifactUrl_shouldWriteLocalArtifactUrlWhenCacheMiss() throws Exception{
    DependencyResolver res = createResolver();

    when(internalResolver.getLocalArtifactUrl(dependency)).thenReturn(url);

    URL url = res.getLocalArtifactUrl(dependency);

    assertEquals(this.url, url);
    assertCacheContents(url);
  }

  @Test
  public void getLocalArtifactUrl_shouldReadLocalArtifactUrlFromCacheIfExists() throws Exception {
    DependencyResolver res = createResolver();
    cache.write(CACHE_NAME, url);

    URL url = res.getLocalArtifactUrl(dependency);

    verify(internalResolver, never()).getLocalArtifactUrl(dependency);

    assertEquals(this.url, url);
  }

  @Test
  public void getLocalArtifactUrl_whenCacheInvalid_shouldFetchDependencyInformation() {
    CacheValidationStrategy failStrategy = mock(CacheValidationStrategy.class);
    when(failStrategy.isValid(any(URL.class))).thenReturn(false);

    DependencyResolver res = new CachedDependencyResolver(internalResolver, cache, cacheNamingStrategy, failStrategy);
    cache.write(CACHE_NAME, this.url);

    res.getLocalArtifactUrl(dependency);

    verify(internalResolver).getLocalArtifactUrl(dependency);
  }

  private void assertCacheContents(URL[] urls) {
    assertArrayEquals(urls, cache.load(CACHE_NAME, URL[].class));
  }

  private void assertCacheContents(URL url) {
    assertEquals(url, cache.load(CACHE_NAME, URL.class));
  }

  private DependencyResolver createResolver() {
    return new CachedDependencyResolver(internalResolver, cache, cacheNamingStrategy, cacheValidationStrategy);
  }

  private DependencyJar createDependency(final String groupId, final String artifactId) {
    return new DependencyJar(groupId, artifactId, null, "") {

      @Override
      public boolean equals(Object o) {
        if(!(o instanceof DependencyJar)) return false;

        DependencyJar d = (DependencyJar) o;

        return this.getArtifactId().equals(d.getArtifactId()) && this.getGroupId().equals(groupId);
      }
    };
  }

  private static class CacheStub implements CachedDependencyResolver.Cache {
    private Map<String, Serializable> map = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T load(String id, Class<T> type) {
      Serializable o = map.get(id);

      return o != null && o.getClass() == type ? (T) o : null;
    }

    @Override
    public <T extends Serializable> boolean write(String id, T object) {
      map.put(id, object);
      return true;
    }
  }
}
