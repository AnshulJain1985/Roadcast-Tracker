package org.traccar.protocol;

import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

public class UproProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public UproProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {
        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "*HQ2011BB1#", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "*HQ2011BB0#", Command.KEY_UNIQUE_ID);
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "{%s}", Command.KEY_DATA);
            default:
                return null;
        }
    }

}
