//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.library;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.NamedJavaFunction;
import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import android.util.*;
import java.util.*;
import com.ansca.corona.*;
import android.content.Context;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.database.Cursor;
/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/** Lua registry ID to the Lua function to be called when the ad request finishes. */
	private int fListener;

	/** This corresponds to the event name, e.g. [Lua] event.name */
	private static final String EVENT_NAME = "pluginlibraryevent";


	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	public LuaLoader() {
		// Initialize member variables.
		fListener = CoronaLua.REFNIL;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called everytime a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new GetAllContactsWrapper(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.
	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		// Remove the Lua listener reference.
		CoronaLua.deleteRef( runtime.getLuaState(), fListener );
		fListener = CoronaLua.REFNIL;
	}

	private java.util.Hashtable<Integer, java.util.Hashtable<String, Object>> getAllContactsHashtable(Context ctx){
	  ContentResolver cr = ctx.getContentResolver();
      Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
              null, null, null, null);
      
	  java.util.Hashtable<Integer, java.util.Hashtable<String, Object>> contactAddress = new java.util.Hashtable<Integer, java.util.Hashtable<String, Object>>();
      if (cur.getCount() > 0) {
    	  int idx = 0;
          while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                      cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                   Cursor pCur = cr.query(
                             ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                             null,
                             ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                             new String[]{id}, null);
         	      java.util.Hashtable<String, Object> contact= new java.util.Hashtable<String, Object>();
        	      java.util.Hashtable<Integer, java.util.Hashtable<String, String>> phones = new java.util.Hashtable<Integer, java.util.Hashtable<String, String>>();
        	      int idxPhones = 0;
        	      while (pCur.moveToNext()) {
                      String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                      
                      int columnIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                      String phoneLabel = pCur.getString(columnIndex);
                      int labelType = pCur.getInt(columnIndex);
                      if(labelType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                      {
                    	  phoneLabel = pCur.getString(columnIndex);
                      }
                      else
                      {
                          CharSequence seq = ContactsContract.CommonDataKinds.Phone.getTypeLabel(ctx.getResources(), labelType, "Mobile");
                          phoneLabel = seq.toString();
                      }
                      Log.d("Address", "Name: " + name + ", Phone No: " + phoneNo);
                      java.util.Hashtable<String, String> phone = new java.util.Hashtable<String, String>();
            	      phone.put("label", "" + phoneLabel);
            	      phone.put("phonenumber", phoneNo);
            	      phones.put(idxPhones++, phone);
                  }
                  contact.put("name", name);
         	      contact.put("phones", phones);
                  contactAddress.put(idx++, contact);
                  pCur.close();
              }
          }
      }
      return contactAddress;
  }
	/**
	 * The following Lua function has been called:  library.init( listener )
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param L Reference to the Lua state that the Lua function was called from.
	 * @return Returns the number of values to be returned by the library.init() function.
	 */
	public int getAllContacts(LuaState luaState) {
		Log.d("---Address---", "Init");
		java.util.Hashtable<Integer, java.util.Hashtable<String, Object>> allContacts = getAllContactsHashtable(com.ansca.corona.CoronaEnvironment.getApplicationContext());
		// Create a new Lua table within the Lua state to copy the Java hashtable's entries to.
		// Creating a Lua table in this manner automatically pushes it on the Lua stack.
		// For best performance, you should pre-allocate the Lua table like below if the number of entries is known.
		// The newTable() method's first argument should be set to zero since we are not creating a Lua array.
		// The newTable() method's second argument should be set to the number of entries in the Java dictionary.
		luaState.newTable(0, allContacts.size());
		int luaTableStackIndex = luaState.getTop();
		Log.d("---Address---", "Start first loop");

		// Copy the Java hashtable's entries to the Lua table.
		int idxContacts = 0;
		for (java.util.Map.Entry<Integer, java.util.Hashtable<String, Object>> entry : allContacts.entrySet()) {
			Log.d("---Address---", "Get Contacts , index = " + entry.getKey());
			java.util.Hashtable<String, Object> entryVal = entry.getValue();
			String name = (String)entryVal.get("name");
			Log.d("---Address---", "Get Name = " + name);
			
			java.util.Hashtable<Integer, java.util.Hashtable<String, String>> phones = (java.util.Hashtable<Integer, java.util.Hashtable<String, String>>)entryVal.get("phones");
			Log.d("---Address---", "Get Phones");
			
			luaState.pushInteger(++idxContacts);
			luaState.newTable(0, 2);
			luaState.pushString(name);
			luaState.setField(-2, "name");

			luaState.pushString("phones");
			luaState.newTable(0, phones.size());

			int idxPhones = 0;
			Log.d("---Address---", "Start Second Loop");
			for (java.util.Map.Entry<Integer, java.util.Hashtable<String, String>> entry1 : phones.entrySet())
			{
				java.util.Hashtable<String, String> entryVal1 = entry1.getValue();
				String label = (String)entryVal1.get("label");
				Log.d("---Address---", "Get Label = " + label);
				
				String phonenumber = (String)entryVal1.get("phonenumber");
				Log.d("---Address---", "Get phonenumber = " + phonenumber);
				
				luaState.pushInteger(++idxPhones);
				luaState.newTable(0, 2);
				luaState.pushString(label);
				luaState.setField(-2, "label");
				luaState.pushString(phonenumber);
				luaState.setField(-2, "phonenumber");

				luaState.setTable(-3);
			}
			Log.d("---Address---", "End of second loop");
				
			luaState.setTable(-3);
			luaState.setTable(-3);
			// Insert the above value into the Lua table with the given key.
			// This does the equivalent of "table[key] = value" in Lua.
			// The setField() method automatically pops the value off of the Lua stack that was pushed up above.
			//luaState.setField(-2, "contactlist");
		}
		Log.d("---Address---", "End of first loop");
			
		// Return 1 to indicate that this Lua function returns 1 Lua table.
		return 1;
	}


	/** Implements the library.init() Lua function. */
	private class GetAllContactsWrapper implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "getAllContacts";
		}
		
		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param luaState Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {
			return getAllContacts(L);
		}
	}
}
