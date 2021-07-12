/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class Tk103BmsProtocolDecoder extends BaseProtocolDecoder {

    private static final String KEY_BMS_CHARGE_PROTECTION_VOLTAGE = "bCPV";
    private static final String KEY_BMS_DISCHARGE_PROTECTION_VOLTAGE = "bDPV";
    private static final String KEY_BMS_SEC_CHARGE_PROTECTION_VOLTAGE = "bSCPV";
    private static final String KEY_BMS_SEC_DISCHARGE_PROTECTION_VOLTAGE = "bSDPV";
    private static final String KEY_BMS_CHARGE_RECOVERING_VOLTAGE = "bCRV";
    private static final String KEY_BMS_DISCHARGE_RECOVERING_VOLTAGE = "bDRV";
    private static final String KEY_BMS_BALANCED_STARTING_VOLTAGE = "bBSV";
    private static final String KEY_BMS_DISCHARGE_OVERCURRENT_PROTECTION = "bDOCP";
    private static final String KEY_BMS_PEAK_DISCHARGE_PROTECTION = "bPDP";
    private static final String KEY_BMS_DISCHARGE_PROTECTION_DELAY = "bDPD";
    private static final String KEY_BMS_CHARGING_CURRENT_PROTECTION = "bCCP";
    private static final String KEY_BMS_CHARGE_BALANCE_RATION = "bCBR";
    private static final String KEY_BMS_EQUILIZATION_ACCURACY_MV = "bEQA";
    private static final String KEY_BMS_MOS_TEMP_PROTECTION = "bMOSTP";
    private static final String KEY_BMS_BALANCED_TEMP_PROTECTION = "bBALTP";
    private static final String KEY_BMS_BATTERY_TEMP_PROTECTION = "bBTP";
    private static final String KEY_BMS_BATTERY_TEMP_RECOVERY = "bBTR";
    private static final String KEY_BMS_BATTERY_STRING_SETTING = "bBSS";
    private static final String KEY_BMS_BATTERY_CAPACITY_SETTING = "bBCS";
    private static final String KEY_BMS_BALANCE_SWITCH = "bBS";
    private static final String KEY_BMS_CHARGE_SWITCH = "bCS";
    private static final String KEY_BMS_DISCHARGE_SWITCH = "bDS";
    private static final String KEY_BMS_DEDICATED_CHARGE_SWITCH = "bDCS";
    private static final String KEY_BMS_REMAINING_CAPACITY_ALARM_VAL = "bRCAV";
    private final boolean decodeLow;

    private static final String KEY_BMS_NO_OF_BATTERIES = "bNB";
    private static final String KEY_BMS_BATTERY_VOLTAGE_PREFIX = "bBV_";
    private static final String KEY_BMS_WORKING_STATUS = "bWS";
    private static final String KEY_BMS_CURRENT = "bCur";
    private static final String KEY_BMS_TOTAL_VOLTAGE = "bTV";
    private static final String KEY_BMS_REMAINING_CAPACITY = "bRC";
    private static final String KEY_BMS_BATTERY_OVER_TEMP = "bBOT";
    private static final String KEY_BMS_CHARGE_PROTECTION = "bCP";
    private static final String KEY_BMS_DISCHARGE_PROTECTION = "bDP";
    private static final String KEY_BMS_BATTERY_DROPPED = "bDrop";
    private static final String KEY_BMS_BALANCED = "bBal";
    private static final String KEY_BMS_CYCLES = "bCyc";
    private static final String ALARM_BMS_CELL_OVER_VOLTAGE = "bCOV";
    private static final String ALARM_BMS_TEMP_DETECT_LINE_OPEN = "bTDLO";
    private static final String ALARM_BMS_CELL_DETECT_LINE_OPEN = "bCDLO";
    private static final String ALARM_BMS_SHORT_CIRCUIT = "bSC";
    private static final String ALARM_BMS_DISCHARGE_OVER_CURRENT = "bDOC";
    private static final String ALARM_BMS_CHARGE_OVER_CURRENT = "bCOC";
    private static final String ALARM_BMS_MOS_OVER_TEMP = "bMOSOT";
    private static final String ALARM_BMS_LOW_CAPACITY = "bLC";
    private static final String ALARM_BMS_CELL_TEMP_DIFFERENCE_EXCEED_DISCHARGE_TEMP = "bCTDEDT";
    private static final String ALARM_BMS_CELL_TEMP_DIFFERENCE_EXCEED_CHARGE_TEMP = "bCTDECT";
    private static final String ALARM_BMS_CELL_TEMP_LOWER_DISCHARGE_TEMP = "bLDT";
    private static final String ALARM_BMS_CELL_TEMP_LOWER_CHARGE_TEMP = "bLCT";
    private static final String ALARM_BMS_CELL_TEMP_EXCEED_DISCHARGE_TEMP = "bTEDT";
    private static final String ALARM_BMS_CELL_TEMP_EXCEED_CHARGE_TEMP = "bTECT";
    private static final String ALARM_BMS_PRESSURE_LOW = "bPL";
    private static final String ALARM_BMS_PRESSURE_HIGH = "bPH";
    private static final String ALARM_BMS_BATTERY_UNDER_VOLTAGE = "bBUV";
    private static final String KEY_BMS_CHARGING = "bCharge";
    private static final String KEY_BMS_DISCHARGING = "bDischarge";
    private static final String KEY_BMS_TEMP_QUANTITY = "bTQ";
    private static final String KEY_BMS_POWER_BOARD_TEMP = "bPBT";
    private static final String KEY_BMS_BALANCE_PLATE_TEMP = "bBPT";
    private static final String KEY_BMS_BATTERY_TEMP_PREFIX = "bBTP_";
    private static final String KEY_BMS_CALIBRATION_CAPACITY = "bCC";
    private static final String KEY_BMS_TOTAL_DISCHARGE_CAPACITY = "bTDC";
    private static final String KEY_BMS_PROTECTION_BOARD_INFO = "bPBI";

    public Tk103BmsProtocolDecoder(Protocol protocol) {
        super(protocol);
        decodeLow = Context.getConfig().getBoolean(getProtocolName() + ".decodeLow");
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(").optional()
            .number("(d+)(,)?")                  // device id
            .expression("(.{4}),?")              // command
            .number("(d*)")
            .number("(dd)(dd)(dd),?")            // date (mmddyy if comma-delimited, otherwise yyddmm)
            .expression("([AV]),?")              // validity
            .number("(d+)(dd.d+)")               // latitude
            .expression("([NS]),?")
            .number("(d+)(dd.d+)")               // longitude
            .expression("([EW]),?")
            .number("(d+.d)(?:d*,)?")            // speed
            .number("(dd)(dd)(dd),?")            // time (hhmmss)
            .groupBegin()
            .number("(?:([d.]{6})|(dd)),?")      // course
            .number("([01])")                    // charge
            .number("([01])")                    // ignition
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(xxx)")                     // fuel
            .number("L(x+)")                     // odometer
            .or()
            .number("(d+.d+)")                   // course
            .groupEnd()
            .any()
            .number("([+-]ddd.d)?")              // temperature
            .text(")").optional()
            .compile();

    private static final Pattern PATTERN_BATTERY = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .text("ZC20,")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+),")                     // battery level
            .number("(d+),")                     // battery voltage
            .number("(d+),")                     // power voltage
            .number("d+")                        // installed
            .any()
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .text("(").optional()
            .number("(d{12})")                   // device id
            .text("BZ00,")
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+),")                     // cid
            .any()
            .compile();

    private static final Pattern PATTERN_LBSWIFI = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .expression("(.{4}),")               // command
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(d+),")                     // cid
            .number("(d+),")                     // number of wifi macs
            .number("((?:(?:xx:){5}(?:xx)\\*[-+]?d+\\*d+,)*)")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .any()
            .compile();

    private static final Pattern PATTERN_COMMAND_RESULT = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .expression(".{4},")                 // command
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("\\$([\\s\\S]*?)(?:\\$|$)") // message
            .any()
            .compile();

    private static final Pattern PATTERN_BMS_FF = new PatternBuilder()
            .text("(").optional()
            .number("(d+)")                   // device id
            .text("BS50")
            .number("(x{4})")                     // content length
            .number("(xx)")                       // start bit FF
            .number("(xx)")                       // Number of batteries
            .number("(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})")  // 12 batteries voltage
            .number("(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})(x{4})")  // 12 batteries voltage
            .number("(x{2})")                       // working status
            .number("(x{4})")                     // current amp
            .number("(x{4})")                     // total voltage
            .number("(xx)")                       // remaining capacity percentage
            .number("(xx)")                       // battery over temp 0 normal
            .number("(xx)")                       // charging protection
            .number("(xx)")                       // discharge protection
            .number("(xx)")                       // battery dropped
            .number("(xx)")                       // balance opening and closing
            .number("(x{4})")                     // cycles
            .number("(xx)")                       // alarms
            .number("(xx)")                       // alarms 2
            .number("(xx)")                       // b0 battery undervoltage
            .number("(xx)")                       // 1 to indicate that the charging current is detected.
            .number("(xx)")                       // 1 to indicate that the effective discharge current is detected
            .number("x{4}")                     // skip
            .number("(xx)")                       // temperature quantity
            .number("(xx)")                       // power board temp Offset 40 degrees 40 = 0 degrees 50 = 10 35 = -5
            .number("(xx)")                       // balance plate temperature
            .number("(xx)(xx)(xx)(xx)(xx)(xx)(xx)")                    // balance plate temperature
            .number("(x{4})")                     // Calibration capacity 0.01
            .number("(x{8})")                     // Total discharge capacity
            .number("x{8}")                    // skip
            .number("(x{16})")                    // Protection board factory information
            .any()
            .text(")").optional()
            .compile();

    private static final Pattern PATTERN_BMS_FA = new PatternBuilder()
            .text("(").optional()
            .number("(d+)")                   // device id
            .text("BS50")
            .number("(x{4})")                     // content length
            .number("(xx)")                       // start bit FA
            .number("(xx)")                       // Number of data
            .number("(x{4})")                     // Charge protection voltage
            .number("(x{4})")                     // Discharge protection voltage
            .number("(x{4})")                     // Secondary charge protection voltage
            .number("(x{4})")                     // Secondary discharge protection value
            .number("(x{4})")                     // Charging recovery voltage
            .number("(x{4})")                     // Discharge recovery voltage
            .number("(x{4})")                     // Balanced starting voltage
            .number("(x{4})")                     // Discharge overcurrent protection
            .number("(x{4})")                     // Peak discharge protection
            .number("(xx)")                       // Discharge protection delay
            .number("(xx)")                       // Charging current protection
            .number("(xx)")                       // Charge balance ratio (10%-100%)
            .number("(xx)")                       // Equalization accuracy Mv
            .number("(xx)")                       // MOS tube temperature protection value
            .number("(xx)")                       // Balanced temperature protection value
            .number("(xx)")                       // Battery temperature protection value
            .number("(xx)")                       // Battery temperature recovery value
            .number("x{4}")                       // unused
            .number("(xx)")                       // Battery string setting
            .number("(x{4})")                     // Battery capacity setting
            .number("(xx)")                       // Balance switch
            .number("xx")                         // unused
            .number("(xx)")                       // charging switch
            .number("(xx)")                       // discharging switch
            .number("(xx)")                       // Dedicated charger switch 1 = use dedicated charger
            .number("(xx)")                       // Remaining capacity alarm value
            .any()
            .text(")").optional()
            .compile();

    private String decodeAlarm(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_ACCIDENT;
            case 2:
                return Position.ALARM_SOS;
            case 3:
                return Position.ALARM_VIBRATION;
            case 4:
                return Position.ALARM_LOW_SPEED;
            case 5:
                return Position.ALARM_OVERSPEED;
            case 6:
                return Position.ALARM_GEOFENCE_EXIT;
            default:
                return null;
        }
    }

    private void decodeType(Position position, String type, String data) {
        switch (type) {
            case "BO01":
                position.set(Position.KEY_ALARM, decodeAlarm(data.charAt(0) - '0'));
                break;
            case "ZC11":
            case "DW31":
            case "DW51":
                position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                break;
            case "ZC12":
            case "DW32":
            case "DW52":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case "ZC13":
            case "DW33":
            case "DW53":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case "ZC15":
            case "DW35":
            case "DW55":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "ZC16":
            case "DW36":
            case "DW56":
                position.set(Position.KEY_IGNITION, false);
                break;
            case "ZC29":
            case "DW42":
            case "DW62":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "ZC17":
            case "DW37":
            case "DW57":
                position.set(Position.KEY_ALARM, Position.ALARM_REMOVING);
                break;
            case "ZC25":
            case "DW3E":
            case "DW5E":
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            case "ZC26":
            case "DW3F":
            case "DW5F":
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                break;
            case "ZC27":
            case "DW40":
            case "DW60":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                break;
            default:
                break;
        }
    }

    private Integer decodeBattery(int value) {
        switch (value) {
            case 6:
                return 100;
            case 5:
                return 80;
            case 4:
                return 50;
            case 3:
                return 20;
            case 2:
                return 10;
            default:
                return null;
        }
    }

    private Position decodeBattery(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_BATTERY, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        int batterylevel = parser.nextInt(0);
        if (batterylevel != 255) {
            position.set(Position.KEY_BATTERY_LEVEL, decodeBattery(batterylevel));
        }

        int battery = parser.nextInt(0);
        if (battery != 65535) {
            position.set(Position.KEY_BATTERY, battery * 0.01);
        }

        int power = parser.nextInt(0);
        if (power != 65535) {
            position.set(Position.KEY_POWER, power * 0.1);
        }

        return position;
    }

    private Position decodeNetwork(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_NETWORK, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(0), parser.nextInt(0), parser.nextHexInt(0), parser.nextHexInt(0))));

        return position;
    }

    private Position decodeLbsWifi(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_LBSWIFI, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodeType(position, parser.next(), "0");

        getLastLocation(position, null);

        Network network = new Network();

        network.addCellTower(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt()));

        int wifiCount = parser.nextInt();
        if (parser.hasNext()) {
            String[] wifimacs = parser.next().split(",");
            if (wifimacs.length == wifiCount) {
                for (int i = 0; i < wifiCount; i++) {
                    String[] wifiinfo = wifimacs[i].split("\\*");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            wifiinfo[0], Integer.parseInt(wifiinfo[1]), Integer.parseInt(wifiinfo[2])));
                }
            }
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        return position;
    }

    private Position decodeCommandResult(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_COMMAND_RESULT, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.set(Position.KEY_RESULT, parser.next());

        return position;

    }

@Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (channel != null) {
            String id = sentence.substring(1, 13);
            String type = sentence.substring(13, 17);
            if (type.equals("BP00")) {
                channel.writeAndFlush(new NetworkMessage("(" + id + "AP01HSO)", remoteAddress));
                return null;
            } else if (type.equals("BP05")) {
                channel.writeAndFlush(new NetworkMessage("(" + id + "AP05)", remoteAddress));
            }
        }

        if (sentence.contains("ZC20")) {
            return decodeBattery(channel, remoteAddress, sentence);
        } else if (sentence.contains("BZ00")) {
            return decodeNetwork(channel, remoteAddress, sentence);
        } else if (sentence.contains("ZC03")) {
            return decodeCommandResult(channel, remoteAddress, sentence);
        } else if (sentence.contains("DW5")) {
            return decodeLbsWifi(channel, remoteAddress, sentence);
        } else if (sentence.contains("BS50")) {
            return decodeBMS(channel, remoteAddress, sentence);
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        boolean alternative = parser.next() != null;

        decodeType(position, parser.next(), parser.next());

        DateBuilder dateBuilder = new DateBuilder();
        if (alternative) {
            dateBuilder.setDateReverse(parser.nextInt(0),
                    parser.nextInt(0), parser.nextInt(0));
        } else {
            dateBuilder.setDate(parser.nextInt(0), parser.nextInt(0),
                    parser.nextInt(0));
        }

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.setSpeed(convertSpeed(parser.nextDouble(0), "kmh"));

        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }
        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }

        if (parser.hasNext(7)) {
            position.set(Position.KEY_CHARGE, parser.nextInt() == 0);
            position.set(Position.KEY_IGNITION, parser.nextInt() == 1);

            int mask1 = parser.nextHexInt();
            position.set(Position.PREFIX_IN + 2, BitUtil.check(mask1, 0) ? 1 : 0);
            position.set("panic", BitUtil.check(mask1, 1) ? 1 : 0);
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(mask1, 2) ? 1 : 0);
            if (decodeLow || BitUtil.check(mask1, 3)) {
                position.set(Position.KEY_BLOCKED, BitUtil.check(mask1, 3) ? 1 : 0);
            }

            position.set(Position.KEY_DOOR, mask1 == 1);

            int mask2 = parser.nextHexInt();
            for (int i = 0; i < 3; i++) {
                if (decodeLow || BitUtil.check(mask2, i)) {
                    position.set("hs" + (3 - i), BitUtil.check(mask2, i) ? 1 : 0);
                }
            }
