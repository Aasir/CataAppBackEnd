package com.mycompany.app;

import java.io.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.*;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
        String auth = "6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE";
        boolean run = true;
        long start = System.currentTimeMillis( );
        updateGtfs();
        while(run) {
            if((System.currentTimeMillis( ) - start) >= (60*60*24*7) ) {
                updateGtfs();
            }
            if((System.currentTimeMillis( ) - start) >= 0000 ) {
                start = System.currentTimeMillis( );
                System.out.println("Updating");
                
                //////////////////////////////////////////////////////////////
                //
                // Get the feeds for buses and routes
                //
                //////////////////////////////////////////////////////////////
                URL vehiclesPB = new URL("http://developers.cata.org/gtfsrt/vehicle/vehiclepositions.pb");
                FeedMessage feed = FeedMessage.parseFrom(vehiclesPB.openStream());
                
                URL tripsPB = new URL("http://developers.cata.org/gtfsrt/tripupdate/tripupdates.pb");
                FeedMessage tripsFeed = FeedMessage.parseFrom(tripsPB.openStream());
                
                
                //////////////////////////////////////////////////////////////
                //
                // Gets the execution time
                //
                //////////////////////////////////////////////////////////////
                long timestamp = feed.getHeader().getTimestamp();
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/timestamp.json?auth=6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE","PUT",String.valueOf(timestamp));
                
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
                // Updates routes into firebase
                //
                //////////////////////////////////////////////////////////////
                String stopUpdateStr = "{ ";
                for (FeedEntity entity : tripsFeed.getEntityList()) {
                                
                    String id = entity.getId();
                    String tripId = entity.getTripUpdate().getTrip().getTripId();
                    String tripStartTime = entity.getTripUpdate().getTrip().getStartTime();
                    String tripStartDate = entity.getTripUpdate().getTrip().getStartDate();
                    String tripRoute = entity.getTripUpdate().getTrip().getRouteId();
                    String vehicleId = entity.getTripUpdate().getVehicle().getId();
                    
                    String route = "\"" + id + "\" : { \"StartTime\": \"" + tripStartTime +"\", \"StartDate\": \""+ tripStartDate + "\", \"RouteNumber\": \"" + tripRoute + "\", \"BusNumber\": \"" + vehicleId + "\", \"Stops\": {"; 

                    String stopsListStr = "";
                    for (com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate item : entity.getTripUpdate().getStopTimeUpdateList()) {
                        String stopNumber = String.valueOf(item.getStopSequence());
                        String delay = Integer.toString(item.getDeparture().getDelay());
                        String time = Long.toString(item.getDeparture().getTime());
                        String stopId = item.getStopId();
                        
                        String stop = "\"" + stopNumber + "\" : { \"Delay\": \"" + delay +"\", \"Time\": \""+ time + "\", \"StopId\": \"" + stopId + "\"}, ";
                        stopsListStr += stop;
                    }
                    route += stopsListStr;
                    route += " } }, ";
                    stopUpdateStr += route;  
                }
                stopUpdateStr += " }";
                stopUpdateStr = stopUpdateStr.replaceAll(",  }", " }");
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/routes.json?auth=6whGHpWI2HxG1chn1ar82m0eG2303MzLDQdDMHoE","PUT", stopUpdateStr);
                
                
            }
            run = false;
        }
    }
    
    public static void updateGtfs() {
		try {
			String ftpUrl = "";
			String saveFile = "../gtfs.zip";
			URL url = new URL(ftpUrl);
			 
			URLConnection conn = url.openConnection();
			InputStream inputStream = conn.getInputStream();
            
            ZipInputStream zipStream = new ZipInputStream(inputStream);
            			
			try {
                ZipEntry entry;
                while((entry = zipStream.getNextEntry())!=null) {
                    if(entry.getName().equals("stops.txt")) {
                        
                        StringBuilder dataStr = new StringBuilder();
                        int bytesRead;
                        byte[] tempBuffer = new byte[2048];
                        try {
                            while ((bytesRead = zipStream.read(tempBuffer)) != -1) {
                                dataStr.append(new String(tempBuffer, 0, bytesRead));
                            }
                        } catch (IOException e) {
                            
                        }
                        System.out.println(dataStr);
                    }
                }
            } catch(Exception e) {
                
            }
		} catch(Exception e) {
			
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
