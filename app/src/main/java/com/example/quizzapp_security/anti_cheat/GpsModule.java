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

    private static class AllowedLocation {
        double lat;
        double lng;
        float radius;
        String name;

        AllowedLocation(String name, double lat, double lng, float radius) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.radius = radius;
        }
    }

    public interface OnLocationAlertListener {
        void onOutsideZone(String details);
    }

    public GpsModule(Context context, OnLocationAlertListener listener) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.listener = listener;
        this.allowedLocations = new ArrayList<>();

        // Zone 1
        allowedLocations.add(new AllowedLocation("Centre", 33.5731, -7.5898, 1000));
        // Zone 2
        allowedLocations.add(new AllowedLocation("École", 33.6000, -7.6000, 1000));
        // Zone 3 (Mise à jour avec votre position réelle détectée)
        allowedLocations.add(new AllowedLocation("Lieu 3 (Mise à jour)", 33.581435, -7.6334967, 1000));
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
        String closestZone = "";

        Log.d(TAG, "Position détectée: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());

        for (AllowedLocation allowed : allowedLocations) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), 
                                   allowed.lat, allowed.lng, results);
            float distance = results[0];

            Log.d(TAG, "Distance de " + allowed.name + ": " + (int)distance + "m (Rayon: " + allowed.radius + "m)");

            if (distance <= allowed.radius) {
                isInsideAnyZone = true;
                break;
            }
            if (distance < minDistance) {
                minDistance = distance;
                closestZone = allowed.name;
            }
        }

        if (!isInsideAnyZone) {
            Log.w(TAG, "ALERTE: Utilisateur hors zone !");
            listener.onOutsideZone("Hors zone. Distance min: " + (int)minDistance + "m de " + closestZone);
        } else {
            Log.d(TAG, "Position validée (dans une zone autorisée)");
        }
    }
}