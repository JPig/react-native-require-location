package com.location.required;

import android.app.Application;
import android.content.ComponentName;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.text.Html;
import com.facebook.react.bridge.*;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.Build;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.PendingResult;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class LocationServicesRequiredModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    
    //Globals
    private Promise promiseCallback;
    private ReadableMap map;
    private ReactApplicationContext mReactContext;
    private LocationManager locationManager; 
    private Activity activity;   

    public LocationServicesRequiredModule(ReactApplicationContext reactContext) {
        super(reactContext);                
        reactContext.addActivityEventListener(this); 
        mReactContext = reactContext;        
        locationManager = (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public String getName() {
        return "LocationServicesRequired";
    }

    @ReactMethod
    public void showSettings(ReadableMap configMap, Promise promise) {      
      
        promiseCallback = promise;
        map = configMap; 
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          displayLocationSettingsRequest(mReactContext); //newer devices (6.0+)
        } else {
          displayLocationSettings(); //older devices
        }
    }  

    
    private void displayLocationSettings() { 
      
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
          promiseCallback.reject("Activity doesn't exist");
          return;
        }
      
        activity = currentActivity;
      
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);        
        
        //Set Dialog Content
        builder.setMessage(Html.fromHtml(map.getString("message")))
          .setPositiveButton(map.getString("ok"),
            new DialogInterface.OnClickListener() {              
                public void onClick(DialogInterface dialogInterface, int id) {    
                  try {                    
                    Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivityForResult(settingsIntent, 1234);          
                    dialogInterface.dismiss();                    
                  } catch(Exception e) {
                    promiseCallback.reject(e);
                  } 
                }
            })
          .setNegativeButton(map.getString("cancel"),
            new DialogInterface.OnClickListener() {                    
                public void onClick(DialogInterface dialogInterface, int id) {
                    promiseCallback.reject(new Throwable("disabled"));
                    getActivity().finish();        
                }                      
            });
              
        //Show Dialog    
        builder.create().show();
    }
     
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {      
      if (requestCode == 1234) {
        switch (resultCode) {          
            case Activity.RESULT_CANCELED:            
              activity.finish();
              break;
            
            default:
              checkLocationService(); 
              break;            
        }        
      }
    }
    
    @ReactMethod
    public void isLocationEnabled(Promise promise) {    
      try {
         int isLocationEnabled = _isLocationEnabled(getContext()) ? 1 : 0;
         promiseCallback.resolve(isLocationEnabled);
      } catch (SettingNotFoundException e) {
         promiseCallback.reject("Location setting error occured");
      }
    }  
    
    
    public void checkLocationService() {    
      try {
         int isLocationEnabled = _isLocationEnabled(getContext()) ? 1 : 0;
         promiseCallback.resolve("enabled");
      } catch (SettingNotFoundException e) {
         promiseCallback.reject(new Throwable("disabled"));
      }
    }
    
    
    public static boolean _isLocationEnabled(Context context) throws SettingNotFoundException {
      
       int locationMode = 0;
       String locationProviders;

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
           locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
           return locationMode != Settings.Secure.LOCATION_MODE_OFF;

       } else {
           locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
           return !TextUtils.isEmpty(locationProviders);
       }
    }  
    
    private void displayLocationSettingsRequest(Context context) {
      
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
          promiseCallback.reject("Activity doesn't exist");
          return;
        }
      
        activity = currentActivity; //set activity
      
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());        
        
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {                  
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {                
                            status.startResolutionForResult(activity, 1234);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            promiseCallback.reject(new Throwable("Something broke"));
                        }
                        break;
                }
            }
        });       
      
    }
   
    //****** HELPERS **********//
   
    protected Activity getActivity() {
        return this.getCurrentActivity();        
    }

    protected Application getApplication() {
        return activity.getApplication();
    }

    protected Context getContext() {      
      return activity.getApplicationContext();             
    }
}
