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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.util.Date;
import java.util.TimeZone;

public class TranssyncProtocolDecoder extends BaseProtocolDecoder {

    private final int distanceFilter = Context.getConfig().getInteger(getProtocolName() + ".distanceFilter");


    public TranssyncProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Object decodeBasic(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        int length = buf.readUnsignedByte();
//        int lac = buf.readUnsignedShort();
        buf.skipBytes(2);

        String imei = ByteBufUtil.hexDump(buf.readBytes(8)).substring(1);
        DeviceSession deviceSession = null;
        deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        //Information Serial Number
        buf.skipBytes(2);
        //protocol number
        buf.skipBytes(1);

        DateBuilder dateBuilder = new DateBuilder(TimeZone.getTimeZone("UTC"))
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

        position.setTime(dateBuilder.getDate());

        double latitude = buf.readUnsignedInt() / 60.0 / 30000.0;
        double longitude = buf.readUnsignedInt() / 60.0 / 30000.0;

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readShort());

        int mnc = buf.readUnsignedByte();
//        int cid = buf.readUnsignedShort();
        buf.skipBytes(2);

        int status3 = buf.readUnsignedByte();
        int status2 = buf.readUnsignedByte();
        int status1 = buf.readUnsignedByte();
        int status0 = buf.readUnsignedByte();

        position.setValid(BitUtil.check(status0, 0));

        Position last = Context.getIdentityManager().getLastPosition(position.getDeviceId());

        if (!BitUtil.check(status0, 1)) {
            latitude = -latitude;
        }
        if (!BitUtil.check(status0, 2)) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);

        boolean isIgnition = BitUtil.check(status0, 3);
        position.set(Position.KEY_IGNITION, isIgnition);

        if (distanceFilter > 0 && !isIgnition) {
            if (last != null && last.getLatitude() != 0.0 && last.getLongitude() != 0.0) {

                double distance = DistanceCalculator.distance(
                        position.getLatitude(), position.getLongitude(),
                        last.getLatitude(), last.getLongitude());

                distance = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_EVEN).doubleValue();

                if (distance < distanceFilter || !position.getValid()) {
                    getLastLocation(position, new Date());
                }
            }
        }

        if (BitUtil.check(status0, 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
            position.set(Position.KEY_CHARGE, false);
        } else {
            position.set(Position.KEY_CHARGE, true);
        }
        position.set(Position.KEY_DOOR, BitUtil.check(status0, 5));
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(status0, 6));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(status0, 7));

        if (BitUtil.check(status2, 5)) {
            position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
        }
        if (BitUtil.check(status2, 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        if (BitUtil.check(status2, 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }

        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_HDOP, buf.readUnsignedByte());

        position.set(Position.PREFIX_ADC + 1, buf.readShort());

//        buf.skipBytes(2);
//        buf.skipBytes(1);
//        buf.skipBytes(1);
//        buf.skipBytes(5);
//        buf.skipBytes(1);
//        buf.skipBytes(buf.readUnsignedByte());
//
//        buf.skipBytes(1);
//        buf.skipBytes(buf.readUnsignedByte());
//
//        buf.skipBytes(2);
        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readShort();

        if (header == 0x2a2a || header == 0x3a3a) {
            return decodeBasic(channel, remoteAddress, buf);
        } else {
            return null;
        }
    }

}
