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
        Integer leftHelper = null;
        Integer rightHelper = null;

        Interval expandInterval = new Interval(array[1], array[2]);
        Interval floorInterval = internalData.floor(expandInterval);
        if (floorInterval != null) {
            // There exist a Node has "start" smaller or equal to constraint.start
            if (floorInterval.start < expandInterval.start) {
                if (floorInterval.end > expandInterval.start) {
                    // [0,20] [1,X] => check the value of X with 20
                    if (expandInterval.end > floorInterval.end) {
                        // [0, 20] [1, X], X > 20
                        // Start the combining loop from floorInterval, with threshold X, combining to floorInterval
                        int originalFloorRight = floorInterval.end;

                        Iterator<Interval> iterator = internalData.iterator();
                        int shouldCheck = 0;
                        while (iterator.hasNext()) {
                            Interval step = iterator.next();
                            if (shouldCheck == 1) {
                                if (expandInterval.end >= step.end) {
                                    // Remove step entirely
                                    iterator.remove();
                                } else if (expandInterval.end < step.start) {
                                    // No more Intervals to combine
                                    floorInterval.end = expandInterval.end;
                                    break;
                                } else {
                                    // Combine the step and return
                                    floorInterval.end = step.end;
                                    iterator.remove();
                                    break;
                                }
                            }
                            if (step.equals(floorInterval)) {
                                shouldCheck = 1;
                            }
                        }
                        if (floorInterval.end == originalFloorRight) {
                            // End of List reached
                            floorInterval.end = expandInterval.end;
                        }

                    } else {
                        // Nothing changes
                    }
                } else if (floorInterval.end <= expandInterval.start) {
                    // [0,1] [3,X] => Ignore floorInterval && there's no interval such as [2,...]
                    // Start the combining loop from floorInterval's next, with threshold X, combining to expandInterval

                    Iterator<Interval> iterator = internalData.iterator();
                    int shouldCheck = 0;
                    while (iterator.hasNext()) {
                        Interval step = iterator.next();
                        if (shouldCheck == 1) {
                            if (expandInterval.end >= step.end) {
                                // Remove step entirely
                                iterator.remove();
                            } else if (expandInterval.end < step.start) {
                                // No more Intervals to combine
                                // Add expandInterval to Map
                                break;
                            } else {
                                // Combine the step and return
                                expandInterval.end = step.end;
                                iterator.remove();
                                break;
                            }
                        }
                        if (step.equals(floorInterval)) {
                            shouldCheck = 1;
                        }
                    }
                    // Don't need to take care of largest situation
                    internalData.add(expandInterval);




                }
            } else {
                // [0,20] [0, X] => Compare X and 20, then proceed on combining loop
                if (expandInterval.end > floorInterval.end) {
                    // Start the combining loop from floorInterval, with threshold X, combining to floorInterval
                    int originalFloorRight = floorInterval.end;

                    Iterator<Interval> iterator = internalData.iterator();
                    int shouldCheck = 0;
                    while (iterator.hasNext()) {
                        Interval step = iterator.next();
                        if (shouldCheck == 1) {
                            if (expandInterval.end >= step.end) {
                                // Remove step entirely
                                iterator.remove();
                            } else if (expandInterval.end < step.start) {
                                // No more Intervals to combine
                                floorInterval.end = expandInterval.end;
                                break;
                            } else {
                                // Combine the step and return
                                floorInterval.end = step.end;
                                iterator.remove();
                                break;
                            }
                        }
                        if (step.equals(floorInterval)) {
                            shouldCheck = 1;
                        }
                    }
                    if (floorInterval.end == originalFloorRight) {
                        // End of List reached
                        floorInterval.end = expandInterval.end;
                    }
                } else {
                    // nothing changes
                }
            }
        } else {
            // Start the combining loop from the beginning with threshold X, combining to expandInterval
            Iterator<Interval> iterator = internalData.iterator();
            int shouldCheck = 1;
            while (iterator.hasNext()) {
                Interval step = iterator.next();
                if (shouldCheck == 1) {
                    if (expandInterval.end >= step.end) {
                        // Remove step entirely
                        iterator.remove();
                    } else if (expandInterval.end < step.start) {
                        // No more Intervals to combine
                        // Add expandInterval to Map
                        break;
                    } else {
                        // Combine the step and return
                        expandInterval.end = step.end;
                        iterator.remove();
                        break;
                    }
                }
            }
            // Don't need to take care of largest situation
            internalData.add(expandInterval);
        }
    }

    private void loopForAddition(Interval expandInterval, Interval floorInterval, boolean shouldIncludeFloor, boolean shouldCombineFloor, int initialCheckFlag) {

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

            System.out.println(solution.process(new int[]{1, 1, 5}));
            System.out.println(solution.process(new int[]{1, 0, 1}));
            System.out.println(solution.process(new int[]{1, 7, 11}));
            System.out.println(solution.process(new int[]{1, 2, 3}));
            System.out.println(solution.process(new int[]{1, 8, 9}));
            System.out.println(solution.process(new int[]{1, 14, 19}));
            System.out.println(solution.process(new int[]{1, 28, 29}));
            System.out.println(solution.process(new int[]{1, 38, 39}));
            System.out.println(solution.process(new int[]{1, 0, 15}));
            System.out.println(solution.process(new int[]{1, 33, 43}));
            System.out.println(solution.process(new int[]{-1, 20, 23}));
            System.out.println(solution.process(new int[]{-1, 2, 11}));




            System.out.println("abc");
        } catch (InvalidRemovalException e) {
            e.printStackTrace();
        }
    }


}
