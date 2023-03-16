package org.dslul.openboard.translator.pro.fcm.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class DB {

    private static DB instance;
    private final SharedPreferences preferences;
    private String DEFAULT_APP_IMAGEDATA_DIRECTORY;
    private String lastImagePath = "";

    public DB(final Context appContext) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public static DB getInstance(final Context context) {
        if (DB.instance != null) return DB.instance;
        else return DB.instance = new DB(context);
    }

    /**
     * Check if external storage is writable or not
     *
     * @return true if writable, false otherwise
     */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Check if external storage is readable or not
     *
     * @return true if readable, false otherwise
     */
    public static boolean isExternalStorageReadable() {
        final String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Decodes the Bitmap from 'path' and returns it
     *
     * @param path image path
     * @return the Bitmap from 'path'
     */
    public Bitmap getImage(final String path) {
        Bitmap bitmapFromPath = null;
        try {
            bitmapFromPath = BitmapFactory.decodeFile(path);

        } catch (final Exception e) {

            e.printStackTrace();
        }

        return bitmapFromPath;
    }

    /**
     * Returns the String path of the last saved image
     *
     * @return string path of the last saved image
     */
    public String getSavedImagePath() {
        return this.lastImagePath;
    }

    /**
     * Saves 'theBitmap' into folder 'theFolder' with the name 'theImageName'
     *
     * @param theFolder    the folder path dir you want to save it to e.g "DropBox/WorkImages"
     * @param theImageName the name you want to assign to the image file e.g "MeAtLunch.png"
     * @param theBitmap    the image you want to save as a Bitmap
     * @return returns the full path(file system address) of the saved image
     */
    public String putImage(final String theFolder, final String theImageName, final Bitmap theBitmap) {
        if (theFolder == null || theImageName == null || theBitmap == null)
            return null;

        DEFAULT_APP_IMAGEDATA_DIRECTORY = theFolder;
        final String mFullPath = this.setupFullPath(theImageName);

        if (!mFullPath.equals("")) {
            this.lastImagePath = mFullPath;
            this.saveBitmap(mFullPath, theBitmap);
        }

        return mFullPath;
    }

    /**
     * Saves 'theBitmap' into 'fullPath'
     *
     * @param fullPath  full path of the image file e.g. "Images/MeAtLunch.png"
     * @param theBitmap the image you want to save as a Bitmap
     * @return true if image was saved, false otherwise
     */
    public boolean putImageWithFullPath(final String fullPath, final Bitmap theBitmap) {
        return !(fullPath == null || theBitmap == null) && this.saveBitmap(fullPath, theBitmap);
    }

    // Getters

    /**
     * Creates the path for the image with name 'imageName' in DEFAULT_APP.. directory
     *
     * @param imageName name of the image
     * @return the full path of the image. If it failed to create directory, return empty string
     */
    private String setupFullPath(final String imageName) {
        final File mFolder = new File(Environment.getExternalStorageDirectory(), this.DEFAULT_APP_IMAGEDATA_DIRECTORY);

        if (DB.isExternalStorageReadable() && DB.isExternalStorageWritable() && !mFolder.exists()) {
            if (!mFolder.mkdirs()) {
                return "";
            }
        }

        return mFolder.getPath() + '/' + imageName;
    }

    /**
     * Saves the Bitmap as a PNG file at path 'fullPath'
     *
     * @param fullPath path of the image file
     * @param bitmap   the image as a Bitmap
     * @return true if it successfully saved, false otherwise
     */
    private boolean saveBitmap(final String fullPath, final Bitmap bitmap) {
        if (fullPath == null || bitmap == null)
            return false;

        boolean fileCreated = false;
        boolean bitmapCompressed = false;
        boolean streamClosed = false;

        final File imageFile = new File(fullPath);

        if (imageFile.exists())
            if (!imageFile.delete())
                return false;

        try {
            fileCreated = imageFile.createNewFile();

        } catch (final IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(imageFile);
            bitmapCompressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

        } catch (final Exception e) {
            e.printStackTrace();
            bitmapCompressed = false;

        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                    streamClosed = true;

                } catch (final IOException e) {
                    e.printStackTrace();
                    streamClosed = false;
                }
            }
        }

        return (fileCreated && bitmapCompressed && streamClosed);
    }

    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key SharedPreferences key
     * @return int value at 'key' or 'defaultValue' if key not found
     */
    public int getInt(final String key) {
        return this.preferences.getInt(key, 0);
    }

    /**
     * Get parsed ArrayList of Integers from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of Integers
     */
    public ArrayList<Integer> getListInt(final String key) {
        final String[] myList = TextUtils.split(this.preferences.getString(key, ""), "‚‗‚");
        final ArrayList<String> arrayToList = new ArrayList<>(Arrays.asList(myList));
        final ArrayList<Integer> newList = new ArrayList<>();

        for (final String item : arrayToList)
            newList.add(Integer.parseInt(item));

        return newList;
    }

    /**
     * Get long value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue long value returned if key was not found
     * @return long value at 'key' or 'defaultValue' if key not found
     */
    public long getLong(final String key, final long defaultValue) {
        return this.preferences.getLong(key, defaultValue);
    }

    /**
     * Get float value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key SharedPreferences key
     * @return float value at 'key' or 'defaultValue' if key not found
     */
    public float getFloat(final String key) {
        return this.preferences.getFloat(key, 0);
    }

    /**
     * Get double value from SharedPreferences at 'key'. If exception thrown, return 'defaultValue'
     *
     * @param key          SharedPreferences key
     * @param defaultValue double value returned if exception is thrown
     * @return double value at 'key' or 'defaultValue' if exception is thrown
     */
    public double getDouble(final String key, final double defaultValue) {
        final String number = this.getString(key);

        try {
            return Double.parseDouble(number);

        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get parsed ArrayList of Double from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of Double
     */
    public ArrayList<Double> getListDouble(final String key) {
        final String[] myList = TextUtils.split(this.preferences.getString(key, ""), "‚‗‚");
        final ArrayList<String> arrayToList = new ArrayList<String>(Arrays.asList(myList));
        final ArrayList<Double> newList = new ArrayList<Double>();

        for (final String item : arrayToList)
            newList.add(Double.parseDouble(item));

        return newList;
    }

    /**
     * Get String value from SharedPreferences at 'key'. If key not found, return ""
     *
     * @param key SharedPreferences key
     * @return String value at 'key' or "" (empty String) if key not found
     */
    public String getString(final String key) {
        return this.preferences.getString(key, "");
    }


//    public ArrayList<Object> getListObject(String key, Class<?> mClass){
//    	Gson gson = new Gson();
//
//    	ArrayList<String> objStrings = getListString(key);
//    	ArrayList<Object> objects =  new ArrayList<Object>();
//
//    	for(String jObjString : objStrings){
//    		Object value  = gson.fromJson(jObjString,  mClass);
//    		objects.add(value);
//    	}
//    	return objects;
//    }

    /**
     * Get parsed ArrayList of String from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return ArrayList of String
     */
    public ArrayList<String> getListString(final String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(this.preferences.getString(key, ""), "‚‗‚")));
    }

    /**
     * Get boolean value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     *
     * @param key SharedPreferences key
     * @return boolean value at 'key' or 'defaultValue' if key not found
     */
    public boolean getBoolean(final String key) {
        return this.preferences.getBoolean(key, false);
    }


    // Put methods

    public <T> ArrayList<T> getListObject(final String key, final Class<T> mClass) {
        final Gson gson = new Gson();

        final ArrayList<String> objStrings = this.getListString(key);
        final ArrayList<T> objects = new ArrayList<>();

        for (final String jObjString : objStrings) {
            final T value = gson.fromJson(jObjString, mClass);
            objects.add(value);
        }
        return objects;
    }

    public Object getObject(final String key, final Class<?> classOfT) {

        final String json = this.getString(key);
        final Object value = new Gson().fromJson(json, classOfT);
        if (value == null)
            throw new NullPointerException();
        return value;
    }

    /**
     * Put int value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value int value to be added
     */
    public void putInt(final String key, final int value) {
        this.checkForNullKey(key);
        this.preferences.edit().putInt(key, value).apply();
    }

    /**
     * Put ArrayList of Integer into SharedPreferences with 'key' and save
     *
     * @param key     SharedPreferences key
     * @param intList ArrayList of Integer to be added
     */
    public void putListInt(final String key, final ArrayList<Integer> intList) {
        this.checkForNullKey(key);
        final Integer[] myIntList = intList.toArray(new Integer[intList.size()]);
        this.preferences.edit().putString(key, TextUtils.join("‚‗‚", myIntList)).apply();
    }

    /**
     * Put long value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value long value to be added
     */
    public void putLong(final String key, final long value) {
        this.checkForNullKey(key);
        this.preferences.edit().putLong(key, value).apply();
    }

    /**
     * Put float value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value float value to be added
     */
    public void putFloat(final String key, final float value) {
        this.checkForNullKey(key);
        this.preferences.edit().putFloat(key, value).apply();
    }

    /**
     * Put double value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value double value to be added
     */
    public void putDouble(final String key, final double value) {
        this.checkForNullKey(key);
        this.putString(key, String.valueOf(value));
    }

    /**
     * Put ArrayList of Double into SharedPreferences with 'key' and save
     *
     * @param key        SharedPreferences key
     * @param doubleList ArrayList of Double to be added
     */
    public void putListDouble(final String key, final ArrayList<Double> doubleList) {
        this.checkForNullKey(key);
        final Double[] myDoubleList = doubleList.toArray(new Double[doubleList.size()]);
        this.preferences.edit().putString(key, TextUtils.join("‚‗‚", myDoubleList)).apply();
    }

    /**
     * Put String value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value String value to be added
     */
    public void putString(final String key, final String value) {
        this.checkForNullKey(key);
        this.checkForNullValue(value);
        this.preferences.edit().putString(key, value).apply();
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     *
     * @param key        SharedPreferences key
     * @param stringList ArrayList of String to be added
     */
    public void putListString(final String key, final ArrayList<String> stringList) {
        this.checkForNullKey(key);
        final String[] myStringList = stringList.toArray(new String[stringList.size()]);
        this.preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

//    public void putListObject(String key, ArrayList<Object> objArray) {
//        checkForNullKey(key);
//        Gson gson = new Gson();
//        ArrayList<String> objStrings = new ArrayList<String>();
//        for (Object obj : objArray) {
//            objStrings.add(gson.toJson(obj));
//        }
//        putListString(key, objStrings);
//    }

    /**
     * Put boolean value into SharedPreferences with 'key' and save
     *
     * @param key   SharedPreferences key
     * @param value boolean value to be added
     */
    public void putBoolean(final String key, final boolean value) {
        this.checkForNullKey(key);
        this.preferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Put ObJect any type into SharedPrefrences with 'key' and save
     *
     * @param key SharedPreferences key
     * @param obj is the Object you want to put
     */
    public void putObject(final String key, final Object obj) {
        this.checkForNullKey(key);
        final Gson gson = new Gson();
        this.putString(key, gson.toJson(obj));
    }

    public <T> void putListObject(final String key, final ArrayList<T> objArray) {
        this.checkForNullKey(key);
        final Gson gson = new Gson();
        final ArrayList<String> objStrings = new ArrayList<String>();
        for (final T obj : objArray) {
            objStrings.add(gson.toJson(obj));
        }
        this.putListString(key, objStrings);
    }

    /**
     * Remove SharedPreferences item with 'key'
     *
     * @param key SharedPreferences key
     */
    public void remove(final String key) {
        this.preferences.edit().remove(key).apply();
    }

    /**
     * Delete image file at 'path'
     *
     * @param path path of image file
     * @return true if it successfully deleted, false otherwise
     */
    public boolean deleteImage(final String path) {
        return new File(path).delete();
    }

    /**
     * Clear SharedPreferences (remove everything)
     */
    public void clear() {
        this.preferences.edit().clear().apply();
    }

    /**
     * Retrieve all values from SharedPreferences. Do not modify collection return by method
     *
     * @return a Map representing a list of key/value pairs from SharedPreferences
     */
    public Map<String, ?> getAll() {
        return this.preferences.getAll();
    }

    /**
     * Register SharedPreferences change listener
     *
     * @param listener listener object of OnSharedPreferenceChangeListener
     */
    public void registerOnSharedPreferenceChangeListener(
            final SharedPreferences.OnSharedPreferenceChangeListener listener) {

        this.preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Unregister SharedPreferences change listener
     *
     * @param listener listener object of OnSharedPreferenceChangeListener to be unregistered
     */
    public void unregisterOnSharedPreferenceChangeListener(
            final SharedPreferences.OnSharedPreferenceChangeListener listener) {

        this.preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     *
     * @param key pref key
     */
    public void checkForNullKey(final String key) {
        if (key == null) {
            throw new NullPointerException();
        }
    }

    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     *
     * @param value pref key
     */
    public void checkForNullValue(final String value) {
        if (value == null) {
            throw new NullPointerException();
        }
    }
}