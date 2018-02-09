package your;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by Amos on 2018/2/8.
 */
public class Solution {

    private TreeSet<Interval> internalData;


    public Solution() {
        this.internalData = new TreeSet<>(new Comparator<Interval>() {
            @Override
            public int compare(Interval o1, Interval o2) {
                return Integer.compare(o1.start, o2.start);
            }
        });
    }

    // 1 for adding, -1 for removing
    public TreeSet<Interval> process(int[] array) throws InvalidRemovalException {
        if (!preProcessInput(array)) {
            return internalData;
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

            } else {
                // Removing interval
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
                            Iterator<Interval> iterator = internalData.iterator();
                            int selectFlag = 0;
                            while (iterator.hasNext()) {
                                Interval step = iterator.next();
                                if (step.equals(floorInterval)) {
                                    selectFlag = 1;
                                    if (constraintInterval.end >= step.end) {
                                        // Remove step entirely
                                        iterator.remove();
                                    } else if (constraintInterval.end <= step.start) {
                                        // No more Intervals to remove
                                        selectFlag = 2;
                                    } else {
                                        // Update step Interval's start value
                                        step.start = constraintInterval.end;
                                        selectFlag = 2;
                                    }
                                } else {
                                    if (selectFlag == 1) {
                                        if (constraintInterval.end >= step.end) {
                                            // Remove step entirely
                                            iterator.remove();
                                        } else if (constraintInterval.end <= step.start) {
                                            // No more Intervals to remove
                                            selectFlag = 2;
                                        } else {
                                            // Update step Interval's start value
                                            step.start = constraintInterval.end;
                                            selectFlag = 2;
                                        }
                                    } else if (selectFlag == 2) {
                                        break;
                                    }
                                }
                            }
                        } else if (floorInterval.end <= constraintInterval.start) {

                            // Remove all subsequent nodes until reach current.end
                            Iterator<Interval> iterator = internalData.iterator();
                            int beginMarking = 0;
                            while (iterator.hasNext()) {
                                Interval stepInterval = iterator.next();
                                if (stepInterval.equals(floorInterval)) {
                                    beginMarking = 1;
                                } else {
                                    if (beginMarking == 1) {
                                        // Compare current.end
                                        if (constraintInterval.end >= stepInterval.end) {
                                            // Remove this Interval
                                            iterator.remove();
                                        } else if (constraintInterval.end <= stepInterval.start) {
                                            // Break the loop
                                            beginMarking = 2;
                                        } else {
                                            // Reset stepInterval
                                            stepInterval.start = constraintInterval.end;
                                            beginMarking = 2;
                                        }
                                    } else if (beginMarking == 2) {
                                        break;
                                    }
                                }
                            }


                        }

                    } else {
                        // previous.start == current.start
                        if (constraintInterval.end < floorInterval.end) {
                            floorInterval.start = constraintInterval.end;
                        } else if (constraintInterval.end == floorInterval.end) {
                            internalData.remove(floorInterval);
                        } else {
                            // Remove previous and all subsequent nodes until reach current.end
                            Iterator<Interval> iterator = internalData.iterator();
                            int beginMarking = 0;
                            while (iterator.hasNext()) {
                                Interval stepInterval = iterator.next();
                                if (stepInterval.equals(floorInterval)) {
                                    beginMarking = 1;
                                    iterator.remove();
                                } else {
                                    if (beginMarking == 1) {
                                        // Compare current.end
                                        if (constraintInterval.end >= stepInterval.end) {
                                            // Remove this Interval
                                            iterator.remove();
                                        } else if (constraintInterval.end <= stepInterval.start) {
                                            // Break the loop
                                            beginMarking = 2;
                                        } else {
                                            // Reset stepInterval
                                            stepInterval.start = constraintInterval.end;
                                            beginMarking = 2;
                                        }
                                    } else if (beginMarking == 2) {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // current Interval has smallest start time, apply iterator directly
                    Iterator<Interval> iterator = internalData.iterator();
                    int beginMarking = 1;
                    while (iterator.hasNext()) {
                        Interval stepInterval = iterator.next();
                        if (beginMarking == 1) {
                            // Compare current.end
                            if (constraintInterval.end >= stepInterval.end) {
                                // Remove this Interval
                                iterator.remove();
                            } else if (constraintInterval.end <= stepInterval.start) {
                                // Break the loop
                                beginMarking = 2;
                            } else {
                                // Reset stepInterval
                                stepInterval.start = constraintInterval.end;
                                beginMarking = 2;
                            }
                        } else {
                            break;
                        }
                    }
                }

                if (leftHelper != null) {
                    internalData.add(new Interval(leftHelper, rightHelper));
                }



            }
        }

        return internalData;
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
            solution.process(new int[]{1, 1, 20});
            solution.process(new int[]{-1, -2, -1});
            solution.process(new int[]{-1, -1, 1});
            solution.process(new int[]{-1, 1, 2});
            solution.process(new int[]{-1, 3, 4});
            solution.process(new int[]{-1, 23, 24});
            solution.process(new int[]{-1, 20, 21});
            solution.process(new int[]{-1, 19, 20});
            solution.process(new int[]{-1, 17, 18});
            solution.process(new int[]{-1, 1, 7});
            solution.process(new int[]{-1, 10, 21});
            solution.process(new int[]{-1, 8, 9});
//            solution.process(new int[]{-1, 1, 11});



            System.out.println("abc");
        } catch (InvalidRemovalException e) {
            e.printStackTrace();
        }
    }


}
