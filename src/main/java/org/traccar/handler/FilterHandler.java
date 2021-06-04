/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.util.Date;


@ChannelHandler.Sharable
public class FilterHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterHandler.class);

    private final boolean filterInvalid;
    private final boolean filterZero;
    private final boolean filterDuplicate;
    private final long filterFuture;
    private final boolean filterApproximate;
    private final int filterAccuracy;
    private final boolean filterStatic;
    private final int filterDistance;
    private final int filterMaxSpeed;
    private final long filterMinPeriod;
    private final long skipLimit;
    private final boolean skipAttributes;

    public FilterHandler(Config config) {
        filterInvalid = config.getBoolean(Keys.FILTER_INVALID);
        filterZero = config.getBoolean(Keys.FILTER_ZERO);
        filterDuplicate = config.getBoolean(Keys.FILTER_DUPLICATE);
        filterFuture = config.getLong(Keys.FILTER_FUTURE) * 1000;
        filterAccuracy = config.getInteger(Keys.FILTER_ACCURACY);
        filterApproximate = config.getBoolean(Keys.FILTER_APPROXIMATE);
        filterStatic = config.getBoolean(Keys.FILTER_STATIC);
        filterDistance = config.getInteger(Keys.FILTER_DISTANCE);
        filterMaxSpeed = config.getInteger(Keys.FILTER_MAX_SPEED);
        filterMinPeriod = config.getInteger(Keys.FILTER_MIN_PERIOD) * 1000;
        skipLimit = config.getLong(Keys.FILTER_SKIP_LIMIT) * 1000;
        skipAttributes = config.getBoolean(Keys.FILTER_SKIP_ATTRIBUTES_ENABLE);
    }

    private boolean filterInvalid(Position position) {
        return filterInvalid && (!position.getValid()
                || position.getLatitude() > 90 || position.getLongitude() > 180
                || position.getLatitude() < -90 || position.getLongitude() < -180);
    }

    private boolean filterZero(Position position) {
        return filterZero && (position.getLatitude() == 0.0 || position.getLongitude() == 0.0);
    }

    private boolean filterDuplicate(Position position, Position last) {
        if (filterDuplicate && last != null && position.getFixTime().equals(last.getFixTime())
                && last.getBoolean(Position.KEY_IGNITION) == position.getBoolean(Position.KEY_IGNITION)) {
            for (String key : position.getAttributes().keySet()) {
                if (!last.getAttributes().containsKey(key)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean filterFuture(Position position) {
        return filterFuture != 0 && position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture;
    }

    private boolean filterAccuracy(Position position) {
        return filterAccuracy != 0 && position.getAccuracy() > filterAccuracy;
    }

    private boolean filterApproximate(Position position) {
        return filterApproximate && position.getBoolean(Position.KEY_APPROXIMATE);
    }

    private boolean filterStatic(Position position) {
        return filterStatic && position.getSpeed() == 0.0;
    }

    private boolean filterDistance(Position position, Position last) {
        if (filterDistance != 0 && last != null
                && (last.getBoolean(Position.KEY_IGNITION) == position.getBoolean(Position.KEY_IGNITION))) {
            return position.getDouble(Position.KEY_DISTANCE) < filterDistance;
        }
        return false;
    }

    private boolean filterMaxSpeed(Position position, Position last) {
        if (filterMaxSpeed != 0 && last != null) {
            double distance = position.getDouble(Position.KEY_DISTANCE);
            double time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return UnitsConverter.knotsFromMps(distance / (time / 1000)) > filterMaxSpeed;
        }
        return false;
    }

    private boolean filterMinPeriod(Position position, Position last) {
        if (filterMinPeriod != 0 && last != null) {
            long time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return time > 0 && time < filterMinPeriod;
        }
        return false;
    }

    private boolean skipLimit(Position position, Position last) {
        if (skipLimit != 0 && last != null) {
            return (position.getServerTime().getTime() - last.getServerTime().getTime()) > skipLimit;
        }
        return false;
    }

    private boolean skipAttributes(Position position, Position last) {
        if (skipAttributes) {
            String attributesString = Context.getIdentityManager().lookupAttributeString(
                    position.getDeviceId(), "filter.skipAttributes", "", false, true);
            for (String attribute : attributesString.split("[ ,]")) {
                if (position.getAttributes().containsKey(attribute)) {
                    return true;
                }
            }
        }
        if (last != null && position.getProtocol().equals("teltonika")
                && ((last.getLong(Position.PREFIX_ADC + 1) != position.getLong(Position.PREFIX_ADC + 1))
                || (last.getLong("di1") != position.getLong("di1"))
                || (last.getLong("di2") != position.getLong("di2")))) {
            return true;
        }
        return last != null && position.getAttributes().containsKey(Position.KEY_POWER)
                && position.getDouble(Position.KEY_POWER) != last.getDouble(Position.KEY_POWER);
    }

    private boolean filter(Position position, Position last) {

        StringBuilder filterType = new StringBuilder();

        if (skipAttributes(position, last)) {
            return false;
        }

        if (filterInvalid(position)) {
            filterType.append("Invalid ");
        }
        if (filterZero(position)) {
            filterType.append("Zero ");
        }
        if (filterDuplicate(position, last) && !skipLimit(position, last) && !skipAttributes(position, last)) {
            filterType.append("Duplicate ");
        }
        if (filterFuture(position)) {
            filterType.append("Future ");
        }
        if (filterAccuracy(position)) {
            filterType.append("Accuracy ");
        }
        if (filterApproximate(position)) {
            filterType.append("Approximate ");
        }
        if (filterStatic(position) && !skipLimit(position, last) && !skipAttributes(position, last)) {
            filterType.append("Static ");
        }
        if (filterDistance(position, last) && !skipLimit(position, last) && !skipAttributes(position, last)) {
            filterType.append("Distance ");
        }
        if (filterMaxSpeed(position, last)) {
            filterType.append("MaxSpeed ");
        }
        if (filterMinPeriod(position, last)) {
            filterType.append("MinPeriod ");
        }

        if (filterType.length() > 0) {

            StringBuilder message = new StringBuilder();
            message.append("Position filtered by ");
            message.append(filterType.toString());
            message.append("filters from device: ");
            message.append(Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId());

            LOGGER.info(message.toString());
            return true;
        }

        return false;
    }

    @Override
    protected Position handlePosition(Position position) {

        Position last = null;
        if (Context.getIdentityManager() != null) {
            last = Context.getIdentityManager().getLastPosition(position.getDeviceId());
        }

        if (filter(position, last)) {
            return null;
        }

        if (last != null) {
            if (position.getLatitude() == 0.0 || position.getLongitude() == 0.0
                    || position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture) {
                position.setFixTime(last.getFixTime());
                position.setValid(last.getValid());
                position.setLatitude(last.getLatitude());
                position.setLongitude(last.getLongitude());
                position.setAltitude(last.getAltitude());
                position.setSpeed(last.getSpeed());
                position.setCourse(last.getCourse());
                position.setAccuracy(last.getAccuracy());
                if (last.getAttributes().containsKey(Position.KEY_DISTANCE)) {
                    position.set(Position.KEY_DISTANCE, last.getDouble(Position.KEY_DISTANCE));
                    position.set(Position.KEY_TOTAL_DISTANCE, last.getDouble(Position.KEY_TOTAL_DISTANCE));
                }
            }
        } else {
            if (position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture) {
                position.setFixTime(new Date(0));
            }
        }
        return position;
    }

}
