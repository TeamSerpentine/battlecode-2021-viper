package viper1.robots;

import battlecode.common.*;
import viper1.utility.Constants;

import static viper1.utility.Constants.ClearCode;
import static viper1.utility.Utility.*;
import static viper1.utility.Utility.pathfindToTarget;

public class Slanderer extends Robot {

    private RobotType ownType;
    private MapLocation lastSlandererLocation = null;
    private Direction initialDirection;

    // (TEMPORARY) variable for when the slanderer turns into a politician
    MapLocation destination;

    public Slanderer(RobotController rc) {
        super(rc);
    }

    @Override
    protected void initialize() {
        enemy = rc.getTeam().opponent();

        // take note of enlightenment center ID for orders
        // get friendly robots on 8 adjacent tiles
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(2, rc.getTeam());

        for (int i = 0; i < nearbyFriendlyRobots.length; i++) {
            if (nearbyFriendlyRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                IDHomeHQ = nearbyFriendlyRobots[i].getID();
                break;
            }
        }

        super.initialize();
        this.initialDirection = randomDirection();
    }

    /**
     * Run 1 round of this robot
     */
    @Override
    protected void run() throws GameActionException {
        ownType = rc.getType();

        if (ownType == RobotType.SLANDERER) {
            // Get all enemy robots in the sense radius
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

            // See if there is a slanderer
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.MUCKRAKER) {
                    lastSlandererLocation = robot.location;
                }
            }

            // If we ever saw a slanderer, run away from it. Else, move in the initially defined direction.
            Direction toMove;
            if (lastSlandererLocation != null) {
                toMove = lastSlandererLocation.directionTo(rc.getLocation());
            } else {
                toMove = initialDirection;
            }

            // Avoid walking into the HQ or other robots and getting stuck
            RobotInfo robotAtMoveLocation = null;
            MapLocation newLocation = rc.getLocation().add(toMove);
            if (rc.canSenseLocation(newLocation)) {
                robotAtMoveLocation = rc.senseRobotAtLocation(newLocation);
            }
            if (robotAtMoveLocation != null) {
                toMove = toMove.rotateRight();
            }


            // avoid standing still next to HQs (TEMPORARY SOLUTION)
            // get friendly robots on 8 adjacent tiles
            //TODO implement a better solution, current solution makes it worse at times
            RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(5, rc.getTeam());
            for (int i = 0; i < nearbyFriendlyRobots.length; i++) {
                if (nearbyFriendlyRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    // get our direction from HQ
                    toMove = nearbyFriendlyRobots[i].getLocation().directionTo(rc.getLocation());
                    break;
                }
            }

            //Move, if we can
            if (rc.canMove(toMove)) {
                rc.move(toMove);
            }

        } else {
            // temporarily basically just put part of the offensive politician ai here

            // offensive AI
//            System.out.println("Offensive AI");

            // Read flag from HQ
            int flagHomeHQ = getFlagFromHomeHQ(rc, IDHomeHQ);

            // calculate the amount of conviction that will be distributed when empowering
            // = current conviction * multiplier - 10
            int equivalentConviction = (int) Math.floor(rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0)) - 10;

            // act upon orders from HQ flag
            if (flagHomeHQ != 0) {
                int extraInformation = FlagToExtraInformation(flagHomeHQ);

                if (extraInformation == Constants.EnemyHQcode) {
                    // HQ is sending an enemy base location!
                    // Let's head there!
                    destination = FlagToLocation(rc, flagHomeHQ);
                } else if (extraInformation == ClearCode) {
                    MapLocation loc = FlagToLocation(rc, flagHomeHQ);
                    // receiving clear code, check if we are currently going there
                    // if we are, set a new random destination and reset to default AI
                    // also reset flag
                    if (destination != null) {
                        if (destination.distanceSquaredTo(loc) == 0) {
                            destination = getRandomDestination(rc);
                            rc.setFlag(0);
                        }
                    }
                }
            }

            // look for enemy HQs
            int senseRadius = rc.getType().sensorRadiusSquared;
            for (RobotInfo robot : rc.senseNearbyRobots(senseRadius, enemy)) {
                // set flag to location of enemy HQ when one is found (and we currently have no flag)
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    foundEnemyHQ(rc, robot.getLocation());
                    break;
                }
            }

            if (rc.isReady()) {
                int actionRadius = rc.getType().actionRadiusSquared;
                RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius);
                for (RobotInfo robot : attackable) {
                    // If one is a neutral base that we can takeover right now, do it
                    if (robot.getTeam() == Team.NEUTRAL && robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                        // check if we can take it over
                        // what radius do we need to hit the EC
                        int rad = rc.getLocation().distanceSquaredTo(robot.getLocation());
                        if (rad <= RobotType.POLITICIAN.actionRadiusSquared){
                            RobotInfo[] inRange = rc.senseNearbyRobots(rad);

                            //actually might sometimes be 1 more than this value
                            int convictionPerRobot = (int) Math.floor((float) equivalentConviction / inRange.length);

                            if (convictionPerRobot >= robot.getConviction()){
                                rc.empower(rad);
                            }
                        }
                    }

                    // TODO implement a better way of attacking units other than just always explode when you see one
                    if (robot.getTeam() == enemy){
                        System.out.println("ENEMY FOUND, EXPLODING");
                        int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                        rc.empower(dist); // empower with just enough radius to hit the enemy and not potentially waste
                    }

                }

            }

            // TODO have a better solution than just moving randomly
            // if we have a destination, go there, else move randomly (temporary solution)
            if (destination != null) {
                // try to pathfind to destination
                Direction direction = pathfindToTarget(rc, destination);
                if (rc.canMove(direction)) {
                    rc.move(direction);
                }
            } else {
                // move randomly
                if (tryMove(randomDirection(), rc)) {
//                    System.out.println("I moved!");
                }
            }


        }
    }
}
