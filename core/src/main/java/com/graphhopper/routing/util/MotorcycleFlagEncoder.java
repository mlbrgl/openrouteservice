/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMWay;
import com.graphhopper.reader.osm.conditional.ConditionalTagsInspector;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import java.util.HashSet;

import static com.graphhopper.routing.util.PriorityCode.BEST;

/**
 * Defines bit layout for motorbikes
 * <p>
 *
 * @author Peter Karich
 * @author boldtrn
 */
public class MotorcycleFlagEncoder extends CarFlagEncoder
{
    public static final int CURVATURE_KEY = 112;

    private EncodedDoubleValue reverseSpeedEncoder;
    private EncodedValue priorityWayEncoder;
    private EncodedValue curvatureEncoder;
    private final HashSet<String> avoidSet = new HashSet<String>();
    private final HashSet<String> preferSet = new HashSet<String>();

    public MotorcycleFlagEncoder()
    {
        this(5, 5, 0);
    }

    public MotorcycleFlagEncoder( PMap properties )
    {
        this(
                (int) properties.getLong("speedBits", 5),
                properties.getDouble("speedFactor", 5),
                properties.getBool("turnCosts", false) ? 1 : 0
        );
        this.properties = properties;
        this.setBlockFords(properties.getBool("blockFords", true));
    }

    public MotorcycleFlagEncoder( String propertiesStr )
    {
        this(new PMap(propertiesStr));
    }

    public MotorcycleFlagEncoder( int speedBits, double speedFactor, int maxTurnCosts )
    {
        super(speedBits, speedFactor, maxTurnCosts);
        restrictions.remove("motorcar");
        //  moped, mofa
        restrictions.add("motorcycle");

        trackTypeSpeedMap.clear();
        defaultSpeedMap.clear();

        trackTypeSpeedMap.put("grade1", 20); // paved
        trackTypeSpeedMap.put("grade2", 15); // now unpaved - gravel mixed with ...
        trackTypeSpeedMap.put("grade3", 10); // ... hard and soft materials
        trackTypeSpeedMap.put("grade4", 5); // ... some hard or compressed materials
        trackTypeSpeedMap.put("grade5", 5); // ... no hard materials. soil/sand/grass

        avoidSet.add("motorway");
        avoidSet.add("trunk");
        avoidSet.add("motorroad");
        avoidSet.add("residential");

        preferSet.add("primary");
        preferSet.add("secondary");
        preferSet.add("tertiary");

        maxPossibleSpeed = 120;

        // autobahn
        defaultSpeedMap.put("motorway", 100);
        defaultSpeedMap.put("motorway_link", 70);
        defaultSpeedMap.put("motorroad", 90);
        // bundesstraße
        defaultSpeedMap.put("trunk", 80);
        defaultSpeedMap.put("trunk_link", 75);
        // linking bigger town
        defaultSpeedMap.put("primary", 65);
        defaultSpeedMap.put("primary_link", 60);
        // linking towns + villages
        defaultSpeedMap.put("secondary", 60);
        defaultSpeedMap.put("secondary_link", 50);
        // streets without middle line separation
        defaultSpeedMap.put("tertiary", 50);
        defaultSpeedMap.put("tertiary_link", 40);
        defaultSpeedMap.put("unclassified", 30);
        defaultSpeedMap.put("residential", 30);
        // spielstraße
        defaultSpeedMap.put("living_street", 5);
        defaultSpeedMap.put("service", 20);
        // unknown road
        defaultSpeedMap.put("road", 20);
        // forestry stuff
        defaultSpeedMap.put("track", 15);

        conditionalTagsInspector = new ConditionalTagsInspector(restrictions, restrictedValues, intendedValues);
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    /**
     * Define the place of the speedBits in the edge flags for car.
     */
    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        reverseSpeedEncoder = new EncodedDoubleValue("Reverse Speed", shift, speedBits, speedFactor,
                defaultSpeedMap.get("secondary"), maxPossibleSpeed);
        shift += reverseSpeedEncoder.getBits();

        priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1, 3, 7);
        shift += priorityWayEncoder.getBits();

