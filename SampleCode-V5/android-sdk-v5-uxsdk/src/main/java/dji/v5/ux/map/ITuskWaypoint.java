package dji.v5.ux.map;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;

public interface ITuskWaypoint {
    void addTuskWaypointOnMap(DJILatLng wp);
    void removeTuskWaypoint(DJIMarker wp);

}
