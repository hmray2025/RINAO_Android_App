package dji.v5.ux.mapkit.maplibre.provider;

import android.content.Context;
import android.view.Gravity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.WellKnownTileServer;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;

import androidx.annotation.NonNull;
import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.MapkitOptions;
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal;
import dji.v5.ux.mapkit.core.places.IInternalPlacesClient;
import dji.v5.ux.mapkit.core.providers.MapProvider;
import dji.v5.ux.mapkit.maplibre.map.MaplibreMapView;
import dji.v5.ux.mapkit.maplibre.place.MaplibrePlaceDelegate;

import static dji.v5.ux.mapkit.core.Mapkit.MapProviderConstant.MAPLIBRE_MAP_PROVIDER;


import dji.v5.ux.mapkit.maplibre.map.MaplibreMapView;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
public class MaplibreProvider extends MapProvider {

    public MaplibreProvider() {
        providerType = MAPLIBRE_MAP_PROVIDER;
    }

    @Override
    protected DJIMapViewInternal requestMapView(@NonNull Context context,
                                                @NonNull MapkitOptions mapkitOptions) {
        DJIMapViewInternal mapView = null;

        final int mapType = mapkitOptions.getMapType();
        Mapkit.mapType(mapType);
        Mapkit.mapProvider(providerType);
        Mapbox.getInstance(context.getApplicationContext(), Mapkit.getMapboxAccessToken(), WellKnownTileServer.Mapbox);
        MapboxMapOptions options = MapboxMapOptions.createFromAttributes(context);
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(new LatLng(40.0150, -105.2705)) // Sets the new camera position to Boulder
//                .zoom(11)
//                .build();
//        options.camera(cameraPosition);
        options.textureMode(true);
        options.attributionGravity(Gravity.BOTTOM | Gravity.RIGHT);
        options.logoGravity(Gravity.BOTTOM | Gravity.RIGHT);
        options.logoMargins(new int[]{0, 0, 75, 12});
        mapView = new MaplibreMapView(context, options);

        return mapView;
    }

    @Override
    protected IInternalPlacesClient requestGeocodingClient(Context context, MapkitOptions mapkitOptions) {
        IInternalPlacesClient client = null;
        Mapkit.geocodingProvider(getProviderType());
        client = new MaplibrePlaceDelegate();
        return client;
    }
}
