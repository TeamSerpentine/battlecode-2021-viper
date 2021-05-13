package viper1.utility;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static viper1.utility.Constants.SPAWNABLE_ROBOT;

public class Utility {
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    public static Direction randomDirection() {
        Direction[] directions = Direction.allDirections();
        return directions[(int) (Math.random() * (directions.length - 1))];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    public static RobotType randomSpawnableRobotType() {
        return SPAWNABLE_ROBOT[(int) (Math.random() * SPAWNABLE_ROBOT.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @param rc  RobotController
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMove(Direction dir, RobotController rc) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get a list of all map locations that can be sensed by the robot
     *
     * @param rc RobotController
     * @return ll map locations that can be sensed
     */
    public static List<MapLocation> getMapLocationsInSenseRadius(RobotController rc) {
        return getMapLocationsInRadius(rc, rc.getType().sensorRadiusSquared);
    }

    /**
     * Get a list of all map locations within a squared radius
     *
     * @param rc     RobotController
     * @param radius Squared radius in which to return all locations
     * @return List<MapLocation> All map locations within the squared radius
     */
    public static List<MapLocation> getMapLocationsInRadius(RobotController rc, int radius) {
        MapLocation botLocation = rc.getLocation();
        // TODO Implement
        return new ArrayList<>();
    }

    /**
     * Determine next direction to move in to go to destination.
     *
     * @param rc          RobotController
     * @param destination Destination to move to
     * @return Direction to move in
     */
    public static Direction pathfindToTarget(RobotController rc, MapLocation destination) throws GameActionException {
        return pathfindToTargetOptimal(rc, destination, 2*2);
    }

    /**
     * Determine next direction to move in to go to destination in a straight line.
     *
     * @param rc          RobotController
     * @param destination Destination to move to
     * @return Direction to move in
     */
    public static Direction pathfindToTargetStraight(RobotController rc, MapLocation destination) {
        return rc.getLocation().directionTo(destination);
    }

    /**
     * Determine next direction to move in to go to destination with simple heuristics like walking around HQs.
     *
     * @param rc          RobotController
     * @param destination Destination to move to
     * @return Direction to move in
     */
    public static Direction pathfindToTargetHeuristic(RobotController rc, MapLocation destination)
            throws GameActionException {
        Direction direction = rc.getLocation().directionTo(destination);
        MapLocation nextLocation = rc.getLocation().add(direction);
        Direction startDirection = direction;
        while (!rc.canSenseLocation(nextLocation) || rc.senseRobotAtLocation(nextLocation) != null) {
            // Make sure not to keep spinning
            if (direction.rotateRight() == startDirection) {
                break;
            }

            // Since we can't go in `direction`, try 1 further clockwise
            direction = direction.rotateRight();
            nextLocation = rc.getLocation().add(direction);
        }

        return direction;
    }

    public static Direction pathfindToTargetOptimal(RobotController rc, MapLocation destination)
            throws GameActionException {
        return pathfindToTargetOptimal(rc, destination, rc.getType().sensorRadiusSquared);
    }

    public static Direction pathfindToTargetOptimal(RobotController rc, MapLocation destination, int rangeSquared)
            throws GameActionException {
        // Make sure the range is not bigger than the range we can sense
        int maxSenseRangeSquared = rc.getType().sensorRadiusSquared;
        if (rangeSquared > maxSenseRangeSquared) {
            rangeSquared = maxSenseRangeSquared;
        }

        int rad1d = (int) Math.ceil(Math.sqrt(rangeSquared));
        MapLocation location = rc.getLocation();
        class LocInfo {
            int ix; // x index in map array
            int iy; // y index in map array
            double weight; // Higher for harder to pass
            double distance = Double.MAX_VALUE; // Distance to `closestLoc`
            boolean checked = false; // Whether this has been checked in Dijkstra already
        }

        // LocInfo for each sensible cell (and some squares around it in a square)
        LocInfo[][] map = new LocInfo[rad1d*2+1][rad1d*2+1];

        // Location that is sensible that is closest to the destination
        LocInfo closestLoc = null;
        int closestDist = Integer.MAX_VALUE;

        // Fill the map and find closestLoc
        for (int dx = -rad1d; dx <= rad1d; dx++) {
            for (int dy = -rad1d; dy <= rad1d; dy++) {
                LocInfo locInfo = new LocInfo();
                locInfo.ix = dx + rad1d;
                locInfo.iy = dy + rad1d;
                MapLocation senseLoc = location.translate(dx, dy);
                if (rc.canSenseLocation(senseLoc) && rc.senseRobotAtLocation(senseLoc) == null) {
                    // We can sense this location and there's no robot there
                    // TODO Properly determine how many turns it takes to pass through this square
                    locInfo.weight = 1. / rc.sensePassability(senseLoc);

                    int dist = senseLoc.distanceSquaredTo(destination);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestLoc = locInfo;
                    }
                } else {
                    locInfo.weight = 100000000; // A lot, practically impassable (but avoiding overflows)
                }
                map[locInfo.ix][locInfo.iy] = locInfo;
            }
        }

        // If we can't sense any location, we can't do much, so just move with heuristics
        if (closestLoc == null) {
            return pathfindToTargetHeuristic(rc, destination);
        }

        // Perform pathfinding using the map from current position to sensible location closest to destination
        LocInfo source = map[rad1d + 1][rad1d + 1];
        LocInfo target = closestLoc;

        // Dijkstra main information finding
        target.distance = 0;
        PriorityQueue<LocInfo> queue = new PriorityQueue<>(Comparator.comparingDouble(o -> o.distance));
        queue.add(target);
        while (!queue.isEmpty()) {
            LocInfo currentLocInfo = queue.poll();

            // If this has already been checked, skip it
            if (currentLocInfo.checked) {
                continue;
            }
            currentLocInfo.checked = true;

            // If this is the source, we're done
            if (currentLocInfo.equals(source)) {
                break;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        // A neighbouring square
                        int nx = currentLocInfo.ix + dx;
                        int ny = currentLocInfo.iy + dy;
                        if (nx >= 0 && nx < map.length && ny >= 0 && ny < map.length) {
                            // That is in the `map`
                            LocInfo neighbor = map[nx][ny];
                            if (!neighbor.checked) {
                                // And has not been checked yet
                                // Compute distance to `neighbor` via `currentLocInfo`
                                double distance = currentLocInfo.distance + neighbor.weight;
                                if (distance < neighbor.distance) {
                                    // It's closes than a potential previous path to it
                                    // Update the distance to get to neighbor and add it to check for further pathing
                                    neighbor.distance = distance;
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dijkstra next step finding
        // For all squares we can move to, check which one gets us closest
        // (taking into account weight, so distance constructed by Dijkstra)
        Direction bestDirection = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.allDirections()) {
            double distance = map[direction.dx + rad1d][direction.dy + rad1d].distance;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDirection = direction;
            }
        }

        // Return direction that gets us closest
        return bestDirection;
    }

    /**
     * Turn a given MapLocation into a flag
     *
     * @param loc              Location the flag translates to
     * @param extraInformation extra information to send along with the flag (int < 2^10)
     * @return integer value flag should take
     */
    public static int LocationToFlag(MapLocation loc, int extraInformation) {
        // warning: does currently not use encryption!
        int x = loc.x;
        int y = loc.y;
        return (extraInformation << (2 * Constants.NBITS)) + ((x & Constants.BITMASK) << Constants.NBITS) + (y & Constants.BITMASK);
    }

    // add 0 as extra information when none is supplied
    public static int LocationToFlag(MapLocation loc) {
        return LocationToFlag(loc, 1000);
    }

    /**
     * Turn a given flag into a MapLocation, does not retrieve extrainformation
     *
     * @param rc   RobotController
     * @param flag flag integer to translate
     * @return MapLocation the flag translates to
     */
    public static MapLocation FlagToLocation(RobotController rc, int flag) {

        int y = flag & Constants.BITMASK;
        int x = (flag >> Constants.NBITS) & Constants.BITMASK;
//        int extraInformation = flag >> (2*NBITS);

        MapLocation currentLocation = rc.getLocation();
        int offsetX128 = currentLocation.x >> Constants.NBITS;
        int offsetY128 = currentLocation.y >> Constants.NBITS;

        MapLocation actualLocation = new MapLocation((offsetX128 << Constants.NBITS) + x, (offsetY128 << Constants.NBITS) + y);

        // You can probably code this in a neater way, but it works
        MapLocation alternative = actualLocation.translate(-(1 << Constants.NBITS), 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(1 << Constants.NBITS, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, -(1 << Constants.NBITS));
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, 1 << Constants.NBITS);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        return actualLocation;
    }

    /**
     * Retrieve possible extra information from flag
     *
     * @param flag flag integer to translate
     * @return integer value of extra information
     */
    public static int FlagToExtraInformation(int flag) {
        return flag >> (2 * Constants.NBITS);
    }


    /**
     * When an enemy HQ is found and we do not already have a flag, set it as flag
     * FUNCTION ASSUMES THAT IT IS ONLY CALLED WHEN AN ENEMY HQ IS FOUND, DOES NOT CHECK AGAIN
     *
     * @param rc  RobotController
     * @param loc location of enemy HQ
     */
    public static void foundEnemyHQ(RobotController rc, MapLocation loc) {
        int ownID = rc.getID();
        try {
            // flag to put up indicating the location found
            int flag = LocationToFlag(loc, Constants.EnemyHQcode);
            rc.setFlag(flag);
        } catch (Exception e) {
            System.out.println("ERROR: Could not get own flag?");
        }
    }

    /**
     * Gets the current flag of the HQ where this unit was built
     *
     * @param rc RobotController
     * @param id Id of home HQ
     * @returns integer value of the flag of the home HQ
     */
    public static int getFlagFromHomeHQ(RobotController rc, int id) {
        int flag;
        try {
            flag = rc.getFlag(id);
        } catch (Exception e) {
            flag = 0;
        }
        return flag;
    }

    /**
     * Generates a random destination based on current location
     *
     * @param rc RobotController
     * @returns location of destination
     */
    public static MapLocation getRandomDestination(RobotController rc) {
        // Choose a random destination to go towards
        int dx = (int) (64 * (2 * Math.random() - 1));
        int dy = (int) (64 * (2 * Math.random() - 1));
        MapLocation destination = rc.getLocation().translate(dx, dy);
//        System.out.println("I'm a " + rc.getType() + " going to " + destination.toString());
        return destination;
    }
}
