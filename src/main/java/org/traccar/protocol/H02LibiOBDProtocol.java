/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Keys;
import org.traccar.model.Command;

public class H02LibiOBDProtocol extends BaseProtocol {

    public H02LibiOBDProtocol() {
        setSupportedDataCommands(
                Command.TYPE_ALARM_ARM,
                Command.TYPE_ALARM_DISARM,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_POSITION_PERIODIC
        );
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                int messageLength = Context.getConfig().getInteger(Keys.PROTOCOL_MESSAGE_LENGTH.withPrefix(getName()));
                pipeline.addLast(new H02LibiOBDFrameDecoder(messageLength));
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new H02LibiOBDProtocolEncoder(H02LibiOBDProtocol.this));
                pipeline.addLast(new H02LibiOBDProtocolDecoder(H02LibiOBDProtocol.this));
            }
        });
        addServer(new TrackerServer(true, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new H02LibiOBDProtocolEncoder(H02LibiOBDProtocol.this));
                pipeline.addLast(new H02LibiOBDProtocolDecoder(H02LibiOBDProtocol.this));
            }
        });
    }
}
