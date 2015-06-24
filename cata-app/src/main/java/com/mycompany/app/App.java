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

public class App {
    static String auth = "";
    static String ftpUrl = "";
    public static void main( String[] args ) throws Exception
    {
        boolean run = true;
        long start = System.currentTimeMillis( );
        updateGtfs();
        while(run) {
            if((System.currentTimeMillis( ) - start) >= (60*60*24*7) ) {
                updateGtfs();
            }
            if((System.currentTimeMillis( ) - start) >= 30000 ) {
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
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/timestamp.json?auth="+auth,"PUT",String.valueOf(timestamp));
                
                //////////////////////////////////////////////////////////////
                //
                // Deletes all vehicles, to refresh and remove buses that
                // stopped running
                //
                //////////////////////////////////////////////////////////////
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/vehicles.json?auth="+auth,"DELETE","");
                
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
                
                    String vehicle = "\"" + id + "\" : { \"Trip\": \"" + tripId +"\", \"Lat\": \""+ Float.toString(latitude) + "\", \"Long\": \"" + Float.toString(longitude) + "\", \"Timestamp\": \"" + Long.toString(vehicle_timestamp) + "\" },";
                    //System.out.println(vehicle);
                    vehicleUpdateStr += vehicle;
                                
                }
                vehicleUpdateStr += " }";
                vehicleUpdateStr = vehicleUpdateStr.replaceAll(", }", " }");
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/vehicles.json?auth="+auth,"PUT", vehicleUpdateStr);
                
                //////////////////////////////////////////////////////////////
                //
                // Deletes all routes
                //
                //////////////////////////////////////////////////////////////
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/routes.json?auth="+auth,"DELETE","");
                
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
                firebaseCall("https://sizzling-fire-5776.firebaseio.com/routes.json?auth="+auth,"PUT", stopUpdateStr); 
            }
            //run = false;
        }
    }
    
    public static void updateGtfs() {
        firebaseCall("https://sizzling-fire-5776.firebaseio.com/stops.json?auth="+auth,"DELETE","");
        String stops = "";
		String stopTimes = "";
		try {
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
                        stops = dataStr.toString();
                    }
					if(entry.getName().equals("stop_timesWAIT.txt")) {
                        
                        StringBuilder stopTimesStr = new StringBuilder();
                        int bytesRead;
                        byte[] tempBuffer = new byte[2048];
						
                        try {
                            while ((bytesRead = zipStream.read(tempBuffer)) != -1) {
                                stopTimesStr.append(new String(tempBuffer, 0, bytesRead));
                            }
                        } catch (IOException e) {
                            
                        }
                        stopTimes = stopTimesStr.toString();
						System.out.println("Here");
						System.out.println(stopTimes);
                    }
                }
            } catch(Exception e) {
                
            }
		} catch(Exception e) {
			
		}
        
        BufferedReader reader = new BufferedReader(new StringReader(stops));
        String line = "";
        String stopsStr = "{ ";
        try {
            while((line = reader.readLine()) != null) {
                String[] stringArray = line.split(",");
                String latitude = stringArray[0];
                String longitude = stringArray[3];
                String name = stringArray[2];
                String streets = stringArray[7];
                String short_description = stringArray[8];
                
                String singleStop = "\"" + name + "\" : { \"Latitude\": \"" + latitude +"\", \"Longitude\": \""+ longitude + "\", \"Streets\": \"" + streets + "\", \"Description\": \"" + short_description + "\" },";
                stopsStr += singleStop;
                
            }
        } catch(IOException e) {
            
        }
        stopsStr += " }";
        stopsStr = stopsStr.replaceAll(", }", " }");
        firebaseCall("https://sizzling-fire-5776.firebaseio.com/stop_locations.json?auth="+auth,"PUT", stopsStr);
        
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
