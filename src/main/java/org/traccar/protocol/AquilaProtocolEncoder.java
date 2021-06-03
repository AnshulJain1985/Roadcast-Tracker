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

import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class AquilaProtocolEncoder extends BaseProtocolEncoder {

//    set$123456789@aquila123#SET_D01:1*
    private Object formatCommand(Command command, String content) {
        String uniqueId = getUniqueId(command.getDeviceId());
        String result = String.format("#set$%s@aquila123#%s*" + "\r\n", uniqueId, content);
        return result;
    }


    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "SET_DO1:1");
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "SET_DO1:0");
            case Command.TYPE_CUSTOM:
                return Unpooled.copiedBuffer(
                        command.getString(Command.KEY_DATA) + "\r\n", StandardCharsets.US_ASCII);
            default:
                return null;
        }
    }
}
