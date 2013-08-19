package me.vpu.periodicreminder;

import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

public class ReminderConfig extends Activity {
	private int mAppWidgetId;
	private ReminderDbAdapter mDb;
	private Context mCtx;
	
	//private static final String TAG = "ReminderConfig";
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCtx = this;
        setContentView(R.layout.reminder_config);
        
        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) {
           // Setup in case of failure
           mAppWidgetId = extras.getInt(
              AppWidgetManager.EXTRA_APPWIDGET_ID,
              AppWidgetManager.INVALID_APPWIDGET_ID);
           Intent cancelResultValue = new Intent();
           cancelResultValue.putExtra(
              AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
           setResult(RESULT_CANCELED, cancelResultValue);
        }
        Button okButton = (Button)findViewById(R.id.config_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Extract info from widgets and create entry
				EditText title = (EditText)findViewById(R.id.config_title_edit);
				DatePicker last = (DatePicker)findViewById(R.id.config_last_edit);
				EditText interval = (EditText)findViewById(R.id.config_interval_edit);
				EditText count = (EditText)findViewById(R.id.config_count_edit);
				Date last_date = new GregorianCalendar(last.getYear(), last.getMonth(),
										  last.getDayOfMonth()).getTime();
				//Log.d(TAG, "Date: "+last.getYear()+"-"+last.getMonth()+"-"+last.getDayOfMonth());
		        mDb = new ReminderDbAdapter(mCtx).open();
				mDb.createEntry(mAppWidgetId, title.getText().toString(),
								last_date, Integer.parseInt(interval.getText().toString()),
								Integer.parseInt(count.getText().toString()));
				mDb.close();

				// Draw the widget
				Intent drawWidget = new Intent();
				drawWidget.setClassName(getPackageName(), "me.vpu.periodicreminder.ReminderWidget");
				drawWidget.setAction(ReminderWidget.DRAW_WIDGET);
				drawWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				drawWidget.setData(Uri.withAppendedPath(Uri.parse(
						ReminderWidget.URI_SCHEME + "://widget/id/"), String.valueOf(mAppWidgetId)));
				sendBroadcast(drawWidget);
				
				// Everything a-ok
				Intent okValue = new Intent();
				okValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				setResult(RESULT_OK, okValue);
				finish();
			}
		});
        Button cancelButton = (Button)findViewById(R.id.config_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish(); // Already set up for failure, just finish.
			}
		});
    }
}
