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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BoltCamProtocolEncoder extends BaseProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        ChannelBuffer id = ChannelBuffers.wrappedBuffer(
                DataConverter.parseHex(getUniqueId(command.getDeviceId())));
        ChannelBuffer data = ChannelBuffers.dynamicBuffer();
        byte[] time = DataConverter.parseHex(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                data.writeByte(0xf0);
                return BoltCamProtocolDecoder.formatMessage(
                        BoltCamProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
            case Command.TYPE_ENGINE_RESUME:
                data.writeByte(0xf1);
                return BoltCamProtocolDecoder.formatMessage(
                        BoltCamProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
            case Command.TYPE_OUTPUT_CONTROL:
                if (command.getInteger(Command.KEY_INDEX) == 1) {
                    return formatMessage(BoltCamProtocolDecoder.MSG_RTP_CAMERA_PLAY, id,
                            Integer.parseInt(command.getString(Command.KEY_DATA)));
                } else {
                    return formatStopMessage(BoltCamProtocolDecoder.MSG_RTP_CAMERA_PLAY_CONTROL, id,
                            Integer.parseInt(command.getString(Command.KEY_DATA)));
                }
            default:
                return null;
        }
    }

    public static ChannelBuffer formatMessage(int type, ChannelBuffer id, int command) {

        ChannelBuffer data = ChannelBuffers.dynamicBuffer();
        // ip
        ChannelBuffer ip = ChannelBuffers.wrappedBuffer("35.200.245.46".getBytes(StandardCharsets.US_ASCII));
        data.writeByte(ip.readableBytes());
        data.writeBytes(ip);
        data.writeShort(1078);
        data.writeShort(0);
        data.writeByte(command);
        data.writeByte(0x00);
        data.writeByte(0x00);

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        buf.writeShort(0x0078);
        buf.writeBytes(data);
        buf.writeByte(Checksum.xor(buf.toByteBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

    public static ChannelBuffer formatStopMessage(int type, ChannelBuffer id, int channelId) {

        ChannelBuffer data = ChannelBuffers.dynamicBuffer();
        data.writeByte(channelId);
        data.writeByte(0x00);
        data.writeByte(0x00);
        data.writeByte(0x00);

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        buf.writeShort(1);
        buf.writeBytes(data);
        buf.writeByte(Checksum.xor(buf.toByteBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

}
