/**
 *
 * ExternalStorageReadOnlyHelper.java
 * 
 * Created: Jan 16, 2011 5:30:31 PM
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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.util.AndroidRuntimeException;

public abstract class ExternalStorageReadOnlyOpenHelper {
	private SQLiteDatabase database;
	private File dbFile;
	private SQLiteDatabase.CursorFactory factory;

	public ExternalStorageReadOnlyOpenHelper(String dbFileName,
			SQLiteDatabase.CursorFactory factory) {
		this.factory = factory;

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			throw new AndroidRuntimeException(
					"External storage (SD-Card) not mounted");
		}
		File appDbDir = new File(Environment.getExternalStorageDirectory(),
				"Android/data/it.sasabz.android.sasabus/db");
		if (!appDbDir.exists()) {
			appDbDir.mkdirs();
		}
		this.dbFile = new File(appDbDir, dbFileName);
	}

	public boolean databaseFileExists() {
		return dbFile.exists();
	}

	private void open() {
		if (dbFile.exists()) {
			// database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),
			// factory, SQLiteDatabase.OPEN_READONLY);

			// Hack to avoid the following exception:
			// android.database.sqlite.SQLiteException: attempt to
			// write a readonly database

			try {
				database = SQLiteDatabase.openDatabase(
						dbFile.getAbsolutePath(), factory,
						SQLiteDatabase.OPEN_READONLY);
			} catch (SQLiteException e) {
				final String message = e.getMessage();
				if (message == null) {
					throw e;
				}
				if (!message.contains("attempt to write a readonly database")) {
					throw e;
				}
				// We tried to open the database in read-only mode but this
				// failed because of a bug which manifests on Model:LG-P500
				// Release:2.3.3 Sdk:10. The openDatabase method tries to write
				// to the DB it opened readonly. Hoping it needs to do this only
				// once, try to open the db in readwrite mode, close it, then
				// try again readonly.
				database = SQLiteDatabase.openDatabase(
						dbFile.getAbsolutePath(), factory,
						SQLiteDatabase.OPEN_READWRITE);
				database.close();
				database = SQLiteDatabase.openDatabase(
						dbFile.getAbsolutePath(), factory,
						SQLiteDatabase.OPEN_READONLY);
			}
		}
	}

	public synchronized void close() {
		if (database != null) {
			database.close();
			database = null;
		}
	}

	public synchronized SQLiteDatabase getReadableDatabase() {
		return getDatabase();
	}

	private SQLiteDatabase getDatabase() {
		if (database == null) {
			open();
		}
		return database;
	}
}