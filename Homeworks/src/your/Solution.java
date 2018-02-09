package your;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by Amos on 2018/2/8.
 */
public class Solution {

    private TreeSet<Interval> internalData;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Interval step: internalData) {
            sb.append(step.toString()).append(",");
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        return sb.toString();
    }

    public Solution() {
        this.internalData = new TreeSet<>(new Comparator<Interval>() {
            @Override
            public int compare(Interval o1, Interval o2) {
                return Integer.compare(o1.start, o2.start);
            }
        });
    }

    // 1 for adding, -1 for removing
    public String process(int[] array) throws InvalidRemovalException {
        if (!preProcessInput(array)) {
            return this.toString();
        }
        if (internalData.size() == 0 ) {
            if (array[0] == -1) {
                throw new InvalidRemovalException("List is empty, cannot remove.");
            } else {
                internalData.add(new Interval(array[1], array[2]));
            }
        } else {
            if (array[0] == 1) {
                // Adding interval
                processForAddition(array);
            } else {
                // Removing interval
                processForRemoval(array);
            }
        }
        return this.toString();
    }

    private void processForAddition(int[] array) {

    }


    
    private void processForRemoval(int[] array) {
        Integer leftHelper = null;
        Integer rightHelper = null;

        Interval constraintInterval = new Interval(array[1], array[2]);
        Interval floorInterval = internalData.floor(constraintInterval);
        if (floorInterval != null) {
            // There exist a Node has "start" smaller or equal to constraint.start
            if (floorInterval.start < constraintInterval.start) {
                if (floorInterval.end > constraintInterval.start) {
                    // [0,20] [1,X] => [0,1] [1,20] upperBound=X
                    // Helper used to finally add one Interval back
                    leftHelper = floorInterval.start; rightHelper = constraintInterval.start;
                    floorInterval.start = constraintInterval.start;
                    // Checking nodes including floor and after with upperBound X
                    loopForRemoval(constraintInterval, floorInterval, true, 0);
                } else if (floorInterval.end <= constraintInterval.start) {
                    // [0,1] [3,X] => Ignore floorInterval && there's no interval such as [2,...]
                    // Checking nodes after floor with upperBound X
                    loopForRemoval(constraintInterval, floorInterval, false, 0);
                }
            } else {
                // [0,20] [0, X] => Compare X and 20, then proceed on while-loop
                if (constraintInterval.end < floorInterval.end) {
                    floorInterval.start = constraintInterval.end;
                } else if (constraintInterval.end == floorInterval.end) {
                    internalData.remove(floorInterval);
                } else {
                    // Checking nodes including floor and after with upperBound X
                    loopForRemoval(constraintInterval, floorInterval, true, 0);
                }
            }
        } else {
            // Checking nodes from the beginning with upperBound X
            loopForRemoval(constraintInterval, null, false, 1);
        }
        // Finally take care of adding Interval back
        if (leftHelper != null) {
            internalData.add(new Interval(leftHelper, rightHelper));
        }
    }

    private void loopForRemoval(Interval constraintInterval, Interval floorInterval, boolean shouldIncludeFloor, int initialCheckFlag) {
        Iterator<Interval> iterator = internalData.iterator();
        int shouldCheck = initialCheckFlag;
        while (iterator.hasNext()) {
            Interval step = iterator.next();
            if (step.equals(floorInterval) && shouldIncludeFloor) {
                shouldCheck = 1;
            }
            if (shouldCheck == 1) {
                if (constraintInterval.end >= step.end) {
                    // Remove step entirely
                    iterator.remove();
                } else if (constraintInterval.end <= step.start) {
                    // No more Intervals to remove
                    break;
                } else {
                    // Update step Interval's start value
                    step.start = constraintInterval.end;
                    break;
                }
            }
            if (step.equals(floorInterval) && !shouldIncludeFloor) {
                shouldCheck = 1;
            }
        }
    }

    private boolean preProcessInput(int[] step) {
        if (step.length == 3) {
            if (step[0] == -1 || step[0] == 1) {
                return step[1] < step[2];
            }
        }
        return false;
    }


    public static void main(String[] args) {
        Solution solution = new Solution();
        try {
            System.out.println(solution.process(new int[]{1, 1, 20}));
            System.out.println(solution.process(new int[]{-1, -2, -1}));
            System.out.println(solution.process(new int[]{-1, -1, 1}));
            System.out.println(solution.process(new int[]{-1, 1, 2}));
            System.out.println(solution.process(new int[]{-1, 3, 4}));
            System.out.println(solution.process(new int[]{-1, 23, 24}));
            System.out.println(solution.process(new int[]{-1, 20, 21}));
            System.out.println(solution.process(new int[]{-1, 19, 20}));
            System.out.println(solution.process(new int[]{-1, 17, 18}));
            System.out.println(solution.process(new int[]{-1, 1, 7}));
            System.out.println(solution.process(new int[]{-1, 10, 21}));
            System.out.println(solution.process(new int[]{-1, 8, 9}));
            System.out.println(solution.process(new int[]{-1, 1, 11}));




            System.out.println("abc");
        } catch (InvalidRemovalException e) {
            e.printStackTrace();
        }
    }


}
