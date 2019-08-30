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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class St351ProtocolDecoder extends BaseProtocolDecoder {

    public St351ProtocolDecoder(St351Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("")
            .expression("(PT|PTH);")              // Header
            .expression("([0-9]+);")              // IMEI
            .expression("([^,]+)?;")              // Protocol Model
            .number("(dddd)(dd)(dd);")            // date utc (DDMMYYYY)
            .number("(dd):(dd):(dd);")            // time utc (hhmmss)
            .expression("([^,]+)?,")              // MCC
            .expression("([^,]+)?,")              // MNC
            .expression("([^,]+)?,")              // CELLID
            .expression("([^,]+)?;")              // LAC
            .number("([+-])(d+.d+);")             // latitude
            .number("([+-])(d+.d+);")             // longitude
            .number("(d+.d+);")                   // course
            .number("(d+.d+);")                   // speed
            .number("(d+);")                      // No of satellites
            .number("(d+);")                      // GSM signal strength
            .expression("([^,]+)?;")                      // odometer
            .number("(d+.?d*)?;?")                // Main input voltage
            .number("(d+.?d*)?;?")                // internal battery voltage
            .number("(d)(d)(d)(d)")               // digital Input 4
            .number("(d)(d);")                    // digital Output 2
            .number("(d);")                       // ignition
            .number("(d);")                       // extra
            .number("(d);")                       // extra
            .number("(d+)")                       // Frame number
            .any()
            .compile();


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

    private Object decodeNormal(Position position, Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }
        String header = parser.next();
        String imei = parser.next();
        position.set(Position.KEY_VERSION_FW, parser.next());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        Network network = new Network();
        int mcc = parser.nextHexInt();
        int mnc = parser.nextHexInt();
        int lac = parser.nextHexInt();
        int cellId = parser.nextHexInt();

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
//        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        int noOfSattellites = parser.nextInt(0);
        position.setValid(noOfSattellites > 0);
        position.set(Position.KEY_SATELLITES, noOfSattellites);
        int rssi = parser.nextInt(0);
        position.set(Position.KEY_RSSI, rssi);

        network.addCellTower(CellTower.from(mcc, mnc, lac, cellId, rssi));

        String odometer = parser.next();
        position.set("maininput", parser.nextDouble(0));
        position.set(Position.KEY_BATTERY, parser.nextDouble(0));

        for (int i = 1; i <= 4; i++) {
            int tempDio = parser.nextInt(0);
            position.set(Position.PREFIX_IN + i, tempDio);
        }
        for (int i = 1; i <= 2; i++) {
            position.set(Position.PREFIX_OUT + i, parser.nextInt(0));
        }

        boolean ignition = false;
        switch (parser.nextInt()) {
            case 2:
                ignition = true;
                break;
            case 3:
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            default:
                break;
        }

        if (ignition) {
            position.set(Position.KEY_IGNITION, true);
        } else {
            getLastLocation(position, null);
            position.set(Position.KEY_IGNITION, false);
        }

//        int frameNumber = parser.nextInt(0);

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = (String) msg;
        Position position = new Position(getProtocolName());

        return decodeNormal(position, channel, remoteAddress, sentence);
    }
}
