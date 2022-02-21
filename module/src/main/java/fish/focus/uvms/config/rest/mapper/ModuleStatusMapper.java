/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.config.rest.mapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import fish.focus.uvms.config.rest.entity.ModuleStatus;

public class ModuleStatusMapper {

    public static Map<String, ModuleStatus> mapToModuleStatus(Map<String, Instant> lastPings) {
        Map<String, ModuleStatus> map = new HashMap<>();
        for (Entry<String, Instant> modulePingEntry : lastPings.entrySet()) {
            map.put(modulePingEntry.getKey(), ModuleStatusMapper.mapToModuleStatus(modulePingEntry.getValue()));
        }
        
        return map;
    }

    public static ModuleStatus mapToModuleStatus(Instant lastPing) {
        ModuleStatus status = new ModuleStatus();
        status.setLastPing(lastPing.toString());
        status.setOnline(isOnline(lastPing));
        return status;
    }

    public static boolean isOnline(Instant timestamp) {
        return Duration.between(Instant.now(), timestamp).minus(5, ChronoUnit.MINUTES).isNegative();
    }

}