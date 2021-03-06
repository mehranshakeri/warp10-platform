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

package io.warp10.script.functions;

import java.util.ArrayList;
import java.util.List;

import io.warp10.continuum.store.Constants;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Adds days to a timestamp
 */
public class ADDDAYS extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  public ADDDAYS(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
  
    Object top = stack.pop();
    
    if (!(top instanceof Long)) {
      throw new WarpScriptException(getName() + " expects a number of days on top of the stack.");
    }
    
    int days = ((Number) top).intValue();
    
    top = stack.pop();
    
    String tz = null;
    
    if (top instanceof String) {
      tz = top.toString();
      top = stack.pop();
      if (!(top instanceof Long)) {
        throw new WarpScriptException(getName() + " operates on a tselements list, timestamp, or timestamp and timezone.");
      }
    } else if (!(top instanceof List) && !(top instanceof Long)) {
      throw new WarpScriptException(getName() + " operates on a tselements list, timestamp, or timestamp and timezone.");
    }
    
    
    if (top instanceof Long) {
      long instant = ((Number) top).longValue();
        
      if (null == tz) {
        tz = "UTC";
      }

      DateTimeZone dtz = DateTimeZone.forID(null == tz ? "UTC" : tz);
    
      DateTime dt = new DateTime(instant / Constants.TIME_UNITS_PER_MS, dtz);
    
      dt = dt.plusDays(days);
    
      long ts = dt.getMillis() * Constants.TIME_UNITS_PER_MS + (instant % Constants.TIME_UNITS_PER_MS);
    
      stack.push(ts);
    } else {
      List<Object> elts = new ArrayList<Object>((List<Object>) top);
      
      int year = ((Number) elts.get(0)).intValue();
      int month = ((Number) elts.get(1)).intValue();
      int day = ((Number) elts.get(2)).intValue();
      
      if (days < 0) {
        while(days < 0) {
          days++;
          day = day - 1;
          if (day < 1) {
            month--;
            if (month < 1) {
              year--;
              month = 12;
            }
            if (1 == month || 3 == month || 5 == month || 7 == month || 8 == month || 10 == month || 12 == month) {
              day = 31;
            } else if (4 == month || 6 == month || 9 == month || 11 == month) {
              day = 30;
            } else if (0 == year % 100 || 0 != year % 4) {
              day = 28;
            } else {
              day = 29;
            }
          }
        }        
      } else {
        while(days > 0) {
          days--;
          day = day + 1;

          if ((1 == month || 3 == month || 5 == month || 7 == month || 8 == month || 10 == month || 12 == month) && day > 31) {
            month++;
            day = 1;
          } else if ((4 == month || 6 == month || 9 == month || 11 == month) && day > 30) {
            month++;
            day = 1;
          } else if (2 == month && (0 == year % 100 || 0 != year % 4) && day > 28) {
            month++;
            day = 1;
          } else if (2 == month && day > 29) {
            month++;
            day = 1;
          }

          if (month > 12) {
            month = 1;
            year++;
          }
        }
      }
      
      elts.set(0, (long) year);
      elts.set(1, (long) month);
      elts.set(2, (long) day);
           
      stack.push(elts);
    }
        
    return stack;
  }
}
