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
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class RoadcastProtocolDecoder extends BaseProtocolDecoder {

    public RoadcastProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    //        id=cf0afbc2013d8aca&charge=0&lon=77.52924484&lat=12.94489862&bearing=304.0&accuracy=24.079786&batt=
//                86&speed=8.69&timestamp=1574915680&user_id=1853&company_id=4

//        "imei:cf0afbc2013d8aca,0,G,77.52924484,12.94489862,304.0,24.079786,86,8.69,1574915680,1853,4;"


    private static final Pattern PATTERN = new PatternBuilder()
            .text("imei:")
            .expression("([^,]+),")              // imei
            .number("(d+)?,")                    // charge
            .expression("([FG]),")               // valid
            .number("(d+.d+)?,")                 // lon
            .number("(d+.d+)?,")                 // lat
            .number("(d+.d+)?,")                 // bearing
            .number("(d+.d+)?,")                 // accuracy
            .number("(d+)?,")                    // battery
            .number("(d+.d+)?,")                 // speed
            .number("(d+)?,")                    // timestamp unix
            .number("(d+),")                     // user_id
            .number("(d+)")                     // company_id
            .any()
            .compile();

    private String decodeAlarm(String value) {
        switch (value) {
            case "tracker":
                return null;
            case "help me":
                return Position.ALARM_SOS;
            case "low battery":
                return Position.ALARM_LOW_BATTERY;
            case "stockade":
                return Position.ALARM_GEOFENCE;
            case "move":
                return Position.ALARM_MOVEMENT;
            case "speed":
                return Position.ALARM_OVERSPEED;
            case "acc on":
                return Position.ALARM_POWER_ON;
            case "acc off":
                return Position.ALARM_POWER_OFF;
            case "door alarm":
                return Position.ALARM_DOOR;
            case "ac alarm":
                return Position.ALARM_POWER_CUT;
            case "accident alarm":
                return Position.ALARM_ACCIDENT;
            case "sensor alarm":
                return Position.ALARM_SHOCK;
            case "bonnet alarm":
                return Position.ALARM_BONNET;
            case "footbrake alarm":
                return Position.ALARM_FOOT_BRAKE;
            case "DTC":
                return Position.ALARM_FAULT;
            default:
                return null;
        }
    }

    private Position decodeRegular(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        if (!sentence.isEmpty()) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage("ON", remoteAddress));
            }
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_CHARGE, parser.next().equals("1"));
        position.setValid(parser.next().equals("G"));

        position.setLongitude(parser.nextDouble());
        position.setLatitude(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.setAccuracy(parser.nextDouble());
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.setTime(new Date(parser.nextLong() * 1000));

        position.set("userId", parser.nextInt());
        position.set("compId", parser.nextInt());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        return decodeRegular(channel, remoteAddress, sentence);
    }

}
