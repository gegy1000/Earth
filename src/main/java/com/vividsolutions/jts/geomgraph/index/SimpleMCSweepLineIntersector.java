/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.vividsolutions.jts.geomgraph.index;

/**
 * @version 1.7
 */

import com.vividsolutions.jts.geomgraph.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds all intersections in one or two sets of edges,
 * using an x-axis sweepline algorithm in conjunction with Monotone Chains.
 * While still O(n^2) in the worst case, this algorithm
 * drastically improves the average-case time.
 * The use of MonotoneChains as the items in the index
 * seems to offer an improvement in performance over a sweep-line alone.
 *
 * @version 1.7
 */
public class SimpleMCSweepLineIntersector
        extends EdgeSetIntersector {

    List events = new ArrayList();
    // statistics information
    int nOverlaps;

    /**
     * A SimpleMCSweepLineIntersector creates monotone chains from the edges
     * and compares them using a simple sweep-line along the x-axis.
     */
    public SimpleMCSweepLineIntersector() {
    }

    @Override
    public void computeIntersections(List edges, SegmentIntersector si, boolean testAllSegments) {
        if (testAllSegments) {
            this.add(edges, null);
        } else {
            this.add(edges);
        }
        this.computeIntersections(si);
    }

    @Override
    public void computeIntersections(List edges0, List edges1, SegmentIntersector si) {
        this.add(edges0, edges0);
        this.add(edges1, edges1);
        this.computeIntersections(si);
    }

    private void add(List edges) {
        for (Object edge1 : edges) {
            Edge edge = (Edge) edge1;
            // edge is its own group
            this.add(edge, edge);
        }
    }

    private void add(List edges, Object edgeSet) {
        for (Object edge1 : edges) {
            Edge edge = (Edge) edge1;
            this.add(edge, edgeSet);
        }
    }

    private void add(Edge edge, Object edgeSet) {
        MonotoneChainEdge mce = edge.getMonotoneChainEdge();
        int[] startIndex = mce.getStartIndexes();
        for (int i = 0; i < startIndex.length - 1; i++) {
            MonotoneChain mc = new MonotoneChain(mce, i);
            SweepLineEvent insertEvent = new SweepLineEvent(edgeSet, mce.getMinX(i), null, mc);
            this.events.add(insertEvent);
            this.events.add(new SweepLineEvent(edgeSet, mce.getMaxX(i), insertEvent, mc));
        }
    }

    /**
     * Because Delete Events have a link to their corresponding Insert event,
     * it is possible to compute exactly the range of events which must be
     * compared to a given Insert event object.
     */
    private void prepareEvents() {
        Collections.sort(this.events);
        for (int i = 0; i < this.events.size(); i++) {
            SweepLineEvent ev = (SweepLineEvent) this.events.get(i);
            if (ev.isDelete()) {
                ev.getInsertEvent().setDeleteEventIndex(i);
            }
        }
    }

    private void computeIntersections(SegmentIntersector si) {
        this.nOverlaps = 0;
        this.prepareEvents();

        for (int i = 0; i < this.events.size(); i++) {
            SweepLineEvent ev = (SweepLineEvent) this.events.get(i);
            if (ev.isInsert()) {
                this.processOverlaps(i, ev.getDeleteEventIndex(), ev, si);
            }
        }
    }

    private void processOverlaps(int start, int end, SweepLineEvent ev0, SegmentIntersector si) {
        MonotoneChain mc0 = (MonotoneChain) ev0.getObject();
        /**
         * Since we might need to test for self-intersections,
         * include current insert event object in list of event objects to test.
         * Last index can be skipped, because it must be a Delete event.
         */
        for (int i = start; i < end; i++) {
            SweepLineEvent ev1 = (SweepLineEvent) this.events.get(i);
            if (ev1.isInsert()) {
                MonotoneChain mc1 = (MonotoneChain) ev1.getObject();
                // don't compare edges in same group
                // null group indicates that edges should be compared
                if (ev0.edgeSet == null || (ev0.edgeSet != ev1.edgeSet)) {
                    mc0.computeIntersections(mc1, si);
                    this.nOverlaps++;
                }
            }
        }
    }
}
