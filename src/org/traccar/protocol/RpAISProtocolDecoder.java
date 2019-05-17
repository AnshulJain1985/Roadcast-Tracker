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
import org.traccar.helper.UnitsConverter;
//import org.traccar.helper.Log;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class RpAISProtocolDecoder extends BaseProtocolDecoder {

    public RpAISProtocolDecoder(RpAISProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_LOGIN = new PatternBuilder()
            .text("$LGN,")
            .expression("([^,]+)?,")                // vehicle reg no
            .expression("([0-9]+),")                // IMEI
            .expression("([^,]+)?,")                // Software version
            .number("(-?d+.d+),")                   // latitude
            .expression("([NS]),")
            .number("(-?d+.d+),")                   // longitude
            .expression("([EW])")
            .text("*")
            .compile();

//    $HLM,ROADRPA,1.9AIS,869867036058354,98,20,0,60,60,0000,11*
    private static final Pattern PATTERN_HEARTBEAT = new PatternBuilder()
            .text("$HLM,")
            .expression("([A-Z]+),")                // Vendor Id
            .expression("([^,]+)?,")                // Software version
            .expression("([0-9]+),")                // IMEI
            .number("(d+),")                        // battery percentage
            .number("(d+),")                        // low battery threshold
            .number("(d+),")                        // memory percentage
            .number("(d+),")                        // ignition on timer
            .number("(d+),")                        // ignition off timer
            .number("(d)(d)(d)(d),")                // digital Input 4
            .number("(d)(d)")                      // digital Output 2
            .text("*")
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .expression("(NRM|ALT),")
            .expression("([A-Z]+),")                // Vendor Id
            .expression("([^,]+)?,")                // Software version
            .expression("([A-Z]+),")                // Packet Type
            .number("(dd),")                        // Alert ID
            .expression("([HL]),")                    // Packet Status
            .expression("([0-9]+),")                // IMEI
            .expression("([^,]+)?,")                // vehicle reg no
            .number("(d+),")                        // GPS Fix
            .number("(dd)(dd)(dddd),")                // date utc (DDMMYYYY)
            .number("(dd)(dd)(dd),")                // time utc (hhmmss)
            .number("(-?d+.d+),")                   // latitude
            .expression("([NS]),")
            .number("(-?d+.d+),")                   // longitude
            .expression("([EW]),")
            .number("(d+.d+),")                     // speed
            .number("(d+.d+),")                    // course
            .number("(d+),")                        // No of satellites
            .number("(d+.?d*)?,?")                  // altitude
            .number("(d+.d+),")                     // pdop
            .number("(d+.d+),")                     // hdop
            .expression("([^,]+)?,")                // Operator Name
            .number("(d),")                         // Ignition
            .number("(d),")                         // Main power status
            .number("(d+.?d*)?,?")                  // Main input voltage
            .number("(d+.?d*)?,?")                  // internal battery voltage
            .number("(d),")                         // Emergency Status
            .expression("([OC]),")                  // Temper alert
            .number("(d+),")                        // GSM signal strength
            .expression("([^,]+)?,")                // MCC
            .expression("([^,]+)?,")                // MNC
            .expression("([^,]+)?,")                // LAC
            .expression("([^,]+)?,")                // CELLID
            .number("(d+),")                        // NMR1 signal strength
            .expression("([^,]+)?,")                // NMR1 CellID
            .expression("([^,]+)?,")                // NMR1 LAC
            .number("(d+),")                        // NMR2 signal strength
            .expression("([^,]+)?,")                // NMR2 CellID
            .expression("([^,]+)?,")                // NMR2 LAC
            .number("(d+),")                        // NMR3 signal strength
            .expression("([^,]+)?,")                // NMR3 CellID
            .expression("([^,]+)?,")                // NMR3 LAC
            .number("(d+),")                        // NMR4 signal strength
            .expression("([^,]+)?,")                // NMR4 CellID
            .expression("([^,]+)?,")                // NMR4 LAC
            .number("(d)(d)(d)(d),")                // digital Input 4
            .number("(d)(d),")                      // digital Output 2
            .number("(d+.?d*)?,")                  // Analog input 1
            .number("(d+.?d*)?,")                  // Analog input 2
            .number("(d+),")                        // Frame number
            .number("(d+.?d*)?,")                  // Odometer
            .expression("[^,]*,")
            .expression("[^,]*,")
            .expression("[^,]*,")
            .expression("[^,]*,")
            .expression("[^,]*,")
            .number("(xxxxxxxx)")                   // checksum
            .text("*")
            .any()
            .compile();

    private static final Pattern PATTERN_EMERGENCY = new PatternBuilder()
            .text("$EPB,")
            .expression("([A-Z]+),")                // Packet Type
            .expression("([0-9]+),")                // IMEI
            .expression("([A-Z]+),")                    // Packet Status
            .number("(dd)(dd)(dddd),")                // date utc (DDMMYYYY)
            .number("(dd)(dd)(dd),")                // time utc (hhmmss)
            .number("([VA]),")                        // GPS Fix
            .number("(-?d+.d+),")                   // latitude
            .expression("([NS]),")
            .number("(-?d+.d+),")                   // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,?")                  // altitude
            .number("(d+.d+),")                     // speed
            .number("(d+.d+),")                     // distance
            .expression("([GN]),")                  // Provider
            .expression("([^,]+)?,")                // vehicle reg no
            .expression("([^,]+)?,")                // reply number
            .expression("([^,]+)?,")                // MCC
            .expression("([^,]+)?,")                // MNC
            .expression("([^,]+)?,")                // LAC
            .expression("([A-Z0-9]+)")                // CELLID
            .text("*")
            .number("(xxxxxxxx)")                   // checksum
            .compile();


    private String decodeAlarm(String value) {

//        NR: Normal periodic packet
//        HP: Health packet
//        TA: Tamper alert
//        EA: Emergency alert
//        IN: Ignition On alert
//        IF: Ignition OFF alert
//        BR: Mains reconnected alert
//        BD: Mains disconnected alert
//        BL: Low battery alert
//        BH: Low battery charged alert
//        CC: Configuration over the air alert
//        HA: Harsh acceleration alert
//        HB: Harsh braking alert
//        RT: Harsh/Rash turning alert
//        OS: Over Speed Alert

        switch (value) {
            case "NR":
            case "HP":
            case "IN":
            case "IF":
            case "BH":
            case "CC":
                return null;
            case "TA":
                return Position.ALARM_TAMPERING;
            case "EA":
                return Position.ALARM_SOS;
            case "BR":
                return Position.ALARM_POWER_RESTORED;
            case "BD":
                return Position.ALARM_POWER_CUT;
            case "BL":
                return Position.ALARM_LOW_BATTERY;
            case "HA":
                return Position.ALARM_ACCELERATION;
            case "HB":
                return Position.ALARM_BRAKING;
            case "RT":
                return Position.ALARM_CORNERING;
            case "OS":
                return Position.ALARM_OVERSPEED;
            default:
                return null;
        }
    }

    public void getLastLocation(Position position, Date deviceTime) {
        if (position.getDeviceId() != 0) {
            position.setOutdated(true);

            Position last = Context.getIdentityManager().getLastPosition(position.getDeviceId());
            if (last != null) {
                position.setFixTime(last.getFixTime());
                position.setValid(last.getValid());
                position.setLatitude(last.getLatitude());
                position.setLongitude(last.getLongitude());
                position.setAltitude(last.getAltitude());
                position.setSpeed(last.getSpeed());
                position.setCourse(last.getCourse());
                position.setAccuracy(last.getAccuracy());
            } else {
                position.setFixTime(new Date(0));
            }

            if (deviceTime != null) {
                position.setDeviceTime(deviceTime);
            } else {
                position.setDeviceTime(new Date());
            }
        }
    }

    private Object decodeLogin(Position position, Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_LOGIN, sentence);
        if (!parser.matches()) {
            return null;
        }
        String deviceName = parser.next();
        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_VERSION_FW, parser.next());
        position.setTime(new Date());
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        return position;
    }

    private Object decodeHeartbeat(Position position, Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_HEARTBEAT, sentence);
        if (!parser.matches()) {
            return null;
        }
        position.set(Position.KEY_VERSION_HW, parser.next());
        position.set(Position.KEY_VERSION_FW, parser.next());
        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt(0));

        getLastLocation(position, null);

        int lowBatThreshold = parser.nextInt();
        int memPercentage = parser.nextInt();
        int intervalIgnitionOn = parser.nextInt();
        int intervalIngitionOff = parser.nextInt();

        for (int i = 1; i <= 4; i++) {
            int tempDio = parser.nextInt(0);
            position.set(Position.PREFIX_IN + i, tempDio);
            if (i == 2) {
                position.set(Position.KEY_DOOR, tempDio == 1);
            }
        }
        for (int i = 1; i <= 2; i++) {
            position.set(Position.PREFIX_OUT + i, parser.nextInt(0));
        }
        return position;
    }

    private Object decodeEmergency(Position position, Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_EMERGENCY, sentence);
        if (!parser.matches()) {
            return null;
        }
        String packetType = parser.next();
        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        if (packetType.equals("EMR")) {
            if (channel != null) {
                channel.write("SET EO");
            }
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        position.setDeviceId(deviceSession.getDeviceId());
        String packetStatus = parser.next();
        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setAltitude(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));
        position.set(Position.KEY_DISTANCE, parser.nextDouble(0));
        String provider = parser.next();
        String deviceName = parser.next();
        String replyNumber = parser.next();

        Network network = new Network();
        int mcc = parser.nextInt(0);
        int mnc = parser.nextInt(0);
        int lac = parser.nextHexInt();
        int cellId = parser.nextHexInt();

        network.addCellTower(CellTower.from(mcc, mnc, lac, cellId));

        String checksum = parser.next();
        return position;
    }

    private Object decodeNormal(Position position, Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }
        String header = parser.next();
        position.set(Position.KEY_VERSION_HW, parser.next());
        position.set(Position.KEY_VERSION_FW, parser.next());
        String packetType = parser.next();
        int alertId = parser.nextInt(0);
        String packetStatus = parser.next();
        String imei = parser.next();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_ALARM, decodeAlarm(packetType));
        position.set(Position.KEY_ORIGINAL, parser.next());
        position.setValid(parser.nextInt(0) == 1);
        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));
        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.setAltitude(parser.nextDouble(0));
        position.set(Position.KEY_PDOP, parser.nextDouble(0));
        position.set(Position.KEY_HDOP, parser.nextDouble(0));
        position.set(Position.KEY_OPERATOR, parser.next());
        position.set(Position.KEY_IGNITION, parser.nextInt(0) == 1);
        position.set(Position.KEY_CHARGE, parser.nextInt(0) == 1);
        position.set("maininput", parser.nextDouble(0));
        position.set(Position.KEY_BATTERY, parser.nextDouble(0));
        position.set(Position.KEY_STATUS, parser.nextInt(0));

        String temperAlert = parser.next();
        position.set(Position.KEY_RSSI, parser.nextInt(0));
        Network network = new Network();
        int mcc = parser.nextHexInt();
        int mnc = parser.nextHexInt();
        int lac = parser.nextHexInt();
        int cellId = parser.nextHexInt();

        for (int i = 0; i < 4; i++) {
            int cellIdN = parser.nextHexInt();
            int lacN = parser.nextHexInt();
            int rssiN = parser.nextHexInt();
            network.addCellTower(CellTower.from(mcc, mnc, lacN, cellIdN, rssiN));
        }

        for (int i = 1; i <= 4; i++) {
            int tempDio = parser.nextInt(0);
            position.set(Position.PREFIX_IN + i, tempDio);
        }
        for (int i = 1; i <= 2; i++) {
            position.set(Position.PREFIX_OUT + i, parser.nextInt(0));
        }
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble(0));
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble(0));
//        int frameNumber = parser.nextInt(0);
//        String checksum = parser.next();

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        Position position = new Position(getProtocolName());
        String header = sentence.substring(1, 4);

//        Log.info(String.format("%08X", channel.getId()) + " - RPAIS HEX: " + sentence);

        if (header.equals("LGN")) {
            //Login
            return decodeLogin(position, channel, remoteAddress, sentence);

        } else if (header.equals("HLM")) {
            //heartbeat
            return decodeHeartbeat(position, channel, remoteAddress, sentence);

        } else if (header.equals("EPB")) {
            //emergency
            return decodeEmergency(position, channel, remoteAddress, sentence);

        } else {
            //normal and other packets
            return decodeNormal(position, channel, remoteAddress, sentence);
        }
    }

}
