import com.ivkos.gpsd4j.client.GpsdClient;
import com.ivkos.gpsd4j.client.GpsdClientOptions;
import com.ivkos.gpsd4j.messages.DeviceMessage;
import com.ivkos.gpsd4j.messages.PollMessage;
import com.ivkos.gpsd4j.messages.reports.SKYReport;
import com.ivkos.gpsd4j.messages.reports.TPVReport;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

public class GPSTracker {

    public static void main(String[] args) throws Exception {


//        It is used to set some options to make connection for different situations
        GpsdClientOptions options = new GpsdClientOptions()
                .setReconnectOnDisconnect(true)
                .setConnectTimeout(3000)
                .setIdleTimeout(30)
                .setReconnectAttempts(5)
                .setReconnectInterval(3000);

        // Here we are creating client which uses server and port and also options to connect with gpsd server
        GpsdClient client = new GpsdClient("localhost", 2947, options)
                .setSuccessfulConnectionHandler( gpsdClient -> {
                    DeviceMessage device = new DeviceMessage();
                    device.setPath("/dev/ttyAMA0");
                    device.setNative(true);
                    gpsdClient.sendCommand(device);
                    gpsdClient.watch();
                });

        client.addErrorHandler(System.err::println);


        // It is a handler to take information if connection will be successfull
        client.addHandler(TPVReport.class, tpv ->{
            Double latitude = tpv.getLatitude();
            Double longitude = tpv.getLongitude();
            Double speed = tpv.getSpeed();
            String device = tpv.getDevice();
            LocalDateTime localDateTime = tpv.getTime();

            // Here we are writing url and format of data that will be sent as a POST request
            String url = "";
            String data = "lat=" + latitude + "&lng=" + longitude + "&spd="+ speed + "&dev=" + device + "&time=" + localDateTime;


            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");

                con.setDoOutput(true);

                OutputStream os = con.getOutputStream();
                os.write(data.getBytes());

                os.close();

                int responseCode = con.getResponseCode();
                System.out.println("Response code: " + responseCode);

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            System.out.printf("Lat: %f, Lon: %f\n", latitude, longitude );
        });

        client.addHandler(SKYReport.class, sky ->{
            int numberOfSatellites = sky.getSatellites().size();
            System.out.printf("There are %f satellites\n", numberOfSatellites);
        });

//        client.sendCommand(new PollMessage(), pollMessage -> {
//           Integer activeDevices = pollMessage.getActiveCount();
//        });
        client.start();


//        client.sendCommand().sendCommand(),




        client.stop();
    }

}
