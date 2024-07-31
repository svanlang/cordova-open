package com.disusered;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.content.ActivityNotFoundException;
import android.os.Build;

import java.io.File;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import android.provider.MediaStore;
import android.os.Environment;
import android.content.ContentValues;
import android.content.ContentResolver;

/*
TODO: https://stackoverflow.com/questions/64966826/android-storing-files-in-downloads

create file in downloadsfolder and then try to open.
fix the path thing in www to make sure it keeps on working on ios
keep the original code also to make sure  it works pre api 33

-> fix also the file and mimetypes on new part
cordova plugin remove cordova-open   
cordova plugin add ../../../GitHub/cordova-open 
./android_development.sh

adb logcat | grep -F 'cordova.open'
*/


/**
 * This class starts an activity for an intent to view files
 */
public class Open extends CordovaPlugin {

    public static final String OPEN_ACTION = "open";
	private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals(OPEN_ACTION)) {
            String path = args.getString(0);
            
            if(Build.VERSION.SDK_INT >= 29) //Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
			{
                this.saveFileToDownloadsAndOpen(path, callbackContext);
            }
            else
            {
                this.chooseIntent(path, callbackContext);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @param path
     * @return
     */
    private static String getMimeType(String path) {
        String mimeType = null;

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }

        System.out.println("Mime type: " + mimeType);

        return mimeType;
    }

    /**
     * Creates an intent for the data of mime type
     *
     * @param path
     * @param callbackContext
     */
    private void chooseIntent(String path, CallbackContext callbackContext) {
         Log.d("cordova.open", "PRE API 29" );
        if (path != null && path.length() > 0) {
            try {
                Uri uri = Uri.parse(path);
                String mime = getMimeType(path);
                Intent fileIntent = new Intent(Intent.ACTION_VIEW);

                // see http://stackoverflow.com/questions/25592206/how-to-get-your-context-in-your-phonegap-plugin

                Context context = cordova.getActivity().getApplicationContext();
                File imageFile = new File(uri.getPath());
                Uri photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".cdv.core.file.provider", imageFile);
                fileIntent.setDataAndTypeAndNormalize(photoURI, mime);
                // see http://stackoverflow.com/questions/39450748/intent-shows-a-blank-image
                fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                cordova.getActivity().startActivity(fileIntent);

                callbackContext.success();
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                callbackContext.error(1);
            }
        } else {
            callbackContext.error(2);
        }
    }

    private void saveFileToDownloadsAndOpen( String path, CallbackContext callbackContext )
    {
       if (path != null && path.length() > 0)
       {
            Log.d("cordova.open", "SDK >= 29");
            Context context = this.cordova.getActivity();
            ContentResolver contentResolver = context.getContentResolver();
            Log.d("cordova.open", path );

            Uri fileUri = Uri.parse(path);

            File file = new File(fileUri.getPath());
            String filename = file.getName();
            Log.d("cordova.open", filename );

            String mime = getMimeType(path);
            Log.d("cordova.open", mime );

            try(InputStream is = new URL( path ).openStream())
            {
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mime); 
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS );

                Uri uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                OutputStream out = contentResolver.openOutputStream(uri);
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.close();
                is.close();
                
                contentValues.clear();
                contentResolver.update(uri, contentValues, null, null);	
                Log.d("cordova.open - delete internal file", path);
    
                //delete the file from the inside app location
                deleteInternalFile(path);

                //now try to open the downloaded file
                Log.d("cordova.open - open intent", uri.getPath() );
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.setDataAndType(uri, context.getContentResolver().getType(uri));
                context.startActivity(i);

                //callback success
                this.callbackContext.success(path);		
            }
            catch ( Exception e) 
            {
                e.printStackTrace();
                this.callbackContext.error(e.getMessage());
            }
        } else {
            callbackContext.error(2);
        }
    }

    private void deleteInternalFile(String path)
	{
		Uri myUri = Uri.parse(path);
		File filePath = new File(myUri.getPath());
		boolean deleted = filePath.delete();	
		if ( deleted )
		{
			Log.d("cordova-open", "deleted internal file:" + path);
		}
	}
}