        curvatureEncoder = new EncodedValue("Curvature", shift, 4, 1, 10, 10);
        shift += curvatureEncoder.getBits();

        return shift;
    }

    @Override
    public long acceptWay( OSMWay way )
    {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                String motorcycleTag = way.getTag("motorcycle");
                if (motorcycleTag == null)
                    motorcycleTag = way.getTag("motor_vehicle");

                if (motorcycleTag == null && !way.hasTag("foot") && !way.hasTag("bicycle") || "yes".equals(motorcycleTag))
                    return acceptBit | ferryBit;
            }
            return 0;
        }

        if ("track".equals(highwayValue))
        {
            String tt = way.getTag("tracktype");
            if (tt != null && !tt.equals("grade1"))
                return 0;
        }

        if (!defaultSpeedMap.containsKey(highwayValue))
            return 0;

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            return 0;

        String firstValue = way.getFirstPriorityTag(restrictions);
        if (!firstValue.isEmpty())
        {
            if (restrictedValues.contains(firstValue) && !conditionalTagsInspector.isRestrictedWayConditionallyPermitted(way))
                return 0;
            if (intendedValues.contains(firstValue))
                return acceptBit;
        }

        // do not drive street cars into fords
        if (isBlockFords() && ("ford".equals(highwayValue) || way.hasTag("ford")))
            return 0;

        // do not drive cars over railways (sometimes incorrectly mapped!)
        if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
            return 0;

        if (conditionalTagsInspector.isPermittedWayConditionallyRestricted(way))
            return 0;
        else
            return acceptBit;
    }

    @Override
    public long handleWayTags( OSMWay way, long allowed, long priorityFromRelation )
    {
        if (!isAccept(allowed))
            return 0;

        long encoded = 0;
        if (!isFerry(allowed))
        {
            // get assumed speed from highway type
            double speed = getSpeed(way);
            speed = applyMaxSpeed(way, speed);

            double maxMCSpeed = parseSpeed(way.getTag("maxspeed:motorcycle"));
            if (maxMCSpeed > 0 && maxMCSpeed < speed)
                speed = maxMCSpeed * 0.9;

            // limit speed to max 30 km/h if bad surface
            if (speed > 30 && way.hasTag("surface", badSurfaceSpeedMap))
                speed = 30;

            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if (isRoundabout)
                encoded = setBool(0, K_ROUNDABOUT, true);

            if (way.hasTag("oneway", oneways) || isRoundabout)
            {
                if (way.hasTag("oneway", "-1"))
                {
                    encoded = setReverseSpeed(encoded, speed);
                    encoded |= backwardBit;
                } else
                {
                    encoded = setSpeed(encoded, speed);
                    encoded |= forwardBit;
                }
            } else
            {
                encoded = setSpeed(encoded, speed);
                encoded = setReverseSpeed(encoded, speed);
                encoded |= directionBitMask;
            }

        } else
        {
            encoded = handleFerryTags(way, defaultSpeedMap.get("living_street"), defaultSpeedMap.get("service"), defaultSpeedMap.get("residential"));
            encoded |= directionBitMask;
        }

        // relations are not yet stored -> see BikeCommonFlagEncoder.defineRelationBits how to do this
        encoded = priorityWayEncoder.setValue(encoded, handlePriority(way, priorityFromRelation));

        // Set the curvature to the Maximum
        encoded = curvatureEncoder.setValue(encoded, 10);

        return encoded;
    }

    @Override
    public double getReverseSpeed( long flags )
    {
        return reverseSpeedEncoder.getDoubleValue(flags);
    }

    @Override
    public long setReverseSpeed( long flags, double speed )
    {
        if (speed < 0)
            throw new IllegalArgumentException("Speed cannot be negative: " + speed + ", flags:" + BitUtil.LITTLE.toBitString(flags));

        if (speed < speedEncoder.factor / 2)
            return setLowSpeed(flags, speed, true);

        if (speed > getMaxSpeed())
            speed = getMaxSpeed();

        return reverseSpeedEncoder.setDoubleValue(flags, speed);
    }

    @Override
    protected long setLowSpeed( long flags, double speed, boolean reverse )
    {
        if (reverse)
            return setBool(reverseSpeedEncoder.setDoubleValue(flags, 0), K_BACKWARD, false);

        return setBool(speedEncoder.setDoubleValue(flags, 0), K_FORWARD, false);
    }

    @Override
    public long flagsDefault( boolean forward, boolean backward )
    {
        long flags = super.flagsDefault(forward, backward);
        if (backward)
            return reverseSpeedEncoder.setDefaultValue(flags);

        return flags;
    }

    @Override
    public long setProperties( double speed, boolean forward, boolean backward )
    {
        long flags = super.setProperties(speed, forward, backward);
        if (backward)
            return setReverseSpeed(flags, speed);

        return flags;
    }

    @Override
    public long reverseFlags( long flags )
    {
        // swap access
        flags = super.reverseFlags(flags);

        // swap speeds 
        double otherValue = reverseSpeedEncoder.getDoubleValue(flags);
        flags = setReverseSpeed(flags, speedEncoder.getDoubleValue(flags));
        return setSpeed(flags, otherValue);
    }

    @Override
    public double getDouble( long flags, int key )
    {
        switch (key)
        {
            case PriorityWeighting.KEY:
                return (double) priorityWayEncoder.getValue(flags) / BEST.getValue();
            case MotorcycleFlagEncoder.CURVATURE_KEY:
                return (double) curvatureEncoder.getValue(flags) / 10;
            default:
                return super.getDouble(flags, key);
        }
    }

    private int handlePriority( OSMWay way, long relationFlags )
    {
        String highway = way.getTag("highway", "");
        if (avoidSet.contains(highway))
        {
            return PriorityCode.WORST.getValue();
        } else if (preferSet.contains(highway))
        {
            return PriorityCode.BEST.getValue();
        }

        return PriorityCode.UNCHANGED.getValue();
    }

    @Override
    public void applyWayTags( OSMWay way, EdgeIteratorState edge )
    {
        double speed = this.getSpeed(edge.getFlags());
        double roadDistance = edge.getDistance();
        double beelineDistance = getBeelineDistance(way);
        double bendiness = beelineDistance / roadDistance;

        bendiness = discriminateSlowStreets(bendiness, speed);
        bendiness = increaseBendinessImpact(bendiness);
        bendiness = correctErrors(bendiness);

        edge.setFlags(this.curvatureEncoder.setValue(edge.getFlags(), convertToInt(bendiness)));
    }

    private double getBeelineDistance( OSMWay way )
    {
        return way.getTag("estimated_distance", Double.POSITIVE_INFINITY);
    }

    /**
     * Streets that slow are not fun and probably in a town.
     */
    protected double discriminateSlowStreets( double bendiness, double speed )
    {
        if (speed < 51)
        {
            return 1;
        }
        return bendiness;
    }

    /**
     * A really small bendiness or a bendiness greater than 1 indicates an error in the calculation.
     * Just ignore them. We use bendiness > 1.2 since the beelineDistance is only approximated,
     * therefore it can happen on straight roads, that the beeline is longer than the road.
     */
    protected double correctErrors( double bendiness )
    {
        if (bendiness < 0.01 || bendiness > 1)
        {
            return 1;
        }
        return bendiness;
    }

    /**
     * A good bendiness should become a greater impact. A bendiness close to 1 should not be
     * changed.
     */
    protected double increaseBendinessImpact( double bendiness )
    {
        return (Math.pow(bendiness, 2));
    }

    @Override
    public boolean supports( Class<?> feature )
    {
        if (super.supports(feature))
            return true;

        if (CurvatureWeighting.class.isAssignableFrom(feature))
        {
            return true;
        }

        return PriorityWeighting.class.isAssignableFrom(feature);
    }

    protected int convertToInt( double bendiness )
    {
        bendiness = bendiness * 10;
        return (int) bendiness;
    }

    @Override
    public String toString()
    {
        return "motorcycle";
    }
}
