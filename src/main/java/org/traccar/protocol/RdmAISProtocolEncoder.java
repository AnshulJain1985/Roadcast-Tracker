package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

public class RdmAISProtocolEncoder extends StringProtocolEncoder
        implements StringProtocolEncoder.ValueFormatter {
    @Override
    public String formatValue(String key, Object value) {

        if (key.equals(Command.KEY_FREQUENCY)) {
            long frequency = ((Number) value).longValue();
            if (frequency / 60 / 60 > 0) {
                return String.format("%02dh", frequency / 60 / 60);
            } else if (frequency / 60 > 0) {
                return String.format("%02dm", frequency / 60);
            } else {
                return String.format("%02ds", frequency);
            }
        }

        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "{%s}", Command.KEY_DATA);
            default:
                return null;
        }
    }
}
