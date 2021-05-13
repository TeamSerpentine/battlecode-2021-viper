package viper1.robots;

import battlecode.common.*;
import viper1.utility.Constants;

import static viper1.utility.Constants.*;
import static viper1.utility.Utility.*;

public class Politician extends Robot {

    // different states this robot could be in
    // each level is to execute a different set of instructions
    // level could change a number of times over the robots lifetime
    // 0 = offensive, seeks out enemy HQs, tries to takeover HQs, takeover/destroy enemy units (default)
    // 1 = defensive, stays near HQ & defends slanderers
    int AI_level;

    int convictionNeutralHQ;

    final int numberOfDefensivePoliticians = 3;

    MapLocation destination;
    MapLocation locationHomeHQ;

    @Override
    protected void initialize() {
        enemy = rc.getTeam().opponent();

        // take note of enlightenment center ID for orders
        // get friendly robots on 8 adjacent tiles
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = 0; i < nearbyFriendlyRobots.length; i++) {
            if (nearbyFriendlyRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                IDHomeHQ = nearbyFriendlyRobots[i].getID();
                locationHomeHQ = nearbyFriendlyRobots[i].getLocation();
                break;
            }
        }

        // set AI level to 0 for offensive behaviour (default)
        // 0 = offensive
        // 1 = conquering neutral HQ
        // 2 = defensive
        AI_level = 0;

        // check if there are enough defensive politicians, if not become one
        // defensive politicians always hold defensive flag, so look for politicians that have that flag
        RobotInfo[] nearbyFriendlyRobotsMaxRange = rc.senseNearbyRobots(RobotType.POLITICIAN.sensorRadiusSquared, rc.getTeam());

        int defensivePoliticianCounter = 0;
        for (int i = 0; i < nearbyFriendlyRobotsMaxRange.length; i++) {
            int id = nearbyFriendlyRobotsMaxRange[i].getID();
            try{
                int flag = rc.getFlag(id);
                int extraInformation = FlagToExtraInformation(flag);
                if (extraInformation == DefensiveCode) {
                    defensivePoliticianCounter++;
                }
            } catch (Exception e){
                // don't do anything if we can't get the flag
                // should always be possible
            }
        }
        if (defensivePoliticianCounter < numberOfDefensivePoliticians){
            // set to defensive AI
            AI_level = 2;
        }


