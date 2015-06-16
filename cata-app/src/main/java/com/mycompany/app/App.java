package com.mycompany.app;

import java.io.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.*;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
        boolean run = true;
        while(run) {
            URL url = new URL("http://developers.cata.org/gtfsrt/vehicle/vehiclepositions.pb");
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());
            
            
            //////////////////////////////////////////////////////////////
            //
            // Gets the execution time
            //
            //////////////////////////////////////////////////////////////
            long timestamp = feed.getHeader().getTimestamp();
            firebaseCall("https://sizzling-fire-5776.firebaseio.com/timestamp.json?auth=6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE","PUT",String.valueOf(timestamp));
            
            System.out.println("Updating");
            
            //////////////////////////////////////////////////////////////
            //
            // Deletes all vehicles, to refresh and remove buses that
            // stopped running
            //
            //////////////////////////////////////////////////////////////
            firebaseCall("https://sizzling-fire-5776.firebaseio.com/vehicles.json?auth=6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE","DELETE","");
            
            //////////////////////////////////////////////////////////////
            //
            // Updates buses in firebase
            //
            //////////////////////////////////////////////////////////////            
            String vehicleUpdateStr = "{ ";
            for (FeedEntity entity : feed.getEntityList()) {
                            
                String id = entity.getId();
                String tripId = entity.getVehicle().getTrip().getTripId();
                float latitude = entity.getVehicle().getPosition().getLatitude();
                float longitude = entity.getVehicle().getPosition().getLongitude();
                long vehicle_timestamp = entity.getVehicle().getTimestamp();
            
                String vehicle = "\"" + id + "\" : { \"Trip\": \"" + tripId +"\", \"lat\": \""+ Float.toString(latitude) + "\", \"long\": \"" + Float.toString(longitude) + "\", \"timestamp\": \"" + Long.toString(vehicle_timestamp) + "\" },";
                //System.out.println(vehicle);
                vehicleUpdateStr += vehicle;
                            
            }
            vehicleUpdateStr += " }";
            vehicleUpdateStr = vehicleUpdateStr.replaceAll(", }", " }");
            firebaseCall("https://sizzling-fire-5776.firebaseio.com/vehicles.json?auth=6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE","PUT", vehicleUpdateStr);
            
            //////////////////////////////////////////////////////////////
            //
            // Wait before updating, according to aggrement
            //
            //////////////////////////////////////////////////////////////
            try {
                Thread.sleep(15000);                 //1000 milliseconds is one second.
            } catch(InterruptedException ex) {
                //Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void firebaseCall(String _url,String _method,String _data) {
        String dataStr = null;
        dataStr = _data ;
        
        byte[] data = dataStr.getBytes();
        //InputStream vehicleStream = null;
        try {
            URL url = new URL(_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(_method);
            conn.setRequestProperty("Content-Length", Integer.toString(data.length));
            conn.setUseCaches(false);
            OutputStream out = conn.getOutputStream();
            out.write(data);
            out.close();
            int responseCode = conn.getResponseCode(); 
        } catch(Exception e) {
            System.out.println("exception");
        }
    }
}