//            if (decodeLow || BitUtil.check(mask2, 3)) {
//                position.set(Position.KEY_DOOR, BitUtil.check(mask2, 3) ? 1 : 0);
//            }

            int mask3 = parser.nextHexInt();
            for (int i = 1; i <= 3; i++) {
                if (decodeLow || BitUtil.check(mask3, i)) {
                    position.set("ls" + (3 - i + 1), BitUtil.check(mask3, i) ? 1 : 0);
                }
            }

            position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
            position.set(Position.KEY_ODOMETER, parser.nextLong(16, 0));
        }

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }

        if (parser.hasNext()) {
            position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
        }

        return position;
    }

    private Position decodeBMS(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_BMS_FF, sentence);
        if (parser.matches()) {
            return decodeBMSFF(channel, remoteAddress, parser);
        }

        parser = new Parser(PATTERN_BMS_FA, sentence);
        if (parser.matches()) {
            return decodeBMSFA(channel, remoteAddress, parser);
        }
        return null;
    }

    private Position decodeBMSFA(Channel channel, SocketAddress remoteAddress, Parser parser) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        DecimalFormat df = new DecimalFormat("#.###");
        int contentLength = parser.nextHexInt();
        int startByte = parser.nextHexInt();

        position.set("bms", true);

        position.set(KEY_BMS_CHARGE_PROTECTION_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_DISCHARGE_PROTECTION_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_SEC_CHARGE_PROTECTION_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_SEC_DISCHARGE_PROTECTION_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_CHARGE_RECOVERING_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_DISCHARGE_RECOVERING_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_BALANCED_STARTING_VOLTAGE, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_DISCHARGE_OVERCURRENT_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_PEAK_DISCHARGE_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_DISCHARGE_PROTECTION_DELAY, parser.nextHexInt());
        position.set(KEY_BMS_CHARGING_CURRENT_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_CHARGE_BALANCE_RATION, parser.nextHexInt());
        position.set(KEY_BMS_EQUILIZATION_ACCURACY_MV, parser.nextHexInt());
        position.set(KEY_BMS_MOS_TEMP_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_BALANCED_TEMP_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_TEMP_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_TEMP_RECOVERY, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_STRING_SETTING, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_CAPACITY_SETTING, parser.nextHexInt());
        position.set(KEY_BMS_BALANCE_SWITCH, parser.nextHexInt());
        position.set(KEY_BMS_CHARGE_SWITCH, parser.nextHexInt());
        position.set(KEY_BMS_DISCHARGE_SWITCH, parser.nextHexInt());
        position.set(KEY_BMS_DEDICATED_CHARGE_SWITCH, parser.nextHexInt());
        position.set(KEY_BMS_REMAINING_CAPACITY_ALARM_VAL, parser.nextHexInt());

        return position;
    }

    private Position decodeBMSFF(Channel channel, SocketAddress remoteAddress, Parser parser) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        DecimalFormat df = new DecimalFormat("#.###");
        int contentLength = parser.nextHexInt();
        int startByte = parser.nextHexInt();

        position.set(KEY_BMS_NO_OF_BATTERIES, parser.nextHexInt());

        for (int i = 1; i <= 24; i++) {
            double batteryVoltage = (parser.toLittleEndian(parser.next()) / 1000.0);
            if (batteryVoltage > 0) {
                position.set(KEY_BMS_BATTERY_VOLTAGE_PREFIX + i, df.format(batteryVoltage));
            }
        }
        position.set(KEY_BMS_WORKING_STATUS, parser.nextHexInt());
        position.set(KEY_BMS_CURRENT, parser.nextHexInt());
        position.set(KEY_BMS_TOTAL_VOLTAGE, df.format(parser.nextHexInt() / 1000.0));
        position.set(KEY_BMS_REMAINING_CAPACITY, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_OVER_TEMP, parser.nextHexInt());
        position.set(KEY_BMS_CHARGE_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_DISCHARGE_PROTECTION, parser.nextHexInt());
        position.set(KEY_BMS_BATTERY_DROPPED, parser.nextHexInt());
        position.set(KEY_BMS_BALANCED, parser.nextHexInt());
        position.set(KEY_BMS_CYCLES, parser.toLittleEndian(parser.next()));

        decodeBMSAlarm1(position, parser.nextHexInt(0));
        decodeBMSAlarm2(position, parser.nextHexInt(0));

        position.set(ALARM_BMS_BATTERY_UNDER_VOLTAGE, BitUtil.check(parser.nextHexInt(), 0) ? 1 : 0);

        position.set(KEY_BMS_CHARGING, parser.nextHexInt());
        position.set(KEY_BMS_DISCHARGING, parser.nextHexInt());
        position.set(KEY_BMS_TEMP_QUANTITY, parser.nextHexInt());
        position.set(KEY_BMS_POWER_BOARD_TEMP, parser.nextHexInt() - 40);
        position.set(KEY_BMS_BALANCE_PLATE_TEMP, parser.nextHexInt() - 40);

        for (int i = 1; i <= 7; i++) {
            int temp = parser.nextHexInt();
            if (temp > 0) {
                position.set(KEY_BMS_BATTERY_TEMP_PREFIX + i, temp - 40);
            }
        }

        position.set(KEY_BMS_CALIBRATION_CAPACITY, parser.toLittleEndian(parser.next()));
        position.set(KEY_BMS_TOTAL_DISCHARGE_CAPACITY, parser.nextHexLong());
        position.set(KEY_BMS_PROTECTION_BOARD_INFO, parser.next());

        return position;
    }

    private void decodeBMSAlarm1(Position position, int mask) {
        if (BitUtil.check(mask, 0)) {
            position.set(ALARM_BMS_CELL_OVER_VOLTAGE, 1);
        }
        if (BitUtil.check(mask, 1)) {
            position.set(ALARM_BMS_TEMP_DETECT_LINE_OPEN, 1);
        }
        if (BitUtil.check(mask, 2)) {
            position.set(ALARM_BMS_CELL_DETECT_LINE_OPEN, 1);
        }
        if (BitUtil.check(mask, 3)) {
            position.set(ALARM_BMS_SHORT_CIRCUIT, 1);
        }
        if (BitUtil.check(mask, 4)) {
            position.set(ALARM_BMS_DISCHARGE_OVER_CURRENT, 1);
        }
        if (BitUtil.check(mask, 5)) {
            position.set(ALARM_BMS_CHARGE_OVER_CURRENT, 1);
        }
        if (BitUtil.check(mask, 6)) {
            position.set(ALARM_BMS_MOS_OVER_TEMP, 1);
        }
        if (BitUtil.check(mask, 7)) {
            position.set(ALARM_BMS_LOW_CAPACITY, 1);
        }
    }

    private void decodeBMSAlarm2(Position position, int mask) {
        if (BitUtil.check(mask, 0)) {
            position.set(ALARM_BMS_CELL_TEMP_DIFFERENCE_EXCEED_DISCHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 1)) {
            position.set(ALARM_BMS_CELL_TEMP_DIFFERENCE_EXCEED_CHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 2)) {
            position.set(ALARM_BMS_CELL_TEMP_LOWER_DISCHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 3)) {
            position.set(ALARM_BMS_CELL_TEMP_LOWER_CHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 4)) {
            position.set(ALARM_BMS_CELL_TEMP_EXCEED_DISCHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 5)) {
            position.set(ALARM_BMS_CELL_TEMP_EXCEED_CHARGE_TEMP, 1);
        }
        if (BitUtil.check(mask, 6)) {
            position.set(ALARM_BMS_PRESSURE_LOW, 1);
        }
        if (BitUtil.check(mask, 7)) {
            position.set(ALARM_BMS_PRESSURE_HIGH, 1);
        }

    }

}
