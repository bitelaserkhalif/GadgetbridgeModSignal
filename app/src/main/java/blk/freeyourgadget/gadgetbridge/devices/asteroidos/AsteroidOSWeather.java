package blk.freeyourgadget.gadgetbridge.devices.asteroidos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import blk.freeyourgadget.gadgetbridge.model.WeatherSpec;


/**
 * An adapter class for weather
 */
public class AsteroidOSWeather {
    /**
     * Provides a day's worth of weather
     */
    public class Day {
        /**
         * The minimum temp of the day
         */
        public int minTemp;
        /**
         * The maximum temp of the day
         */
        public int maxTemp;
        /**
         * The current OWM weather condition code
         */
        public int condition;

        /**
         * Creates a Day from the forecast given
         * @param forecast
         */
        public Day(WeatherSpec.Forecast forecast) {
            minTemp = forecast.minTemp;
            maxTemp = forecast.maxTemp;
            condition = forecast.conditionCode;
        }

        /**
         * Creates a Day from the WeatherSpec given
         * @param spec
         */
        public Day(WeatherSpec spec) {
            minTemp = spec.todayMinTemp;
            maxTemp = spec.todayMaxTemp;
            condition = spec.currentConditionCode;
        }
    }

    /**
     * The days of the weather
     */
    public Day[] days = new Day[5];
    /**
     * The city name of the weather
     */
    public String cityName = "";


    /**
     * Creates an AsteroidOSWeather from the WeatherSpec given
     * @param spec
     */
    public AsteroidOSWeather(WeatherSpec spec) {
        cityName = spec.location;
        days[0] = new Day(spec);
        for (int i = 1; i < 5 && i < spec.forecasts.size(); i++) {
            days[i] = new Day(spec.forecasts.get(i));
        }
    }

    /**
     * Returns a byte array of the city name
     * @return a byte array of the city name
     */
    public byte[] getCityName() {
        return cityName.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parses the days' weather conditions and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getWeatherConditions() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.condition >> 8));
            stream.write((byte) (day.condition));
        }
        return stream.toByteArray();
    }

    /**
     * Parses the days' min temps and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getMinTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.minTemp >> 8));
            stream.write((byte) (day.minTemp));
        }
        return stream.toByteArray();
    }

    /**
     * Parses the days' max temps and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getMaxTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.maxTemp >> 8));
            stream.write((byte) (day.maxTemp));
        }
        return stream.toByteArray();
    }
}
