package me.vpu.periodicreminder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import me.vpu.periodicreminder.ReminderDbAdapter.ReminderEntry;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class ReminderWidget extends AppWidgetProvider {
	public final static String URI_SCHEME = "REMWIDGETSCHEME";
	private final static String TAG = "ReminderWidget";
	static HashMap<Integer, Integer> counts;
	static ReminderDbAdapter mDb = null;
	private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd.MM.");
	
	//Actions
	private final static String REQUEST_CONFIRMATION = "me.vpu.periodicreminder.request_confirmation_action";
	private final static String DONE = "me.vpu.periodicreminder.done_action";
	public final static String DRAW_WIDGET = "me.vpu.periodicreminder.draw_widget_action";

	private static final String CONFIRM_RESPONSE_KEY = "me.vpu.periodicreminder.confirm_response";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		for(int id : appWidgetIds) {
			//Log.d(TAG, "onUpdate("+id+")");
			drawWidget(context, id);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for(int id : appWidgetIds) {
			//Log.d(TAG, "Deleting entry for id "+id);
			mDb.deleteEntry(id);
		}
	}

	@Override
	public void onDisabled(Context context) {
		//Log.d(TAG, "Dropping reminder table.");
		super.onDisabled(context);
		mDb.emptyTable();
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (mDb == null)
			mDb = new ReminderDbAdapter(context).open();
		Log.d(TAG, "onReceive()");
		//Log.d("TAG", "DADA: "+intent.getDataString());
	    final String action = intent.getAction();
	    Bundle extras = intent.getExtras();
	    
	    if (action.equals(REQUEST_CONFIRMATION)) {
	    	int id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
	    	//Log.d(TAG, "Widget "+id+" requesting confirmation");
	    	drawConfirm(context, id);
	    	return;
	    } else if (action.equals(DONE)) {
	    	int id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
	    	//Log.d(TAG, "App widget "+id+" done: "+extras.get(CONFIRM_RESPONSE_KEY));
	    	if(extras.getBoolean(CONFIRM_RESPONSE_KEY, false)) {
	    		mDb.updateEntry(id);
	    	}
    		drawWidget(context, id);
	    	return;	    	
	    } else if (action.equals(DRAW_WIDGET)) {
	    	//Log.d(TAG, "Received DRAW_WIDGET action");
	    	drawWidget(context, extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID));
	    	return;
	    }
	    
	    // Fix a glitch in Android 1.5
	    if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
	        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
	                AppWidgetManager.INVALID_APPWIDGET_ID);
	        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
	            this.onDeleted(context, new int[] { appWidgetId });
	        }
	    } else {
	        super.onReceive(context, intent);
	    }

	}
	
	private PendingIntent makeIntentForSelf(Context ctx, int appWidgetId, 
											String action, Bundle extras) {
		Intent i = new Intent();
		i.setClassName(ctx.getPackageName(), "me.vpu.periodicreminder.ReminderWidget");
		if(action != null)
			i.setAction(action);
		i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		if(extras != null)
			i.putExtras(extras);
		i.setData(Uri.parse(ReminderWidget.URI_SCHEME + "://widget/id/"+appWidgetId+"/action/"+action+"/extras/"+extras));
		PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		return pi;
	}
	
	/** Draws the main widget layout. */
	private void drawWidget(Context ctx, int appWidgetId) {
		ReminderEntry e = mDb.fetchEntry(appWidgetId);
		if (e == null) {
			RemoteViews errorViews = new RemoteViews(ctx.getPackageName(), R.layout.main_alert);
			errorViews.setTextViewText(R.id.title, "DB problem :(");
			AppWidgetManager.getInstance(ctx).updateAppWidget(appWidgetId, errorViews);
			return;
		}
		Date next_time = new Date(e.last.getTime()+e.interval*86400000l);
		RemoteViews newViews;
		if(next_time.after(new Date()))
			newViews = new RemoteViews(ctx.getPackageName(), R.layout.main);
		else
			newViews = new RemoteViews(ctx.getPackageName(), R.layout.main_alert);
		
		//newViews.setTextColor(R.id.title, android.R.drawable.screen_background_dark);
		newViews.setTextViewText(R.id.title, e.title);
		
		String info = SHORT_DATE_FORMAT.format(e.last) + " -> "
						+ SHORT_DATE_FORMAT.format(next_time)
						+ ", N=" + e.count;
		newViews.setTextViewText(R.id.info, info);
		
		PendingIntent pi = makeIntentForSelf(ctx, appWidgetId, REQUEST_CONFIRMATION, null);
		newViews.setOnClickPendingIntent(R.id.done_button, pi);
		
		AppWidgetManager.getInstance(ctx).updateAppWidget(appWidgetId, newViews);
	}
	
	/** Draws the confirmation layout. */
	private void drawConfirm(Context ctx, int appWidgetId) {
		RemoteViews newViews = new RemoteViews(ctx.getPackageName(), R.layout.confirm);
		
		Bundle ok = new Bundle();
		ok.putBoolean(CONFIRM_RESPONSE_KEY, true);
		PendingIntent okPi = makeIntentForSelf(ctx, appWidgetId, DONE, ok);
		newViews.setOnClickPendingIntent(R.id.confirm_ok, okPi);

		Bundle cancel = new Bundle();
		cancel.putBoolean(CONFIRM_RESPONSE_KEY, false);
		PendingIntent cancelPi = makeIntentForSelf(ctx, appWidgetId, DONE, cancel);
		newViews.setOnClickPendingIntent(R.id.confirm_cancel, cancelPi);
		
		AppWidgetManager.getInstance(ctx).updateAppWidget(appWidgetId, newViews);
	}
}
