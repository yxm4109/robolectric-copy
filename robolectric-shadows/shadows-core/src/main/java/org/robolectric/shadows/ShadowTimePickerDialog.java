package org.robolectric.shadows;

import android.app.TimePickerDialog;
import android.content.Context;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.Shadow;

import static org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow for {@link android.app.TimePickerDialog}.
 */
@Implements(value = TimePickerDialog.class, inheritImplementationMethods = true)
public class ShadowTimePickerDialog extends ShadowAlertDialog {
  @RealObject
  protected TimePickerDialog realTimePickerDialog;
  private int hourOfDay;
  private int minute;

  public void __constructor__(Context context, int theme, TimePickerDialog.OnTimeSetListener callBack,
                              int hourOfDay, int minute, boolean is24HourView) {
    this.hourOfDay = hourOfDay;
    this.minute = minute;

    Shadow.invokeConstructor(TimePickerDialog.class, realTimePickerDialog,
        ClassParameter.from(Context.class, context),
        ClassParameter.from(int.class, theme),
        ClassParameter.from(TimePickerDialog.OnTimeSetListener.class, callBack),
        ClassParameter.from(int.class, hourOfDay),
        ClassParameter.from(int.class, minute),
        ClassParameter.from(boolean.class, is24HourView));
  }

  public int getHourOfDay() {
    return hourOfDay;
  }

  public int getMinute() {
    return minute;
  }
}
