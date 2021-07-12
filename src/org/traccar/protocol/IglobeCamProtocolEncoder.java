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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IglobeCamProtocolEncoder extends BaseProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf id = Unpooled.wrappedBuffer(
                DataConverter.parseHex(getUniqueId(command.getDeviceId())));
        try {
            ByteBuf data = Unpooled.buffer();
            byte[] time = DataConverter.parseHex(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));

            switch (command.getType()) {
                case Command.TYPE_ENGINE_STOP:
                    data.writeByte(0xf0);
                    return IglobeCamProtocolDecoder.formatMessage(
                            IglobeCamProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                case Command.TYPE_ENGINE_RESUME:
                    data.writeByte(0xf1);
                    return IglobeCamProtocolDecoder.formatMessage(
                            IglobeCamProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                case Command.TYPE_OUTPUT_CONTROL:
                    if (command.getInteger(Command.KEY_INDEX) == 1) {
                        return formatMessage(IglobeCamProtocolDecoder.MSG_RTP_CAMERA_PLAY, id,
                                Integer.parseInt(command.getString(Command.KEY_DATA)));
                    } else {
                        return formatStopMessage(IglobeCamProtocolDecoder.MSG_RTP_CAMERA_PLAY_CONTROL, id,
                                Integer.parseInt(command.getString(Command.KEY_DATA)));
                    }
                default:
                    return null;
            }
        } finally {
            id.release();
        }
    }

    public static ByteBuf formatMessage(int type, ByteBuf id, int command) {

        ByteBuf data = Unpooled.buffer();
        // ip
        ByteBuf ip = Unpooled.wrappedBuffer("164.52.215.72".getBytes(StandardCharsets.US_ASCII));
        data.writeByte(ip.readableBytes());
        data.writeBytes(ip);
        data.writeShort(1078);
        data.writeShort(0);
        data.writeByte(command);
        data.writeByte(0x00);
        data.writeByte(0x00);

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        buf.writeShort(0x0078);
        buf.writeBytes(data);
        data.release();
        buf.writeByte(Checksum.xor(buf.nioBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

    public static ByteBuf formatStopMessage(int type, ByteBuf id, int channelId) {

        ByteBuf data = Unpooled.buffer();
        data.writeByte(channelId);
        data.writeByte(0x00);
        data.writeByte(0x00);
        data.writeByte(0x00);

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        buf.writeShort(1);
        buf.writeBytes(data);
        data.release();
        buf.writeByte(Checksum.xor(buf.nioBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

}
