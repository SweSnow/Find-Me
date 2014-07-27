package com.simon.findme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.rosaloves.bitlyj.Bitly.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class StartActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /*Location objects*/
    public LocationClient mLocationClient;
    public Location mCurrentLocation;
    public LocationManager locationManager;
    public LocationListener locationListener;
    public Location lastGpsLocation;

    /*Views*/
    private TextView mAddress;
    private ProgressBar mActivityIndicator;
    private GoogleMap map;
    private ImageButton mRefreshButton;
    private LinearLayout mLocationUpdateLayout;

    /*Various*/
    public static URLShort urlShort;
    public boolean inDetailMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //We do this to change the Action Bar title font
        this.getActionBar().setDisplayShowCustomEnabled(true);
        this.getActionBar().setDisplayShowTitleEnabled(false);

        //Inflate the mew layout...
        LayoutInflater inflator = LayoutInflater.from(this);
        View v = inflator.inflate(R.layout.titleview, null);

        //...And set the font
        ((TextView)v.findViewById(R.id.title)).setText(this.getTitle());

        this.getActionBar().setCustomView(v);

        setContentView(R.layout.activity_start);

        mAddress = (TextView) findViewById(R.id.address);
        mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
        mRefreshButton = (ImageButton) findViewById(R.id.refresh_btn);
        mLocationUpdateLayout = (LinearLayout) findViewById(R.id.location_update_layout);

        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshlocation(true);
            }
        });

        mLocationClient = new LocationClient(this, this, this);

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                locationManager.removeUpdates(locationListener);

                Location clickedLocation = new Location("Clicked location");
                clickedLocation.setLatitude(latLng.latitude);
                clickedLocation.setLongitude(latLng.longitude);

                mCurrentLocation = clickedLocation;
                refreshlocation(true);

                inDetailMode = true;

                updateDetailMode();
            }
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                mCurrentLocation = location;
                lastGpsLocation = location;

                refreshlocation(false);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        locationManager.removeUpdates(locationListener);
        super.onStop();
    }

    //This is the onClick() method for our main layout's button.
    public void shareLocation(View v) {

        if (mCurrentLocation == null) {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_LONG).show();
        } else {
            //We take the base url for Google Maps. "daddr=" is Destionation Adress
            // and we just add the latitude and Longitude of the current location
            String longUrl = "http://maps.google.com/?daddr=" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude();

            urlShort = new URLShort();

            urlShort.execute(longUrl, getAdressFromLocation(mCurrentLocation));
        }

    }

    public void updateDetailMode() {
        if (inDetailMode) {
            mLocationUpdateLayout.setVisibility(View.VISIBLE);

            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            mLocationUpdateLayout.startAnimation(anim);
        } else {
            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mLocationUpdateLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            mLocationUpdateLayout.startAnimation(anim);
        }
    }

    public void resumeLocationUpdates(View v) {
        inDetailMode = false;
        updateDetailMode();

        mCurrentLocation = lastGpsLocation;
        refreshlocation(true);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void refreshlocation(boolean animate) {

        if (mCurrentLocation == null) {

            if (mLocationClient.getLastLocation() == null) {
                //Show alert prompting user to enable location access
                EnableLocationDialogFragment frag = new EnableLocationDialogFragment();
                frag.show(getFragmentManager(), "EnableLocationDialogFragment");

                mAddress.setText("Error");

            } else {
                mCurrentLocation = mLocationClient.getLastLocation();
                refreshlocation(true);
            }

        } else {
            // Ensure that a Geocoder services is available
            if (Geocoder.isPresent()) {
                // Show the activity indicator
                mActivityIndicator.setVisibility(View.VISIBLE);
                mRefreshButton.setVisibility(View.GONE);
            /*s
             * Reverse geocoding is long-running and synchronous.
             * Run it on a background thread.
             * Pass the current location to the background task.
             * When the task finishes,
             * onPostExecute() displays the address.
             */
                (new GetAddressTask(this)).execute(mCurrentLocation);
            }

            //We create a LatLng position from our mCurrentPosition which can be used by the GoogleMap object
            LatLng position = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

            //Move the camera and add the marker in the correct location
            if (animate) {

                CameraPosition cameraPosition = CameraPosition.builder()
                        .target(position)
                        .zoom(13)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
            }

            map.clear();
            map.addMarker(new MarkerOptions()
                    .title("Current position")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher))
                    .position(position));
        }


    }


    public class URLShort extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShortenUrlDialogFragment frag = new ShortenUrlDialogFragment();
            frag.show(getFragmentManager(), "ShortenUrlDialogFragment");
        }

        @Override
        protected String doInBackground(String... args) {
            //  simmepinne is my username and R_48e81964547b41c697d3c66c833ad59d is the API key I use
            String finalUrl = as("simmepinne", "R_48e81964547b41c697d3c66c833ad59d").call(shorten(args[0])).getShortUrl();

            String longAdress = args[1];
            //We shorten the full adress (Ex. Oxhagsvägen 68, Vendelsö, Sweden) to a shorter one such as
            //just "Oxhagsvägen 68". This provides some context for the shared text without interupting the user flow.

            boolean hasFoundComma = false;
            StringBuilder sb = new StringBuilder();
            int i = 0;

            while (!hasFoundComma) {

                Character charAtI = longAdress.charAt(i);

                if (Character.toString(charAtI).equals(",")) {
                    hasFoundComma = true;
                } else {
                    sb.append(Character.toString(charAtI));
                }
                i++;
            }

            return sb.toString() + " - " + finalUrl;
        }

        @Override
        protected void onPostExecute(String shortUrl) {
            share(shortUrl);
        }
    }

    public void share(String shareText) {
        //We just use a normal share intent
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(sharingIntent, "Share using"));

        Fragment frag = getFragmentManager().findFragmentByTag("ShortenUrlDialogFragment");
        if (frag instanceof ShortenUrlDialogFragment) {
            ((ShortenUrlDialogFragment) frag).dismiss();
        }
    }

    /*
    * Alert Dialog to prompt user to enable location access
    */

    public static class EnableLocationDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Location update failed, plese enable GPS from the settings")
                    .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                        }
                    })
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }



    /*
    * Alert Dialog to show shortening URL progress spinner
    */

    public static class ShortenUrlDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = getActivity().getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.url_short_layout, null));

            // Create the AlertDialog object and return it
            AlertDialog alert = builder.create();
            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Fragment frag = getFragmentManager().findFragmentByTag("ShortenUrlDialogFragment");
                    if (frag instanceof ShortenUrlDialogFragment) {
                        if (urlShort != null) {
                            urlShort.cancel(true);
                        }
                    }
                    dialog.dismiss();
                }
            });
            return builder.create();
        }
    }



    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        mCurrentLocation = mLocationClient.getLastLocation();
        refreshlocation(true);

    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */

            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show();


        }
    }


    /**
     * A subclass of AsyncTask that calls getFromLocation() in the
     * background. The class definition has these generic types:
     * Location - A Location object containing
     * the current location.
     * Void     - indicates that progress units are not used
     * String   - An address passed to onPostExecute()
     */
    private class GetAddressTask extends
            AsyncTask<Location, String, String> {
        Context mContext;
        public GetAddressTask(Context context) {
            super();
            mContext = context;
        }

        /**
         * Get a Geocoder instance, get the latitude and longitude
         * look up the address, and return it
         *
         * @params params One or more Location objects
         * @return A string containing the address of the current
         * location, or an empty string if no address can be found,
         * or an error message
         */

        @Override
        protected String doInBackground(Location... params) {
            return getAdressFromLocation(params[0]);
        }

        @Override
        protected void onPostExecute(String address) {
            // Set activity indicator visibility to "gone"
            mActivityIndicator.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.VISIBLE);
            // Display the results of the lookup.
            mAddress.setText(address);
        }
    }

    public String getAdressFromLocation(Location loc) {
        Geocoder geocoder =
                new Geocoder(this, Locale.getDefault());
        // Create a list to contain the result address
        List<Address> addresses = null;
        try {
                /*
                 * Return 1 address.
                 */
            addresses = geocoder.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 1);
        } catch (IOException e1) {
            Log.e("LocationSampleActivity",
                    "Error, try again");
            e1.printStackTrace();
            return ("Error, try again");
        } catch (IllegalArgumentException e2) {
            // Error message to post in the log
            String errorString = "Illegal arguments " +
                    Double.toString(loc.getLatitude()) +
                    " , " +
                    Double.toString(loc.getLongitude()) +
                    " passed to address service";
            Log.e("LocationSampleActivity", errorString);
            e2.printStackTrace();
            return errorString;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            // Get the first address
            Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
            String addressText = String.format(
                    "%s, %s, %s",
                    // If there's a street address, add it
                    address.getMaxAddressLineIndex() > 0 ?
                            address.getAddressLine(0) : "",
                    // Locality is usually a city
                    address.getLocality(),
                    // The country of the address
                    address.getCountryName());
            // Return the text
            return addressText;
        } else {
            return "No address found";
        }
    }
}
