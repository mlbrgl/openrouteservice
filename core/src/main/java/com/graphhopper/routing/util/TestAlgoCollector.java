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

import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Karich
 */
public class TestAlgoCollector
{
    private final String name;
    private final DistanceCalc distCalc = new DistanceCalcEarth();
    public List<String> errors = new ArrayList<String>();

    public TestAlgoCollector( String name )
    {
        this.name = name;
    }

    public TestAlgoCollector assertDistance( AlgorithmPreparation prepare, List<QueryResult> queryList, OneRun oneRun )
    {
        List<Path> viaPaths = new ArrayList<Path>();
        for (int i = 0; i < queryList.size() - 1; i++)
        {
            Path path = prepare.createAlgo().calcPath(queryList.get(i), queryList.get(i + 1));
            viaPaths.add(path);
        }
        PathMerger pathMerger = new PathMerger().
                setCalcPoints(true).
                setSimplifyRequest(false).
                setEnableInstructions(true);
        GHResponse rsp = new GHResponse();
        pathMerger.doWork(rsp, viaPaths);

        if (!rsp.isFound())
        {
            errors.add(prepare + " returns no path! expected distance: " + rsp.getDistance()
                    + ", expected points: " + oneRun + ". " + queryList);
            return this;
        }

        PointList pointList = rsp.getPoints();
        double tmpDist = pointList.calcDistance(distCalc);
        if (Math.abs(rsp.getDistance() - tmpDist) > .9)
        {
            errors.add(prepare + " path.getDistance was  " + rsp.getDistance()
                    + "\t pointList.calcDistance was " + tmpDist + "\t (expected points " + oneRun.getLocs()
                    + ", expected distance " + oneRun.getDistance() + ") " + queryList);
        }

        if (Math.abs(rsp.getDistance() - oneRun.getDistance()) > .9)
        {
            errors.add(prepare + " returns path not matching the expected distance of " + oneRun.getDistance()
                    + "\t Returned was " + rsp.getDistance() + "\t (expected points " + oneRun.getLocs()
                    + ", was " + pointList.getSize() + ") " + queryList);
        }

        // There are real world instances where A-B-C is identical to A-C (in meter precision).
        if (Math.abs(pointList.getSize() - oneRun.getLocs()) > 1)
        {
            errors.add(prepare + " returns path not matching the expected points of " + oneRun.getLocs()
                    + "\t Returned was " + pointList.getSize() + "\t (expected distance " + oneRun.getDistance()
                    + ", was " + rsp.getDistance() + ") " + queryList);
        }
        return this;
    }

    void queryIndex( Graph g, LocationIndex idx, double lat, double lon, double expectedDist )
    {
        QueryResult res = idx.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
        if (!res.isValid())
        {
            errors.add("node not found for " + lat + "," + lon);
            return;
        }

        GHPoint found = res.getSnappedPoint();
        double dist = distCalc.calcDist(lat, lon, found.lat, found.lon);
        if (Math.abs(dist - expectedDist) > .1)
        {
            errors.add("queried lat,lon=" + (float) lat + "," + (float) lon
                    + " (found: " + (float) found.lat + "," + (float) found.lon + ")"
                    + "\n   expected distance:" + expectedDist + ", but was:" + dist);
        }
    }

    @Override
    public String toString()
    {
        String str = "";
        str += "FOUND " + errors.size() + " ERRORS.\n";
        for (String s : errors)
        {
            str += s + ".\n";
        }
        return str;
    }

    void printSummary()
    {
        if (errors.size() > 0)
        {
            System.out.println("\n-------------------------------\n");
            System.out.println(toString());
        } else
        {
            System.out.println("SUCCESS for " + name + "!");
        }
    }

    public static class OneRun
    {
        private final List<AssumptionPerPath> assumptions = new ArrayList<AssumptionPerPath>();

        public OneRun()
        {
        }

        public OneRun( double fromLat, double fromLon, double toLat, double toLon, double dist, int locs )
        {
            add(fromLat, fromLon, 0, 0);
            add(toLat, toLon, dist, locs);
        }

        public OneRun add( double lat, double lon, double dist, int locs )
        {
            assumptions.add(new AssumptionPerPath(lat, lon, dist, locs));
            return this;
        }

        public int getLocs()
        {
            int sum = 0;
            for (AssumptionPerPath as : assumptions)
            {
                sum += as.locs;
            }
            return sum;
        }

        public void setLocs( int index, int locs )
        {
            assumptions.get(index).locs = locs;
        }

        public double getDistance()
        {
            double sum = 0;
            for (AssumptionPerPath as : assumptions)
            {
                sum += as.distance;
            }
            return sum;
        }

        public void setDistance( int index, double dist )
        {
            assumptions.get(index).distance = dist;
        }

        public List<QueryResult> getList( LocationIndex idx, EdgeFilter edgeFilter )
        {
            List<QueryResult> qr = new ArrayList<QueryResult>();
            for (AssumptionPerPath or : assumptions)
            {
                qr.add(idx.findClosest(or.lat, or.lon, edgeFilter));
            }
            return qr;
        }

        @Override
        public String toString()
        {
            return assumptions.toString();
        }
    }

    static class AssumptionPerPath
    {
        double lat, lon;
        int locs;
        double distance;

        public AssumptionPerPath( double lat, double lon, double distance, int locs )
        {
            this.lat = lat;
            this.lon = lon;
            this.locs = locs;
            this.distance = distance;
        }

        @Override
        public String toString()
        {
            return lat + ", " + lon + ", locs:" + locs + ", dist:" + distance;
        }
    }
}
