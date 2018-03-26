/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class GpsBoxProtocolDecoder extends BaseProtocolDecoder {

    public GpsBoxProtocolDecoder(GpsBoxProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("A:")
            .number("(d+),")                     // imei
            .text("B:")
            .expression("([^,]+),")              // battery in volts
            .text("C:")
            .expression("([^,]+),")              // fuel in %
            .text("D:")
            .number("(dd)")                     // time in HH
            .number("(dd)")                     // time in MM
            .number("(dd),")                    // time in SS
            .text("E:")
            .number("(d+)(dd.d+)")              // latitude (ddmm.mmmm)
            .expression("([NSEW])").optional()
            .text(",F:")
            .number("(d+)(dd.d+)")              // longitude (dddmm.mmmm)
            .expression("([EW])?").optional()
            .text(",G:")
            .number("(d+.?d*)?,")                // speed
            .text("H:")
            .number("(dd)")                     // date in DD , GMT 0
            .number("(dd)")                     // date in MM , GMT 0
            .number("(dd),")                     // date in YY , GMT 0
            .text("I:")
            .expression("([GL]),")                 // FORMAT (G for GPS [value in Degree], L for LBS [value in seconds])
            .text("J:")
//            .expression("([^,]+),")            // bit1
            .number("(x)")                       // Engine ON/OFF status bit
            .number("(x)")                       // SOS button pressed or not status bit
            .number("(x)")                       // Security Mode Bit
            .number("(x)")                       // AC ON/OFF status bit
            .number("(x)")                       // Shock status bit
            .number("(x)")                       // External battery connected or not status bit
            .number("(x)")                       // Tempering switch pressed or not status bit
            .number("(x),")                      // Over speed status bit
            .text("K:")
//            .expression("([^,]+),")            // bit2
            .number("(x)")                       // Immobilize Status
            .number("(x)")                       //
            .number("(x)")                       //
            .number("(x)")                       //
            .number("(x)")                       //
            .number("(x)")                       //
            .number("(x)")                       //
//            .number("(.),")                      // For GPS Fix Indication (‘A’ if GPS fix and ‘V’ if GPS not fixed )
            .expression("([A-Za-z0-9]+),")
            .text("L:")
            .number("(d+),")                     // pin
            .text("M:")
            .number("(d+.?d*)?,?")               // course
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (!sentence.isEmpty()) {
            if (channel != null) {
                channel.write("+##Received OK", remoteAddress);
            }
            int start = sentence.indexOf("{");
            if (start >= 0) {
                sentence = sentence.substring(start + 1, sentence.length() - 1);
            } else {
                return null;
            }
        }

        Position position = new Position(getProtocolName());

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_BATTERY, parser.next());
        position.set(Position.KEY_FUEL_LEVEL, parser.next());

        int localHours = parser.nextInt(0);
        int localMinutes = parser.nextInt(0);
        int localSeconds = parser.nextInt(0);

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_HEM));
        position.setSpeed(parser.nextDouble(0));

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        dateBuilder.setTime(localHours, localMinutes, localSeconds);
        position.setTime(dateBuilder.getDate());
        position.setValid(parser.next().equals("G"));

        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_IGNITION, true);
        } else {
            position.set(Position.KEY_IGNITION, false);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_INPUT, true); //Need to check and update
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_SHOCK);
        }
        if (parser.nextInt(0) == 0) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
            position.set(Position.KEY_CHARGE, false);
        } else {
            position.set(Position.KEY_CHARGE, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }

        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_COMMAND, "Immobilize");
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 1, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 2, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 3, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 4, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 5, true);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.PREFIX_IO + 6, true);
        }

        Position last = Context.getIdentityManager().getLastPosition(position.getDeviceId());

//        boolean speedLogic = (position.getSpeed() > 0.0
//                && last.getSpeed() == 0.0)
//                || (position.getSpeed() == 0.0 && last.getSpeed() > 0.0)
//                || position.getSpeed() > 0.0;

        boolean speedLogic = last == null || (position.getSpeed() > 0.0
                && last.getSpeed() > 0.0);

        if (last == null || (parser.next().equals("A") && (
                position.getBoolean(Position.KEY_IGNITION)
                || (last.getLatitude() != position.getLatitude()
                && last.getLongitude() != position.getLongitude()
                && speedLogic)
                || (last.getCourse() != position.getCourse()
                && speedLogic)))) {
            position.set(Position.KEY_GPS, true);
//            last.setSpeed(position.getSpeed());
            position.set(Position.KEY_VIN, parser.next());
            position.setCourse(parser.nextDouble(0));
        } else {
            getLastLocation(position, null);
            position.setSpeed(position.getSpeed());
        }

        return position;
    }

}
