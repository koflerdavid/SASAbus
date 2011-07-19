/**
 *
 * SelectLineaActivity.java
 * 
 * Created: Jan 16, 2011 11:41:06 AM
 * 
 * Copyright (C) 2011 Paolo Dongilli
 *
 * This file is part of SasaBus.

 * SasaBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SasaBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SasaBus.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package it.sasabz.android.sasabus;

import java.io.File;
import java.util.Locale;

import it.sasabz.android.sasabus.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.AndroidRuntimeException;
import android.util.Log;

public class CheckDatabaseActivity extends ListActivity {

	private final static int DOWNLOAD_SUCCESS_DIALOG = 0;
	private final static int DOWNLOAD_ERROR_DIALOG = 1;
	private final static int MD5_ERROR_DIALOG = 2;
	private final static int NO_NETWORK_CONNECTION = 3;

	public CheckDatabaseActivity() {
	}

	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check if db exists
		Resources res = getResources();
		String appName = res.getString(R.string.app_name);
		String dbVersion = res.getString(R.string.db_version);
		String dbDirName = res.getString(R.string.db_dir);
		String repositoryURL = res.getString(R.string.repository_url);
		String dbFileName = appName + "_" + dbVersion + ".db";
		String dbZIPFileName = dbFileName + ".zip";
		String dbURLName = repositoryURL + dbZIPFileName;
		String md5FileName = dbFileName + ".md5";
		String md5URLName = repositoryURL + md5FileName;

		Log.v("CheckDatabaseActivity", "***** dbURLName: " + dbURLName);
		Log.v("CheckDatabaseActivity", "***** md5URLNAme: " + md5URLName);
		
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			throw new AndroidRuntimeException(getResources().getString(R.string.sd_card_not_mounted));
		}
		File dbDir = new File(Environment.getExternalStorageDirectory(),
				dbDirName);
		// check if dbDir exists; if not create it
		if (!dbDir.exists()) {
			dbDir.mkdirs();
		}

		File dbFile = new File(dbDir, dbFileName);
		File dbZIPFile = new File(dbDir, dbZIPFileName);
		File md5File = new File(dbDir, md5FileName);

		boolean download = false;
		if (dbFile.exists() && md5File.exists()) {
			
			Log.v("CheckDatabaseActivity", "***** MD5: " + MD5Utils.extractMD5(md5File));
			Log.v("CheckDatabaseActivity", "***** calculated MD5: " + MD5Utils.calculateMD5(dbFile));
			
			if (!MD5Utils.checksumOK(dbFile, md5File))
				download = true;
		} else {
			download = true;
		}

		if (download) {
			// verify we have a network connection
			if (haveNetworkConnection()) {
				new FileRetriever(this, dbZIPFile, dbFile, md5File).execute(
						dbURLName, md5URLName);
			} else {
                showDialog(NO_NETWORK_CONNECTION);
			}
		} else {
			// verify files
			if (!MD5Utils.checksumOK(dbFile, md5File)) {
				showDialog(MD5_ERROR_DIALOG);
			} else {
				showDialog(DOWNLOAD_SUCCESS_DIALOG);
			}
		}
	}

	/**
	 * Called when the activity is about to start interacting with the user.
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	private final Dialog createAlertDialog(int msg, String placeholder) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setTitle(R.string.a_given_string);
		builder.setIcon(R.drawable.icon);
		//builder.setMessage(msg);
		builder.setMessage(String.format(getString(msg),placeholder));
		builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {

				startActivity();
			}
		});
		return builder.create();
	}

	private final Dialog createErrorAlertDialog(int msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setTitle(R.string.a_given_string);
		builder.setIcon(R.drawable.icon);
		builder.setMessage(msg);
		builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {
				System.exit(0);
			}
		});
		return builder.create();
	}

	private void startActivity() {
		finish();
		Intent selBacino = new Intent(this, SelectBacinoActivity.class);
		startActivity(selBacino);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case NO_NETWORK_CONNECTION:
			return createErrorAlertDialog(R.string.no_network_connection);
		case DOWNLOAD_SUCCESS_DIALOG:
			return createAlertDialog(R.string.db_ok, getString(R.string.db_version));
		case DOWNLOAD_ERROR_DIALOG:
			return createErrorAlertDialog(R.string.db_download_error);
		case MD5_ERROR_DIALOG:
			return createErrorAlertDialog(R.string.md5_error);
		default:
			return null;
		}
	}
	
	private boolean haveNetworkConnection() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
	}

}
