/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class Minifinder2ProtocolEncoder extends BaseProtocolEncoder {

    public Minifinder2ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(ByteBuf content) {

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0xAB); // header
        response.writeByte(0x00); // properties
        response.writeShort(content.readableBytes());
        response.writeShort(Checksum.crc16(Checksum.CRC16_XMODEM, content.nioBuffer()));
        response.writeShort(0x0101);
        response.writeBytes(content);

        return response;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf content = Unpooled.buffer();

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                content.writeBytes(command.getString(Command.KEY_DATA).getBytes(StandardCharsets.US_ASCII));
                return encodeContent(content);
//            case Command.TYPE_GET_VERSION:
//                return encodeContent(RuptelaProtocolDecoder.MSG_DEVICE_VERSION, content);
//            case Command.TYPE_FIRMWARE_UPDATE:
//                content.writeBytes("|FU_STRT*\r\n".getBytes(StandardCharsets.US_ASCII));
//                return encodeContent(RuptelaProtocolDecoder.MSG_FIRMWARE_UPDATE, content);
//            case Command.TYPE_OUTPUT_CONTROL:
//                content.writeInt(command.getInteger(Command.KEY_INDEX));
//                content.writeInt(Integer.parseInt(command.getString(Command.KEY_DATA)));
//                return encodeContent(RuptelaProtocolDecoder.MSG_SET_IO, content);
//            case Command.TYPE_SET_ODOMETER:
//                content.writeInt(Integer.parseInt(command.getString(Command.KEY_DATA)));
//                return encodeContent(RuptelaProtocolDecoder.MSG_SET_ODOMETER, content);
            default:
                return null;
        }
    }

}
