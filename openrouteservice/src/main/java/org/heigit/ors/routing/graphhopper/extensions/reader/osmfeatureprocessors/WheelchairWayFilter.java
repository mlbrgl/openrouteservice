package org.heigit.ors.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderWay;

import java.io.InvalidObjectException;

public class WheelchairWayFilter implements OSMFeatureFilter {
    private final OSMAttachedSidewalkProcessor osmAttachedSidewalkProcessor;

    private Way osmWay;

    public WheelchairWayFilter() {
        super();
        osmAttachedSidewalkProcessor = new OSMAttachedSidewalkProcessor();
    }

    @Override
    public void assignFeatureForFiltering(ReaderElement element) throws InvalidObjectException {
        if(element instanceof ReaderWay readerWay) {

            if (osmAttachedSidewalkProcessor.hasSidewalkInfo(readerWay)) {
                this.osmWay = new WheelchairSidewalkWay(readerWay);
            } else {
                this.osmWay = new WheelchairSeparateWay(readerWay);
            }
        } else {
            throw new InvalidObjectException("Wheelchair Filtering can only be applied to ways");
        }
    }

    @Override
    public boolean accept() {
        return osmWay.isPedestrianised();
    }

    @Override
    public ReaderElement prepareForProcessing() {
        osmWay.prepare();

        return osmWay.getReaderWay();
    }

    @Override
    public boolean isWayProcessingComplete() {
        return osmWay.hasWayBeenFullyProcessed();
    }

}
