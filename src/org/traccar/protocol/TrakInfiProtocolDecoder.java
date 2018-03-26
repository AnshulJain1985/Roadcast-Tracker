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
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class TrakInfiProtocolDecoder extends BaseProtocolDecoder {

    public TrakInfiProtocolDecoder(TrakInfiProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*SP")
            .number("(d),")                         // COMMAND_CODE
            .number("(d+),")                        // imei
            .number("(dd)")                         // date in DD , GMT 0
            .number("(dd)")                         // date in MM , GMT 0
            .number("(dd),")                        // date in YY , GMT 0
            .number("(dd)")                         // time in HH
            .number("(dd)")                         // time in MM
            .number("(dd),")                        // time in SS
            .expression("([AV]),")                  // (A for GPS Fix, V GPS not fixed)
            .groupBegin()
            .number("(d).(d),")                     // latitude (0.0)
            .or()
            .number("(d+)(dd.d+),")                 // latitude (ddmm.mmmm)
            .groupEnd()
            .expression("([NSEW]),").optional()
            .groupBegin()
            .number("(d).(d),")                     // longitude (0.0)
            .or()
            .number("(d+)(dd.d+),")                 // longitude (dddmm.mmmm)
            .groupEnd()
            .expression("([EW]),").optional()
            .number("(d+.?d*)?,")                   // speed
            .number("(d+.?d*)?,?")                  // course
            .number("(d+),")                        // Number Of Satellites
            .number("(d+),")                        // GSM Signal Strength
            .expression("([ANS]),")                 // Vehicle Status
            .number("(d),")                         // Mains Power Status
            .expression("([^,]+),")                 // battery in volts
            .number("(d),")                         // SOS Alert (Panic)
            .number("(d),")                         // Body Tamper
            .number("(d)")                          // GPI1 (AC Status)
            .number("(d),")                         // GPI2 (Ignition Status)
            .number("(d),")                         // GPO1 Status
            .expression("([LR])")                   // L- live R- Recorded
            .number("([0-9a-fA-F,]*)#").optional()  // end
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (!sentence.isEmpty()) {
            if (channel != null) {
                //Need to check if anything needs to send back.
                channel.write("+##Received OK", remoteAddress);
            }
        }

        Position position = new Position(getProtocolName());

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        int commandCode = parser.nextInt();
        switch (commandCode) {
            case 9:
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            default:
                break;
        }
        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        int localHours = parser.nextInt(0);
        int localMinutes = parser.nextInt(0);
        int localSeconds = parser.nextInt(0);

        dateBuilder.setTime(localHours, localMinutes, localSeconds);
        position.setTime(dateBuilder.getDate());

        String gpsFix = parser.next();

        if (parser.hasNext(2)) { //for 0.0 lat
            parser.skip(3);
        }
        if (parser.hasNext(2)) {
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_HEM));
        }
        if (parser.hasNext(2)) { //for 0.0 lng
            parser.skip(3);
        }
        if (parser.hasNext(2)) {
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_HEM));
        }

        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));
        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.set(Position.KEY_RSSI, parser.nextInt(0));

        position.set(Position.KEY_STATUS, parser.next()); // vehicleStatus

        if (parser.nextInt(0) == 0) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
            position.set(Position.KEY_CHARGE, false);
        } else {
            position.set(Position.KEY_CHARGE, true);
        }

        position.set(Position.KEY_BATTERY, parser.next());

        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
        }
        if (parser.nextInt(0) == 1) {
            position.set(Position.KEY_IGNITION, true);
        } else {
            position.set(Position.KEY_IGNITION, false);
        }

        position.set(Position.KEY_INPUT, parser.nextInt()); //Need to check and update
        position.set(Position.KEY_TYPE, parser.next()); // messageType

        if (gpsFix.equals("A")) {
            position.set(Position.KEY_GPS, true);
            position.setValid(true);
        } else {
            getLastLocation(position, null);
        }

        return position;
    }

}
