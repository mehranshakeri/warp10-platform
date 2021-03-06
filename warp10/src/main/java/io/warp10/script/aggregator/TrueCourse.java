//
//   Copyright 2016  Cityzen Data
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.aggregator;

import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptAggregatorFunction;
import io.warp10.script.WarpScriptBucketizerFunction;
import io.warp10.script.WarpScriptMapperFunction;
import io.warp10.script.WarpScriptReducerFunction;
import io.warp10.script.WarpScriptException;

import java.util.Arrays;
import java.util.Comparator;

import com.geoxp.GeoXPLib;

/**
 * Compute the true course between points on a great circle.
 *
 * @see http://williams.best.vwh.net/avform.htm#Crs
 * 
 * Returned location and elevation are those of the tick being computed.
 */
public class TrueCourse extends NamedWarpScriptFunction implements WarpScriptAggregatorFunction, WarpScriptMapperFunction, WarpScriptBucketizerFunction, WarpScriptReducerFunction {
  
  public TrueCourse(String name) {
    super(name);
  }
  
  @Override
  public Object apply(Object[] args) throws WarpScriptException {
    long tick = (long) args[0];
    final long[] ticks = (long[]) args[3];
    long[] locations = (long[]) args[4];
    long[] elevations = (long[]) args[5];
    
    if (0 == ticks.length) {
      return new Object[] { Long.MAX_VALUE, GeoTimeSerie.NO_LOCATION, GeoTimeSerie.NO_ELEVATION, null };
    }

    //
    // We need to sort indices from oldest tick to most recent
    //
    
    Integer[] idx = new Integer[ticks.length];
    
    long location = GeoTimeSerie.NO_LOCATION;
    long elevation = GeoTimeSerie.NO_ELEVATION;
    
    for (int i = 0; i < idx.length; i++) {
      idx[i] = i;
      if (tick == ticks[i]) {
        location = locations[i];
        elevation = elevations[i];
      }
    }
    
    Arrays.sort(idx, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        if (ticks[o1] < ticks[o2]) {
          return -1;
        } else if (ticks[o1] > ticks[o2]) {
          return 1;
        } else {
          return 0;
        }
      }
    });

    //
    // The idx array now contains the indices in the order
    // of increasing ticks.
    //
    
    //
    // Return null if extrema do not have a location
    //
    
    if (GeoTimeSerie.NO_LOCATION == locations[0] || GeoTimeSerie.NO_LOCATION == locations[locations.length - 1]) {
      return new Object[] { Long.MAX_VALUE, GeoTimeSerie.NO_LOCATION, GeoTimeSerie.NO_ELEVATION, null };
    }
    
    //
    // Extract lat/lon
    //

    double[] latlon1 = GeoXPLib.fromGeoXPPoint(locations[0]);
    double[] latlon2 = GeoXPLib.fromGeoXPPoint(locations[locations.length - 1]);
    
    //
    // Convert in radians
    //
    
    latlon1[0] = Math.toRadians(latlon1[0]);
    latlon1[1] = Math.toRadians(latlon1[1]);
    latlon2[0] = Math.toRadians(latlon2[0]);
    latlon2[1] = Math.toRadians(latlon2[1]);
    
    //
    // Compute true course
    //
    
    double tc = Math.IEEEremainder(Math.atan2(Math.sin(latlon1[1]-latlon2[1])*Math.cos(latlon2[1]), Math.cos(latlon1[0])*Math.sin(latlon2[0])-Math.sin(latlon1[0])*Math.cos(latlon2[0])*Math.cos(latlon1[1]-latlon2[1])), 2.0D*Math.PI);
                
    if (tc < 0) {
      tc = tc + Math.PI + Math.PI;
    }
  
    // Convert to degrees
    tc = Math.toDegrees(tc);
    
    return new Object[] { tick, location, elevation, tc };
  }
}
