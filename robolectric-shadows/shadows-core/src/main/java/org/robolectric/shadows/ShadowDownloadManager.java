package org.robolectric.shadows;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;
import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.fakes.BaseCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shadow for {@link android.app.DownloadManager}.
 */
@Implements(DownloadManager.class)
public class ShadowDownloadManager {

  private long queueCounter = -1; // First request starts at 0 just like in the real DownloadManager
  private Map<Long, DownloadManager.Request> requestMap = new HashMap<>();

  @Implementation
  public long enqueue(DownloadManager.Request request) {
    queueCounter++;
    requestMap.put(queueCounter, request);
    return queueCounter;
  }

  @Implementation
  public int remove(long... ids) {
    int removeCount = 0;
    for (long id : ids) {
      if (requestMap.remove(id) != null) {
        removeCount++;
      }
    }
    return removeCount;
  }

  @Implementation
  public Cursor query(DownloadManager.Query query) {
    ResultCursor result = new ResultCursor();
    ShadowQuery shadow = Shadows.shadowOf(query);
    long[] ids = shadow.getIds();

    if (ids != null) {
      for (long id : ids) {
        DownloadManager.Request request = requestMap.get(id);
        if (request != null) {
          result.requests.add(request);
        }
      }
    }
    return result;
  }

  public DownloadManager.Request getRequest(long id) {
    return requestMap.get(id);
  }

  public int getRequestCount() {
    return requestMap.size();
  }

  @Implements(DownloadManager.Request.class)
  public static class ShadowRequest {
    @RealObject DownloadManager.Request realObject;

    private int status;

    public int getStatus() {
      return this.status;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public Uri getUri() {
      return getFieldReflectively("mUri", realObject, DownloadManager.Request.class);
    }

    public Uri getDestination() {
      return getFieldReflectively("mDestinationUri", realObject, DownloadManager.Request.class);
    }

    public CharSequence getTitle() {
      return getFieldReflectively("mTitle", realObject, DownloadManager.Request.class);
    }

    public CharSequence getDescription() {
      return getFieldReflectively("mDescription", realObject, DownloadManager.Request.class);
    }

    public CharSequence getMimeType() {
      return getFieldReflectively("mMimeType", realObject, DownloadManager.Request.class);
    }

    public int getNotificationVisibility() {
      return getFieldReflectively("mNotificationVisibility", realObject, DownloadManager.Request.class);
    }

    public int getAllowedNetworkTypes() {
      return getFieldReflectively("mAllowedNetworkTypes", realObject, DownloadManager.Request.class);
    }

    public boolean getAllowedOverRoaming() {
      return getFieldReflectively("mRoamingAllowed", realObject, DownloadManager.Request.class);
    }

    public boolean getAllowedOverMetered() {
      return getFieldReflectively("mMeteredAllowed", realObject, DownloadManager.Request.class);
    }

    public boolean getVisibleInDownloadsUi() {
      return getFieldReflectively("mIsVisibleInDownloadsUi", realObject, DownloadManager.Request.class);
    }

    public List<Pair<String, String>> getRequestHeaders() {
      return getFieldReflectively("mRequestHeaders", realObject, DownloadManager.Request.class);
    }
  }

  @Implements(DownloadManager.Query.class)
  public static class ShadowQuery {
    @RealObject DownloadManager.Query realObject;

    public long[] getIds() {
      return getFieldReflectively("mIds", realObject, DownloadManager.Query.class);
    }
  }

  private class ResultCursor extends BaseCursor {
    private static final int COLUMN_INDEX_LOCAL_FILENAME = 0;
    private static final int COLUMN_INDEX_DESCRIPTION = 1;
    private static final int COLUMN_INDEX_REASON = 2;
    private static final int COLUMN_INDEX_STATUS = 3;
    private static final int COLUMN_INDEX_URI = 4;
    private static final int COLUMN_INDEX_LOCAL_URI = 5;

    public List<DownloadManager.Request> requests = new ArrayList<>();
    private int positionIndex = -1;
    private boolean closed;

    @Override
    public int getCount() {
      checkClosed();
      return requests.size();
    }

    @Override
    public boolean moveToFirst() {
      checkClosed();
      positionIndex = 0;
      return !requests.isEmpty();
    }

    @Override
    public boolean moveToNext() {
      checkClosed();
      positionIndex += 1;
      return positionIndex < requests.size();
    }

    @Override
    public int getColumnIndex(String columnName) {
      checkClosed();

      if (DownloadManager.COLUMN_LOCAL_FILENAME.equals(columnName)) {
        return COLUMN_INDEX_LOCAL_FILENAME;

      } else if (DownloadManager.COLUMN_DESCRIPTION.equals(columnName)) {
        return COLUMN_INDEX_DESCRIPTION;

      } else if (DownloadManager.COLUMN_REASON.equals(columnName)) {
        return COLUMN_INDEX_REASON;

      } else if (DownloadManager.COLUMN_STATUS.equals(columnName)) {
        return COLUMN_INDEX_STATUS;

      } else if (DownloadManager.COLUMN_URI.equals(columnName)) {
        return COLUMN_INDEX_URI;

      } else if (DownloadManager.COLUMN_LOCAL_URI.equals(columnName)) {
        return COLUMN_INDEX_LOCAL_URI;
      }

      return -1;
    }

    @Override
    public void close() {
      this.closed = true;
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    @Override
    public String getString(int columnIndex) {
      checkClosed();
      ShadowRequest request = Shadows.shadowOf(requests.get(positionIndex));
      switch (columnIndex) {
        case COLUMN_INDEX_LOCAL_FILENAME:
          return "local file name not implemented";

        case COLUMN_INDEX_REASON:
          return "reason not implemented";

        case COLUMN_INDEX_DESCRIPTION:
          return request.getDescription().toString();

        case COLUMN_INDEX_URI:
          return request.getUri().toString();

        case COLUMN_INDEX_LOCAL_URI:
          return request.getDestination().toString();
      }

      return "Unknown ColumnIndex " + columnIndex;
    }

    @Override
    public int getInt(int columnIndex) {
      checkClosed();
      ShadowRequest request = Shadows.shadowOf(requests.get(positionIndex));
      if (columnIndex == COLUMN_INDEX_STATUS) {
        return request.getStatus();
      }
      return 0;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("Cursor is already closed.");
      }
    }
  }

  private static <T> T getFieldReflectively(String fieldName, Object object, Class aClass) {
    return ReflectionHelpers.getField(object, fieldName);
  }
}
