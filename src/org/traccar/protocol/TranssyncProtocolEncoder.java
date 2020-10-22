/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class TranssyncProtocolEncoder extends BaseProtocolEncoder {

    protected String getUniqueId(long deviceId) {
        return Context.getIdentityManager().getById(deviceId).getUniqueId();
    }

    private ByteBuf encodeContent(long deviceId, String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x2a);
        buf.writeByte(0x2a);

        buf.writeByte(1 + 1 + 3 + content.length() + 2 + 2); // message length

        buf.writeByte(0x00); // message type
        buf.writeByte(0x00); // message type

        buf.writeBytes(DataConverter.parseHex('0' + getUniqueId(deviceId)));


        buf.writeByte(0x03); // message type
        buf.writeByte(0x00); // message type
        buf.writeByte(0x40); // message type

        buf.writeByte(content.length()); // command length
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII)); // command

        buf.writeByte(0x23);
        buf.writeByte(0x23);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(), "VLT;0000;GPO1#ON;");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(), "VLT;0000;GPO1#OFF;");
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            default:
                return null;
        }
    }

}
