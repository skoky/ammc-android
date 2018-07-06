package com.skoky.timing.data

import java.sql.SQLException

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.RuntimeExceptionDao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
class DatabaseHelper(context: Context) : OrmLiteSqliteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // the DAO object we use to access the SimpleData table
    private var myDriverDaoSimple: Dao<MyDriver, Int>? = null
    private var myDriverDao: RuntimeExceptionDao<MyDriver, Int>? = null

    val driverDao: Dao<MyDriver, Int>
        @Throws(SQLException::class)
        get() {
            if (myDriverDaoSimple == null) {
                myDriverDaoSimple = getDao(MyDriver::class.java)
            }
            return this.myDriverDaoSimple!!
        }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
        try {
            Log.i(DatabaseHelper::class.java.name, "onCreate")
            TableUtils.createTableIfNotExists(connectionSource, MyDriver::class.java)
        } catch (e: SQLException) {
            Log.e(DatabaseHelper::class.java.name, "Can't create database", e)
            throw RuntimeException(e)
        }

    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
        try {
            Log.i(DatabaseHelper::class.java.name, "onUpgrade")
            TableUtils.dropTable<MyDriver, Any>(connectionSource, MyDriver::class.java, true)
            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource)
        } catch (e: SQLException) {
            Log.e(DatabaseHelper::class.java.name, "Can't drop databases", e)
            throw RuntimeException(e)
        }

    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    override fun close() {
        super.close()
        myDriverDao = null
    }

    companion object {

        // name of the database file for your application -- change to something appropriate for your app
        private val DATABASE_NAME = "mylaps.db"
        // any time you make changes to your database objects, you may have to increase the database version
        private val DATABASE_VERSION = 1
    }
}
