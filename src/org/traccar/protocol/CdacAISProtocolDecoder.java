/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class CdacAISProtocolDecoder extends BaseHttpProtocolDecoder {

    private static final int DATETIMECORRECTION = -330;

    public CdacAISProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String decodeAlertId(String alertId) {

        switch (alertId) {
            case "01":
            case "02":
                return null;
            case "03":
                return Position.ALARM_POWER_CUT;
            case "04":
                return Position.ALARM_LOW_BATTERY;
            case "06":
                return Position.ALARM_POWER_RESTORED;
            case "09":
            case "16":
                return Position.ALARM_TAMPERING;
            case "10":
                return Position.ALARM_SOS;
            case "13":
                return Position.ALARM_BRAKING;
            case "14":
                return Position.ALARM_ACCELERATION;
            case "15":
                return Position.ALARM_CORNERING;
            case "17":
                return Position.ALARM_OVERSPEED;
            case "18":
                return Position.ALARM_GEOFENCE_ENTER;
            case "19":
                return Position.ALARM_GEOFENCE_EXIT;
            case "23":
                return Position.ALARM_ACCIDENT;
            default:
                return null;
        }

    }

    private static double decodeCoordinate(ByteBuf buf) {
        double degrees = Double.valueOf(buf.readSlice(10).toString(StandardCharsets.US_ASCII));
        String hemisphere = buf.readSlice(1).toString(StandardCharsets.US_ASCII);
        if (hemisphere.equals("S") || hemisphere.equals("W")) {
            degrees = -degrees;
        }
        return degrees;
    }

    private void decodeCommon(ByteBuf buf, Position position) {
        String alertId = buf.readSlice(2).toString(StandardCharsets.US_ASCII);
        position.set(Position.KEY_ALARM, decodeAlertId(alertId));

        String packetStatus = buf.readSlice(1).toString(StandardCharsets.US_ASCII);
        position.setValid(buf.readSlice(1).toString(StandardCharsets.US_ASCII).equals("1"));

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                        Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                        Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        dateBuilder.setTime(Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        dateBuilder.addMinute(DATETIMECORRECTION);
        position.setTime(dateBuilder.getDate());
        position.setLatitude(decodeCoordinate(buf));
        position.setLongitude(decodeCoordinate(buf));

        Network network = new Network();
        int mcc = Integer.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));
        int mnc = Integer.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));
        int lac = Integer.parseInt(buf.readSlice(4).toString(StandardCharsets.US_ASCII), 16);
        int cellId = Integer.parseInt(buf.readSlice(9).toString(StandardCharsets.US_ASCII), 16);

        position.setSpeed(UnitsConverter.knotsFromKph(Double.valueOf(
                buf.readSlice(6).toString(StandardCharsets.US_ASCII))));
        position.setCourse(Double.valueOf(buf.readSlice(6).toString(StandardCharsets.US_ASCII)));
        position.set(Position.KEY_SATELLITES, Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        position.set(Position.KEY_HDOP, Double.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        position.set(Position.KEY_RSSI, Double.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));

        position.set(Position.KEY_IGNITION, Integer.valueOf(buf.readSlice(1).toString(StandardCharsets.US_ASCII)) == 1);
        position.set(Position.KEY_CHARGE, Integer.valueOf(buf.readSlice(1).toString(StandardCharsets.US_ASCII)) == 1);
        position.set(Position.KEY_MOTION, buf.readSlice(1).toString(StandardCharsets.US_ASCII).equals("M"));


        if (buf.readableBytes() == 5) {
            String geofenceId =  buf.readSlice(5).toString(StandardCharsets.US_ASCII);
        }

        if (buf.readableBytes() > 5) {
//            Full Packet parsing
            String vendorId =  buf.readSlice(6).toString(StandardCharsets.US_ASCII);
            position.set(Position.KEY_VERSION_FW, buf.readSlice(6).toString(StandardCharsets.US_ASCII));
            String vehicleRegNo =  buf.readSlice(16).toString(StandardCharsets.US_ASCII);
            position.setAltitude(Double.valueOf(buf.readSlice(7).toString(StandardCharsets.US_ASCII)));
            position.set(Position.KEY_PDOP, Double.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
            position.set(Position.KEY_OPERATOR, buf.readSlice(6).toString(StandardCharsets.US_ASCII));

            for (int i = 0; i < 4; i++) {
                int rssiN = Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII));
                int lacN = Integer.parseInt(buf.readSlice(4).toString(StandardCharsets.US_ASCII), 16);
                int cellIdN = Integer.parseInt(buf.readSlice(9).toString(StandardCharsets.US_ASCII), 16);
                network.addCellTower(CellTower.from(mcc, mnc, lacN, cellIdN, rssiN));
            }
            position.setNetwork(network);

            position.set(Position.KEY_EXTERNAL_BATTERY,
                    Double.valueOf(buf.readSlice(5).toString(StandardCharsets.US_ASCII)));
            position.set(Position.KEY_BATTERY, Double.valueOf(buf.readSlice(5).toString(StandardCharsets.US_ASCII)));

            String tamper =  buf.readSlice(1).toString(StandardCharsets.US_ASCII);

            for (int i = 1; i <= 4; i++) {
                int tempDio = Integer.valueOf(buf.readSlice(1).toString(StandardCharsets.US_ASCII));
                position.set(Position.PREFIX_IN + i, tempDio);
            }

            String frameNumber =  buf.readSlice(6).toString(StandardCharsets.US_ASCII);
            String checksum =  buf.readSlice(8).toString(StandardCharsets.US_ASCII);
        }
    }

    private void decodeNormalPacket(Channel channel, SocketAddress remoteAddress, ByteBuf buf, Position position) {
        String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
        }
        position.setDeviceId(deviceSession.getDeviceId());

        decodeCommon(buf, position);
    }


    private void decodeBatchPacket(Channel channel, SocketAddress remoteAddress, ByteBuf buf, Position position) {
        String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int batchLogCount = Integer.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));

        for (int i = 0; i < batchLogCount; i++) {
            decodeCommon(buf, position);
        }
    }


    private void decodeHealthPacket(Channel channel, SocketAddress remoteAddress, ByteBuf buf, Position position) {
        String vendorId =  buf.readSlice(6).toString(StandardCharsets.US_ASCII);
        position.set(Position.KEY_VERSION_FW, buf.readSlice(6).toString(StandardCharsets.US_ASCII));
        String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int ignitonOnUpdateRate = Integer.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));
        int ignitonOffUpdateRate = Integer.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));
        double batteryPerc = Double.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));
        double lowBatteryThr = Double.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII));
        double memoryPerc = Double.valueOf(buf.readSlice(3).toString(StandardCharsets.US_ASCII));

        for (int i = 1; i <= 4; i++) {
            int tempDio = Integer.valueOf(buf.readSlice(1).toString(StandardCharsets.US_ASCII));
            position.set(Position.PREFIX_IN + i, tempDio);
        }
        double analogInput = Double.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII));

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                        Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                        Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        dateBuilder.setTime(Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)),
                Integer.valueOf(buf.readSlice(2).toString(StandardCharsets.US_ASCII)));
        dateBuilder.addMinute(DATETIMECORRECTION);

        getLastLocation(position, dateBuilder.getDate());
    }


    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;

        ByteBuf buf = request.content();
        String parameter = buf.readSlice(7).toString(StandardCharsets.US_ASCII);
        buf.skipBytes(1);
//        String data = buf.toString(StandardCharsets.US_ASCII);

        Position position = new Position(getProtocolName());

        if (parameter.equals("vltdata")) {
            String header = buf.readSlice(3).toString(StandardCharsets.US_ASCII);

            switch (header) {
                case "NRM":
                case "EPB":
                case "CRT":
                case "ALT":
                case "FUL":
                    decodeNormalPacket(channel, remoteAddress, buf, position);
                    break;
                case "BTH":
                    decodeBatchPacket(channel, remoteAddress, buf, position);
                    break;
                case "HLM":
                    decodeHealthPacket(channel, remoteAddress, buf, position);
                    break;
                case "ACK":
                case "LGN":
                default:
                    break;
            }
        }

        if (position.getDeviceId() != 0) {
            sendResponse(channel, HttpResponseStatus.OK);
            return position;
        } else {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }
    }

}
