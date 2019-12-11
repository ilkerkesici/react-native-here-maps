package com.heremapsrn.react.map;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.os.AsyncTask;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import android.graphics.Bitmap; 
import android.graphics.BitmapFactory;
import 	java.net.URLConnection;
import 	java.io.BufferedInputStream;
import 	java.io.ByteArrayOutputStream;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.ThemedReactContext;


import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapView;
import com.here.android.mpa.common.Image;
import com.heremapsrn.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HereMapView extends MapView {

    private static final String TAG = HereMapView.class.getSimpleName();

    private static final String MAP_TYPE_NORMAL = "normal";
    private static final String MAP_TYPE_SATELLITE = "satellite";

    private Map map;

    private ReactContext reactContext;

    private GeoCoordinate mapCenter;
    private String mapType = "normal";

    private boolean mapIsReady = false;

    private double zoomLevel = 10;

    ArrayList<MapMarker> markers;

    public HereMapView(ReactContext context) {
        super(context);

        markers = new ArrayList<MapMarker>();
        this.reactContext = context;
        MapEngine.getInstance().init(context, new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    Log.i(TAG, "--- Initialization ---");

                    map = new Map();
                    setMap(map);

                    map.setMapScheme(Map.Scheme.NORMAL_DAY);

                    mapIsReady = true;

                    if (mapCenter != null) map.setCenter(mapCenter, Map.Animation.NONE);

                    // Add the marker
                    if (markers != null) {
                        for (MapMarker marker : markers) {
                            map.addMapObject(marker);
                        }
                    }

                    Log.d(TAG, String.format("mapType: %s", mapType));
                    setMapType(mapType);

                    setZoomLevel(zoomLevel);

                    // Create a gesture listener on marker object
                    getMapGesture().addOnGestureListener(
                            new MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                                @Override
                                public boolean onMapObjectsSelected(List<ViewObject> objects) {
                                    for (ViewObject viewObj : objects) {
                                        if (viewObj.getBaseType() == ViewObject.Type.USER_OBJECT) {
                                            if (((MapObject) viewObj).getType() == MapObject.Type.MARKER) {
                                                // At this point we have the originally added
                                                // map marker, so we can do something with it
                                                // (like change the visibility, or more
                                                // marker-specific actions)
                                                if(((MapMarker) viewObj).isInfoBubbleVisible()){
                                                    ((MapMarker) viewObj).hideInfoBubble();
                                                } else {
                                                    ((MapMarker) viewObj).showInfoBubble();
                                                }

                                            }
                                        }
                                    }
                                    // return false to allow the map to handle this callback also
                                    return false;
                                }
                                // Zoomout veya Zoomin algılama
                                @Override
                                public void onPanEnd() {
                                    double level = map.getZoomLevel();
                                    GeoCoordinate center = map.getCenter();
                                    GeoBoundingBox bounding = map.getBoundingBox();
                                    Log.e(TAG, String.format("Map Zoom Level: %f \n", level));
                                    Log.e(TAG, String.format("Latitude: %f, Longitude: %f", center.getLatitude(), center.getLongitude()));
                                    onMove(level, center.getLatitude(), center.getLongitude(), bounding.getTopLeft(), bounding.getBottomRight());
                                }
                            });





                    Log.i(TAG, "INIT FINISH !!!!");

                } else {
                    Log.e(TAG, String.format("Error initializing map: %s", error.getDetails()));
                }


            }
        });


    }
    // Event Emiiter to Ract native 
    private void onMove(double zoom, double latitude, double longitude, GeoCoordinate topLeft, GeoCoordinate bottomRight) { 
        WritableMap payload = Arguments.createMap();
        payload.putDouble("Zoom", zoom);
        payload.putDouble( "CenterLatitude", latitude);
        payload.putDouble("CenterLongitude", longitude);
        payload.putDouble("BottomRightLatitude", bottomRight.getLatitude());
        payload.putDouble("BottomRightLongitude", bottomRight.getLongitude());
        payload.putDouble("TopLeftLatitude", topLeft.getLatitude());
        payload.putDouble("TopLeftLongitude", topLeft.getLongitude());
        this.reactContext.getJSModule( DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onMoveMap", payload); 
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause...");
        MapEngine.getInstance().onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume...");
        MapEngine.getInstance().onResume();
    }

    public void setCenter(ReadableMap center) {

        double latitude = center.getDouble("latitude");
        double longitude = center.getDouble("longitude");

        mapCenter = new GeoCoordinate(latitude, longitude);
        if (mapIsReady) map.setCenter(mapCenter, Map.Animation.LINEAR);

    }

    public void setMapType(String mapType) {
        this.mapType = mapType;
        if (!mapIsReady) return;

        if (mapType.equals(MAP_TYPE_NORMAL)) {
            map.setMapScheme(Map.Scheme.NORMAL_DAY);
        } else if (MAP_TYPE_SATELLITE.equals(mapType)) {
            map.setMapScheme(Map.Scheme.SATELLITE_DAY);
        }
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
        if (!mapIsReady) return;

        map.setZoomLevel(zoomLevel);
    }

    public void setUserLocation(ReadableMap markerPosition) {


        double latitude = markerPosition.getDouble("latitude");
        double longitude = markerPosition.getDouble("longitude");

        // Create a custom marker image
        Image myImage = new Image();

        try {
            myImage.setImageResource(R.drawable.location);
        } catch (IOException e) {
            Log.e(TAG, String.format("Error initializing image marker: %s", e.getMessage()));
        }
        // Create the MapMarker
        MapMarker marker =
                new MapMarker(new GeoCoordinate(latitude, longitude), myImage);
        marker.setAnchorPoint(new PointF(myImage.getWidth() / 2f, myImage.getHeight()));

        markers.add(marker);

        if (mapIsReady) map.addMapObject(marker);

    }

    public void setMarkersList(ReadableArray markersPosition) {
        markers = new ArrayList<MapMarker>();
        for(int i=0; i< markersPosition.size(); i++) {

            ReadableMap readableMap = markersPosition.getMap(i);

            // String[] values = readableMap.getString("location").split(",");

                double latitude = readableMap.getDouble("latitude");
                double longitude = readableMap.getDouble("longitude");

                String title = readableMap.getString("title");
                String description = readableMap.getString("description");
                String eventCategory = readableMap.getString("event_category");

                // Create a custom marker image

                Image myImage = new Image();

                try {
                    switch(eventCategory) {
                        case "0":
                            myImage.setImageResource(R.drawable.location);
                            break;
                        case "1002":
                            myImage.setImageResource(R.drawable.yangin_bina);
                            break;
                        case "1003":
                            myImage.setImageResource(R.drawable.yangin_orman);
                            break;
                        case "1004":
                            myImage.setImageResource(R.drawable.yangin_sanayi);
                            break;
                        case "1005":
                            myImage.setImageResource(R.drawable.yangin);
                            break;
                        case "2001":
                            myImage.setImageResource(R.drawable.altyapi_elektrik);
                            break;
                        case "2002":
                            myImage.setImageResource(R.drawable.altyapi_gaz);
                            break;
                        case "2003":
                            myImage.setImageResource(R.drawable.altyapi_su);
                            break;
                        case "2004":
                            myImage.setImageResource(R.drawable.altyapi_haberlesme);
                            break;
                        case "2005":
                            myImage.setImageResource(R.drawable.altyapi_yol);
                            break;
                        case "2006":
                            myImage.setImageResource(R.drawable.altyapi);
                            break;
                        case "3001":
                            myImage.setImageResource(R.drawable.trafik_ulasim);
                            break;
                        case "3002":
                            myImage.setImageResource(R.drawable.trafik_kaza);
                            break;
                        case "3005":
                            myImage.setImageResource(R.drawable.trafik);
                            break;
                        case "4001":
                            myImage.setImageResource(R.drawable.afet_deprem);
                            break;
                        case "4002":
                            myImage.setImageResource(R.drawable.afet_firtina);
                            break;
                        case "4003":
                            myImage.setImageResource(R.drawable.afet_heyelan);
                            break;
                        case "4004":
                            myImage.setImageResource(R.drawable.afet_sel);
                            break;
                        case "4005":
                            myImage.setImageResource(R.drawable.afet_saganak);
                            break;
                        case "4006":
                            myImage.setImageResource(R.drawable.afet);
                            break;
                        case "5001":
                            myImage.setImageResource(R.drawable.guvenlik_saldiri);
                            break;
                        case "5003":
                            myImage.setImageResource(R.drawable.guvenlik_cinayet);
                            break;
                        case "5004":
                            myImage.setImageResource(R.drawable.guvenlik_hirsizlik);
                            break;
                        case "5005":
                            myImage.setImageResource(R.drawable.guvenlik_teror);
                            break;
                        case "5006":
                            myImage.setImageResource(R.drawable.guvenlik);
                            break;
                        default:
                            // code block
                            myImage.setImageResource(R.drawable.marker);
                    }
                } catch (IOException e) {
                    Log.e(TAG, String.format("Error initializing image marker: %s", e.getMessage()));
                }
                
                MapMarker marker = new MapMarker(new GeoCoordinate(latitude, longitude), myImage);
                marker.setAnchorPoint(new PointF(myImage.getWidth() / 2f, myImage.getHeight()));

                marker.setTitle(title);
                marker.setDescription(description);

                // Add the MapMarker in the list
                markers.add(marker);

                if (mapIsReady) map.addMapObject(marker);
        }

    }
}
