package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {
        //  Implement this method as specified by constructing and returning a new PolyLine
        //  representing the desired line segment.  This can be done with one statement.
        //  Test immediately with `testLiveWireEmpty()`, and think about how the test might change
        //  for non-empty selections (see task 2D).
        return new PolyLine(lastPoint(), p);
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
        // Create a line segment from the end of the previous segment (or from the starting
        //  point if this is only the 2nd point) to the current point `p`, then append that segment
        //  to the current selection path.  This can be done with one statement, similar to
        //  `liveWire()` above.
        //  Test immediately with `testAppend()` and `testFinishSelection()`.
        selection.add(new PolyLine(lastPoint(), p));
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

        //In the case that chosen point to move is the starting point of selection
        if(index == 0){
            //Change selection start point
            start = newPos;
            //Declare new Polyline to be inserted
            Point secondPoint = selection.getFirst().end();
            PolyLine newStart = new PolyLine(newPos, secondPoint);
            selection.set(index, newStart);
            PolyLine newEnd = new PolyLine(selection.getLast().start(), newPos);
            //Set the new Polyline into selection
            selection.set(selection.size()-1, newEnd);
            //Notify listeners that the "selection" property has changed
            propSupport.firePropertyChange("selection", null, selection());
        }else if(index == selection.size()-1){
            //In the case that chosen point is the ending of selection
// Update the second last segment to end at the new position
            Point previousEndStart = selection.get(selection.size() - 2).start();
            PolyLine newSecondLast = new PolyLine(previousEndStart, newPos);
            selection.set(selection.size() - 2, newSecondLast);

            // Update the last segment to start from the new position and connect to the start
            PolyLine newEnd = new PolyLine(newPos, selection.getFirst().start());
            selection.set(selection.size() - 1, newEnd);
            //Notify listeners that the "selection" property has changed
            propSupport.firePropertyChange("selection", null, selection());
        }else{
            //In the case that chosen point is not the starting of selection
            Point previousStartingPoint = selection.get(index - 1).start();
            //Declare new Polyline to be inserted
            PolyLine previousLine = new PolyLine(previousStartingPoint, newPos);
            Point nextEndPoint = selection.get(index).end();
            PolyLine nextLine = new PolyLine(newPos, nextEndPoint);
            //Set the new Polyline into selection
            selection.set(index - 1, previousLine);
            selection.set(index, nextLine);
            //Notify listeners that the "selection" property has changed
            propSupport.firePropertyChange("selection", null, selection());
        }


    }
}
