/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class SiwiProtocolDecoder extends BaseProtocolDecoder {

    public SiwiProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$").expression("[A-Z]+,")     // header
            .number("(d+),")                     // device id
            .number("d+,")                       // unit no
            .expression("([A-Z]),")              // reason
            .number("[^,]*,")                       // command code
            .number("[^,]*,")                    // command value
            .expression("([01]),")               // ignition
            .expression("[01],")                 // power cut
            .expression("[01],")                 // box open
            .number("[^,]*,")                       // message key
            .number("(d+),")                     // odometer
            .number("(d+),")                     // speed
            .number("(d+),")                     // satellites
            .expression("([AV]),")               // valid
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // course
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_EVENT, parser.next());
        boolean isIgnition = parser.next().equals("1");
        position.set(Position.KEY_IGNITION, isIgnition);
        position.set(Position.KEY_ODOMETER, parser.nextInt(0));

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt(0)));

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextDouble(0));
        position.setLongitude(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));
        position.setCourse(parser.nextInt(0));

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY, "IST"));

        if (!isIgnition) {
            getLastLocation(position, position.getDeviceTime());
        }

        return position;
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
            }

            if (deviceTime != null) {
                position.setDeviceTime(deviceTime);
            } else {
                position.setDeviceTime(new Date());
            }
        }
    }

}
