package ch.ethz.ikg.assignment1.Analysis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements the dynamic time warping algorithm. This algorithm aligns multiple trajectories
 * by a non-linear warping of time, which minimizes the distances between points.
 */
public class DynamicTimeWarp {
    /**
     * Computes the dynamic time warping between two trajectories.
     *
     * @param source The source trajectory, given as a list of trackpoints.
     * @param target The target trajectory, given as a list of trackpoints.
     * @return A {@link DTWResult}, which encapsulates both the similarity of the trajectories
     * as well as the mapping of target points to source points.
     */
    public static DTWResult compute(List<Trackpoint> source, List<Trackpoint> target) {
        // Set up some variables.
        int srcLength = source.size();
        int tarLength = target.size();
        double[][] costs = new double[srcLength + 1][tarLength + 1];

        // Initialize the 0 column and row to infinity.
        for (int i = 0; i <= srcLength; i++) {
            costs[i][0] = Double.POSITIVE_INFINITY;
        }
        for (int j = 0; j <= tarLength; j++) {
            costs[0][j] = Double.POSITIVE_INFINITY;
        }
        costs[0][0] = 0.0;

        // Run the actual algorithm, building up a (total) cost matrix.
        for (int i = 1; i <= srcLength; i++) {
            for (int j = 1; j <= tarLength; j++) {
                costs[i][j] = source.get(i - 1).distance(target.get(j - 1)) + Math.min(Math.min(costs[i - 1][j], costs[i - 1][j - 1]), costs[i][j - 1]);
            }
        }

        // Initialize some variables for the backpropagation through the matrix. This will yield
        // the mapping of target points to source points.
        List<List<Integer>> path = new LinkedList<>();

        // Run the algorithm, which simply tracks backwards through the matrix.
        int lastTargetNode = tarLength;
        for (int i = srcLength; i > 0; i--) {
            List<Integer> pts = new LinkedList<>();
            for (int j = lastTargetNode; j > 0; j--) {
                double left = costs[i - 1][j];
                double down = costs[i][j - 1];
                double diag = costs[i - 1][j - 1];
                if (left < down && left < diag) {
                    lastTargetNode = j;
                    break;
                } else if (down < left && down < diag) {
                    pts.add(j - 1);
                } else if (diag < left && diag < down) {
                    pts.add(j - 1);
                    lastTargetNode = j - 1;
                    break;
                }
            }
            // Since we're starting in the "bottom right", we need to reverse everything in the end.
            Collections.reverse(pts);
            path.add(pts);
        }
        Collections.reverse(path);

        // Return everything as a bundled DTWResult.
        return new DTWResult(costs[srcLength][tarLength] / (srcLength + tarLength), path);
    }

    /**
     * Wraps the result of a dynamic time warping.
     */
    public static class DTWResult {
        private double cost;
        private List<List<Integer>> path;

        /**
         * Constructor.
         *
         * @param cost The cost, or similarity value, between two trajectories.
         * @param path The path, or mapping between source and target trajectory.
         */
        DTWResult(double cost, List<List<Integer>> path) {
            this.cost = cost;
            this.path = path;
        }

        /**
         * Gets the cost, or similarity value of the result.
         *
         * @return The similarity value between trajectory one and two.
         */
        public double getCost() {
            return cost;
        }

        /**
         * Gets the path, or mapping, between trajectory one and two.
         *
         * @return The mapping between all trackpoints.
         */
        public List<List<Integer>> getPath() {
            return path;
        }
    }
}
