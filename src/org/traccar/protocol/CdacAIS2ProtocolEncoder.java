/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

public class CdacAIS2ProtocolEncoder extends StringProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(command, Command.KEY_DATA);
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "$CMD,relay,1#");
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "$CMD,relay,0#");
            default:
                return null;
        }
    }

}
