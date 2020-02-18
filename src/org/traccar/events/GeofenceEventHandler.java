/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar.events;

import io.netty.channel.ChannelHandler;
import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.database.GeofenceManager;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@ChannelHandler.Sharable
public class GeofenceEventHandler extends BaseEventHandler {

    private GeofenceManager geofenceManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(GeofenceEventHandler.class);

    public GeofenceEventHandler() {
        geofenceManager = Context.getGeofenceManager();
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = Context.getIdentityManager().getById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getIdentityManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        List<Long> currentGeofences = geofenceManager.getCurrentDeviceGeofences(position);
        List<Long> oldGeofences = new ArrayList<>();
        if (device.getGeofenceIds() != null) {
            oldGeofences.addAll(device.getGeofenceIds());
        }
        List<Long> newGeofences = new ArrayList<>(currentGeofences);

        if (device.getId() == 13350) {
            LOGGER.info("GeoFix position: " + position.getLatitude() + ' ' + position.getLongitude());
            LOGGER.info("GeoFix currentGeofences: " + Arrays.toString(currentGeofences.toArray()));
            LOGGER.info("GeoFix oldGeofences: " + Arrays.toString(oldGeofences.toArray()));
        }

        newGeofences.removeAll(oldGeofences);
        oldGeofences.removeAll(currentGeofences);



        device.setGeofenceIds(currentGeofences);

        Map<Event, Position> events = new HashMap<>();
        for (long geofenceId : oldGeofences) {
            long calendarId = geofenceManager.getById(geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? Context.getCalendarManager().getById(calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_EXIT, position.getDeviceId(), position.getId());
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        for (long geofenceId : newGeofences) {
            long calendarId = geofenceManager.getById(geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? Context.getCalendarManager().getById(calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_ENTER, position.getDeviceId(), position.getId());
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        return events;
    }
}
