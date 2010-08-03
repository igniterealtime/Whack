package org.jivesoftware.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.Message;

import net.sf.jweather.Weather;
import net.sf.jweather.metar.Metar;

/**
 * This component provides weather information obtained from http://weather.noaa.gov. Each request
 * will generate an HTTP request to the above URL. The JWeather library was used for getting
 * weather information in the METAR format.<p>
 *
 * Note: This code shouldn't be considered ready for production since it generate an HTTP request
 * for each received request. Therefore, it won't scale much.
 *
 * @author Gaston Dombiak
 */
public class WeatherComponent extends AbstractComponent {

    Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The XMPP domain to which this component is registered to.
     */
    private String serverDomain;

    /**
     * The name of this component.
     */
    private String name;


    /**
     * Create a new component which provides weather information.
     * 
     * @param name The name of this component.
     * @param serverDomain The XMPP domain to which this component is registered to.
     */
    public WeatherComponent(String name, String serverDomain) {
        this.name = name;
        this.serverDomain = serverDomain;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Weather component - sample component";
    }

    @Override
    public String getDomain() {
        return serverDomain;
    }

    /**
     * Handle a receied message and answer the weather information of the requested station id.
     * The request must be made using Message packets where the body of the message should be the
     * station id.<p>
     *
     * Note: I don't know the list of valid station ids so if you find the list please send it to me
     * so I can add it to this example.
     *
     * @param packet the Message requesting information about a certain station id.
     */
    @Override
    protected void handleMessage(Message message) {
        System.out.println("Received message:"+message.toXML());
        // Get the requested station to obtain it's weather information
        String station = message.getBody();
        // Send the request and get the weather information
        Metar metar = Weather.getMetar(station, 5000);

        // Build the answer
        Message reply = new Message();
        reply.setTo(message.getFrom());
        reply.setFrom(message.getTo());
        reply.setType(message.getType());
        reply.setThread(message.getThread());

        // Append the discovered information if something was found
        if (metar != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("station id : " + metar.getStationID());
            sb.append("\rwind dir   : " + metar.getWindDirection() + " degrees");
            sb.append("\rwind speed : " + metar.getWindSpeedInMPH() + " mph, " +
                                                 metar.getWindSpeedInKnots() + " knots");
            if (!metar.getVisibilityLessThan()) {
                sb.append("\rvisibility : " + metar.getVisibility() + " mile(s)");
            }
            else {
                sb.append("\rvisibility : < " + metar.getVisibility() + " mile(s)");
            }

            sb.append("\rpressure   : " + metar.getPressure() + " in Hg");
            sb.append("\rtemperaturePrecise: " +
                               metar.getTemperaturePreciseInCelsius() + " C, " +
                               metar.getTemperaturePreciseInFahrenheit() + " F");
            sb.append("\rtemperature: " +
                               metar.getTemperatureInCelsius() + " C, " +
                               metar.getTemperatureInFahrenheit() + " F");
            sb.append("\rtemperatureMostPrecise: " +
                               metar.getTemperatureMostPreciseInCelsius() + " C, " +
                               metar.getTemperatureMostPreciseInFahrenheit() + " F");
            reply.setBody(sb.toString());
        }
        else {
            // Answer that the requested station id does not exist
            reply.setBody("Unknown station ID");
        }

        // Send the response to the sender of the request
        send(reply);
    }

}
