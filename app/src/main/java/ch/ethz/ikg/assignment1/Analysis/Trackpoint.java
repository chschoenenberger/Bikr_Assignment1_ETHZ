package ch.ethz.ikg.assignment1.Analysis;

/**
 * Wrapping a location for our own purposes.
 */
public class Trackpoint {
    private double longitude;
    private double latitude;
    private long time = 0;

    /**
     * Constructor.
     *
     * @param longitude The longitude of this trackpoint.
     * @param latitude  The latitude of this trackpoint.
     */
    public Trackpoint(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Constructor with time
     *
     * @param longitude The longitude of this trackpoint.
     * @param latitude  The latitude of this trackpoint.
     * @param time      Time of this trackpoint.
     */
    public Trackpoint(double longitude, double latitude, long time) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = time;
    }

    /**
     * Gets the longitude.
     *
     * @return The longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Gets the latitude.
     *
     * @return The latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Gets the time.
     *
     * @return The time.
     */
    public long getTime() {
        return time;
    }


    /**
     * Calculates the great circle distance between this Point and the Point passed as an argument.
     * The great circle distance is calculated by using the haversine formula.
     * Also other distance formulas could be used (Euclidean or Manhattan etc.)
     *
     * @param target trackpoint.
     * @return The distance.
     */
    public double distance(Trackpoint target) {
        // Transform to RAD
        double sLon = this.longitude * Math.PI / 180;
        double sLat = this.latitude * Math.PI / 180;
        double tLon = target.getLongitude() * Math.PI / 180;
        double tLat = target.getLatitude() * Math.PI / 180;
        int radius = 6371000;

        // Calculate distance with haversine formula
        return 2 * radius * Math.asin(Math.sqrt(haversine(tLat - sLat) + Math.cos(sLat)
                * Math.cos(tLat) * haversine(tLon - sLon)));
    }

    /**
     * This function implements the haversine function. It is used as a helping function to calculate
     * the great circle distance.
     *
     * @param O is the value on which the haversine function is calculated
     * @return The result of the haversine function is returned
     */
    private double haversine(double O) {
        return (1 - Math.cos(O)) / 2;
    }

    /**
     * Returns speed between this point and a given Trackpoint in km/h
     *
     * @param p2 second Trackpoint
     * @return Speed in km/h
     */
    public double speed(Trackpoint p2) {
        double dist = this.distance(p2); // in meters
        long timeDiff = Math.abs(this.time - p2.getTime()); // 1/1000s
        double speed = dist / timeDiff * 3600;
        return speed;
    }
}
