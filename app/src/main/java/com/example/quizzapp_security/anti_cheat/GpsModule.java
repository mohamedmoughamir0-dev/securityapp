package com.example.quizzapp_security.anti_cheat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import java.util.ArrayList;
import java.util.List;

public class GpsModule {
    private final FusedLocationProviderClient fusedLocationClient;
    private final OnLocationAlertListener listener;
    private final List<AllowedLocation> allowedLocations;
    private static final String TAG = "GpsModule";
    
    private int outOfZoneCount = 0;
    private static final int ALERT_THRESHOLD = 2; 

    private static class AllowedLocation {
        double lat; double lng; float radius; String name;
        AllowedLocation(String name, double lat, double lng, float radius) {
            this.name = name; this.lat = lat; this.lng = lng; this.radius = radius;
        }
    }

    public interface OnLocationAlertListener {
        void onOutsideZone(String details);
        void onInsideZone(); 
    }

    public GpsModule(Context context, OnLocationAlertListener listener) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.listener = listener;
        this.allowedLocations = new ArrayList<>();

        // Coordonnées basées sur votre capture d'écran de l'émulateur
        // Point 1 (Celui que vous avez tapé)
        allowedLocations.add(new AllowedLocation("Point Actuel", 33.533984, -7.651264, 20000));
        
        // Point 2 (EMSI sauvegardé)
        allowedLocations.add(new AllowedLocation("EMSI", 33.5814, -7.6335, 3000));
    }

    @SuppressLint("MissingPermission")
    public void checkLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
            .addOnSuccessListener(location -> {
                if (location != null) {
                    processLocation(location);
                } else {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                        if (lastLoc != null) processLocation(lastLoc);
                    });
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Erreur GPS: " + e.getMessage()));
    }

    private void processLocation(Location location) {
        boolean isInsideAnyZone = false;
        float minDistance = Float.MAX_VALUE;

        for (AllowedLocation allowed : allowedLocations) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), 
                                   allowed.lat, allowed.lng, results);
            float distance = results[0];

            if (distance <= allowed.radius) {
                isInsideAnyZone = true;
                break;
            }
            if (distance < minDistance) minDistance = distance;
        }

        if (isInsideAnyZone) {
            outOfZoneCount = 0;
            Log.d(TAG, "Position OK. Distance: " + (int)minDistance + "m");
            listener.onInsideZone(); 
        } else {
            outOfZoneCount++;
            Log.w(TAG, "Hors zone! Distance: " + (int)minDistance + "m. Tentative: " + outOfZoneCount);
            if (outOfZoneCount >= ALERT_THRESHOLD) {
                listener.onOutsideZone("Hors zone (" + (int)minDistance + "m)");
            }
        }
    }
}