        super.initialize();
    }

    public Politician(RobotController rc) {
        super(rc);
    }

    /**
     * Run 1 round of this robot
     *
     * @throws GameActionException
     */
    @Override
    void run() throws GameActionException {
        // Read flag from HQ
        int flagHomeHQ = getFlagFromHomeHQ(rc, IDHomeHQ);

        // calculate the amount of conviction that will be distributed when empowering
        // = current conviction * multiplier - 10
        int equivalentConviction = (int) Math.floor(rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0)) - 10;

        // if young (so still close to base) see if we can gain influence at HQ by empowering right now
        if (age < 20 && rc.isReady()){ // currently doesn't always go off when it should due to pathfinding bytecode
            MapLocation locHQ = null;
            RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(2, rc.getTeam());
            for (int i = 0; i < nearbyFriendlyRobots.length; i++) {
                if (nearbyFriendlyRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    locHQ = nearbyFriendlyRobots[i].getLocation();
                    break;
                }
            }

            if (locHQ != null){
                // see if we can gain influence by empowering right now
                // 2 times own conviction is super arbitrary
                if (equivalentConviction / nearbyFriendlyRobots.length > rc.getConviction() * 2){
                    int rad = rc.getLocation().distanceSquaredTo(locHQ);
                    try{
                        rc.empower(rad);
                    } catch (Exception e){
                        System.out.println("CANT EMPOWER?");
                    }
                }
            }
        }

        // do different stuff based on AI_level
        if (AI_level == 0) {
            // offensive AI
            System.out.println("Offensive AI");

            // act upon orders from HQ flag
            if (flagHomeHQ != 0) {
                // remove defensive flag if we have it
                int ownFlag = rc.getFlag(rc.getID());
                int extraInformation1 = FlagToExtraInformation(ownFlag);
                if (extraInformation1 == DefensiveCode){
                    rc.setFlag(0);
                }

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
                            AI_level = 0;
                            destination = getRandomDestination(rc);
                            rc.setFlag(0);
                        }
                    }
                } else if (extraInformation <= neutralHQcap) {
                    // receiving location of a neutral base
                    // check if we are strong enough to take it over
                    if (rc.getConviction() >= extraInformation + 10){
                        // we can take it over!
                        // set AI to conquering AI, set destination and save conviction amount
                        AI_level = 1;
                        destination =  FlagToLocation(rc, flagHomeHQ);
                        convictionNeutralHQ = extraInformation;
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

        } else if (AI_level == 1){
            // conquering neutral bases
//            System.out.println("Conquering AI, going to " + destination.x + ", " + destination.y);

            // remove defensive flag if we have it
            int ownFlag = rc.getFlag(rc.getID());
            int extraInformation1 = FlagToExtraInformation(ownFlag);
            if (extraInformation1 == DefensiveCode){
                rc.setFlag(0);
            }

            // when we are ready check if we can take it over right now, if so do it
            if (rc.isReady()){
                int rad = rc.getLocation().distanceSquaredTo(destination);
                if (rad <= RobotType.POLITICIAN.actionRadiusSquared){
                    RobotInfo[] inRange = rc.senseNearbyRobots(rad);

                    //actually might sometimes be 1 more than this value
                    int convictionPerRobot = (int) Math.floor((float) equivalentConviction / inRange.length);
                    System.out.println(inRange.length);
                    if (inRange.length > 0 && convictionPerRobot > convictionNeutralHQ){
                        System.out.println("EMPOWERING NEUTRAL BASE!! with " + convictionPerRobot);
                        rc.empower(rad);
                    }

                    // check if the base has been taken over by the enemy, if it has empower if we can take it over
                    RobotInfo[] inRangeHostile = rc.senseNearbyRobots(rad, enemy);
                    for (RobotInfo robot : inRangeHostile){
                        if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER ){
                            System.out.println("Base has been taken by enemy!");
                            int hqConviction = robot.getConviction();
                            if (hqConviction < equivalentConviction){
                                rc.empower(rad);
                            }
                        }
                    }
                }
            }

            // send clear signal if our target base has already been taken over
            // also reset AI and destination
            RobotInfo[] friendliesInRange = rc.senseNearbyRobots(RobotType.POLITICIAN.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo robot : friendliesInRange){
                RobotType type = robot.getType();
                if (type == RobotType.ENLIGHTENMENT_CENTER){
                    if (destination.distanceSquaredTo(robot.getLocation()) == 0){
                        int flag = LocationToFlag(destination, ClearCode);
                        rc.setFlag(flag);
                        AI_level = 0;
                        destination = getRandomDestination(rc);
                    }
                }
            }


            // head to destination
            if (destination != null) {
                // try to pathfind to destination
                Direction direction = pathfindToTarget(rc, destination);
                if (rc.canMove(direction)) {
                    rc.move(direction);
                }
            } else {
                // we should always have a destination in this AI_level
                // so reset AI_level
                AI_level = 0;
            }
        } else if (AI_level == 2){
            // defensive AI

            // set to defensive flag if we don't have it already
            int ownFlag = rc.getFlag(rc.getID());
            int extraInformation1 = FlagToExtraInformation(ownFlag);
            if (extraInformation1 != DefensiveCode){
                int flag = LocationToFlag(rc.getLocation(), DefensiveCode);
                rc.setFlag(flag);
            }

            // stay near base, every round head toward a random point close to HQ
            int dx = (int) (4 * (2 * Math.random() - 1));
            int dy = (int) (4 * (2 * Math.random() - 1));
            destination = locationHomeHQ.translate(dx, dy);

            // if we can conquer a base, change to conquering AI
            int extraInformation = FlagToExtraInformation(flagHomeHQ);
            if (extraInformation <= neutralHQcap) {
                // receiving location of a neutral base
                // check if we are strong enough to take it over
                if (rc.getConviction() >= extraInformation + 10) {
                    // we can take it over!
                    // set AI to conquering AI, set destination and save conviction amount
                    AI_level = 1;
                    destination = FlagToLocation(rc, flagHomeHQ);
                    convictionNeutralHQ = extraInformation;
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
                        int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                        rc.empower(dist); // empower with just enough radius to hit the enemy and not potentially waste
                    }

                }

            }

            // try to pathfind to destination
            Direction direction = pathfindToTarget(rc, destination);
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
        } else {
            // something went wrong with AI_levels, resetting to default
            AI_level = 0;
        }


        // if our flag is the same home HQ, set it to 0 to prevent infinite cycles
        if (flagHomeHQ == rc.getFlag(rc.getID())){
            rc.setFlag(0);
        }
    }
}
