package me.vpu.periodicreminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ReminderDbAdapter {
	public static final String KEY_WIDGET_ID = "widget_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_LAST = "last";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_COUNT = "count";
    public static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String TAG = "ReminderDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "periodic_reminders";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_CREATE =
        "create table if not exists "+DATABASE_TABLE+" ("
        		+KEY_WIDGET_ID+" integer primary key, "
                +KEY_TITLE+" text not null, "
                +KEY_LAST+" date not null,"
                +KEY_INTERVAL+" integer not null,"
                +KEY_COUNT+" integer not null);";
    
    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ReminderDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this
     * @throws SQLException if the database could be neither opened or created
     */
    public ReminderDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
     * Close the database connection, releasing resourcer.
     */
    public void close() {
        mDbHelper.close();
    }

    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createEntry(int widgetId, String title, Date last,
    						int interval, int count) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_WIDGET_ID, widgetId);
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_LAST, DATEFORMAT.format(last));
        initialValues.put(KEY_INTERVAL, interval);
        initialValues.put(KEY_COUNT, count);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
// Unnecessary?
//    public boolean entryExists(int widgetId) {
//        Cursor c = mDb.query(true, DATABASE_TABLE, new String[] {KEY_WIDGET_ID},
//      		   KEY_WIDGET_ID + "=" + widgetId, null, null, null, null, null);
//        boolean result = c != null && !c.moveToFirst();
//        c.close();
//        return result;
//    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteEntry(int widgetId) {
        return mDb.delete(DATABASE_TABLE, KEY_WIDGET_ID + "=" + widgetId, null) > 0;
    }
    
    /**
     * Delete all rows from the table.
     */
    public void emptyTable() {
    	mDb.execSQL("DELETE FROM "+DATABASE_TABLE);
    }

    public static class ReminderEntry {
    	public int widgetId;
    	public String title;
    	public Date last;
    	public int interval;
    	public int count;
    }
    
    /**
     * Return a ReminderEntry that matches the given rowId or null.
     * 
     * @param widgetId app widget id of entry to retrieve
     * @return ReminderEntry or null on error
     * @throws SQLException if note could not be found/retrieved
     */
    public ReminderEntry fetchEntry(int widgetId) throws SQLException {
//    	Log.d(TAG, "fetchEntry("+widgetId+")");
        Cursor c = mDb.query(true, DATABASE_TABLE, 
        						   new String[] {KEY_WIDGET_ID, KEY_TITLE, KEY_LAST,
        										 KEY_INTERVAL, KEY_COUNT},
                         		   KEY_WIDGET_ID + "=" + widgetId, null,
                                   null, null, null, null);
        if (c == null || !c.moveToFirst()) {
//        	Log.d(TAG, "fetchEntry("+widgetId+"): query failed, c="+c.toString());
        	if(c != null)
        		c.close();
        	return null;
        }
        ReminderEntry ret = new ReminderEntry();
        ret.widgetId = widgetId;
        ret.title = c.getString(c.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE));
        String lastString = c.getString(c.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LAST));
        try {
        	ret.last = DATEFORMAT.parse(lastString);
        } catch(ParseException e) {
        	c.close();
        	//Log.d(TAG, "fetchEntry ("+widgetId+"): Could not parse date \""+lastString+"\"");
        	return null;
        }
        ret.interval = c.getInt(c.getColumnIndexOrThrow(ReminderDbAdapter.KEY_INTERVAL));
        ret.count = c.getInt(c.getColumnIndexOrThrow(ReminderDbAdapter.KEY_COUNT));
        c.close();
        return ret;
    }

    /**
     * Update the entry: set last to now and increment count.
     * 
     * @param widgetId id of note to update
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateEntry(int widgetId) {
    	ReminderEntry e = fetchEntry(widgetId);
        ContentValues args = new ContentValues();
        args.put(KEY_LAST, DATEFORMAT.format(new Date()));
        args.put(KEY_COUNT, e.count+1);

        return mDb.update(DATABASE_TABLE, args, KEY_WIDGET_ID + "=" + widgetId, null) > 0;
    }
}
