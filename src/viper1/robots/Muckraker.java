package viper1.robots;

import battlecode.common.*;
import scala.Int;
import viper1.utility.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static viper1.utility.Utility.*;

public class Muckraker extends Robot {

    MapLocation destination;

    // TEMPORARY SOLUTION to stop muckrakers from piling up -> ignore blocking order for some time
    // flags to ignore
    ArrayList<Integer> flagsToIgnore;
    int flagResetTime; // reset ignored flags every so many turns

    // different states this robot could be in
    // each level is to execute a different set of instructions
    // level could change a number of times over the robots lifetime
    // 0 = scouting / hunting slanderers (default)
    // 1 =                                          defunct and unused right now
    // 2 = heading to enemy base and blocking it
    int AI_level;

    public Muckraker(RobotController rc) {
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
//        System.out.println("I was built by " + RobotIDOfWhoBuiltMe);

        // set AI level to 0 for standard scouting/chasing behaviour (default)
        AI_level = 0;

        // get initial orders from HQ flag
        // Read flag from HQ
        int flagHomeHQ = getFlagFromHomeHQ(rc, IDHomeHQ);
//        System.out.println("HOMEFLAG: " + flagHomeHQ + " from hq with id: " + IDHomeHQ);

        // change destination depending on where we were spawned relative to HQ
        int touchRadius = 2;
        for (RobotInfo robot : rc.senseNearbyRobots(touchRadius, rc.getTeam())) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                // get our direction from HQ
                Direction dir = robot.getLocation().directionTo(rc.getLocation());

                // head in that direction
                destination = rc.getLocation().translate(dir.dx * 63, dir.dy * 63);
                break;
            }
        }

        // act upon orders from HQ flag
        if (flagHomeHQ != 0) {
            int extraInformation = FlagToExtraInformation(flagHomeHQ);

            if (extraInformation == Constants.EnemyHQcode) {
                // HQ is sending an enemy base location!
                // Let's head there!
                MapLocation loc = FlagToLocation(rc, flagHomeHQ);
                destination = loc;
                // change to base blocking AI
                AI_level = 2;
            }
        }

        flagsToIgnore = new ArrayList<Integer>();
        flagResetTime = 250;

        super.initialize();

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
        int senseRadius = rc.getType().sensorRadiusSquared;
        int actionRadius = rc.getType().actionRadiusSquared;
        int detectionRadius = rc.getType().detectionRadiusSquared;

        // Destroy a slanderer if one is in action range
        // TODO if there are multiple targets, pick the best one
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
//                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }

        // execute different instructions based on AI_level
        if (AI_level == 0) {
            // scouting / hunting slanderers (default)

            // TODO implement chasing slanderers

            // look for neutral HQs
            for (RobotInfo robot : rc.senseNearbyRobots(senseRadius, Team.NEUTRAL)) {
                int conviction = robot.getConviction();
                if (conviction == 0) {
                    conviction = 1; // so we can keep the flag 0 as default
                }
                // if we don't already have that flag, put on the flag indicating the neutral HQ
                int flag = LocationToFlag(robot.getLocation(), conviction);
                if (rc.getFlag(rc.getID()) != flag){
                    rc.setFlag(flag);
                }
                break;
            }

            // look for enemy HQs
            for (RobotInfo robot : rc.senseNearbyRobots(senseRadius, enemy)) {
                // set flag to location of enemy HQ when one is found (and we currently have no flag)
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    foundEnemyHQ(rc, robot.getLocation());
                }
            }

            // act upon orders from HQ flag
            if (flagHomeHQ != 0 && !flagsToIgnore.contains(flagHomeHQ)) {
                int extraInformation = FlagToExtraInformation(flagHomeHQ);
                if (extraInformation == Constants.EnemyHQcode) {
                    // HQ is sending an enemy base location!
                    // Let's head there!
                    destination = FlagToLocation(rc, flagHomeHQ);
                    // change to base blocking AI
                    AI_level = 2;
                } else if(extraInformation == Constants.ClearCode){
                    // if clear signal is sent, check if we have the location it's talking about
                    // if so, clear our flag
                    MapLocation loc = FlagToLocation(rc, flagHomeHQ);
                    int myFlag = rc.getFlag(rc.getID());
                    MapLocation myFlagLocation = FlagToLocation(rc, myFlag);
                    if (loc.distanceSquaredTo(myFlagLocation) == 0){
                        rc.setFlag(0);
                    }
                }
            }

            // if we have a destination, try to go there, if we're ready
            if (rc.isReady()) {
                if (destination != null) {
                    Direction direction = pathfindToTarget(rc, destination);
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                    }
                } else {
                    // if we don't have a destination, generate a (random) new one
                    destination = getRandomDestination(rc);
                }
            }


            //TODO implement a better pseudo-random searching algorithm
            // choose a new destination every so often (temporary solution)
            if (age > 200) {
                double p = Math.random();
                if (p < 0.02) {
                    destination = getRandomDestination(rc);
                }
            }

            //TODO add detecting when stuck in the same area for some time -> changing destination

            // TODO change AI/destination when found border
        } else if (AI_level == 2) {
            // heading to enemy base and blocking it

            // look for neutral HQs
            for (RobotInfo robot : rc.senseNearbyRobots(senseRadius, Team.NEUTRAL)) {
                int conviction = robot.getConviction();
                if (conviction == 0) {
                    conviction = 1; // so we can keep the flag 0 as default
                }
                // if we don't already have that flag, put on the flag indicating the neutral HQ
                int flag = LocationToFlag(robot.getLocation(), conviction);
                if (rc.getFlag(rc.getID()) != flag){
                    rc.setFlag(flag);
                }
                break;
            }

            // look for enemy HQs and slanderers
            for (RobotInfo robot : rc.senseNearbyRobots(senseRadius, enemy)) {
                // set flag to location of enemy HQ when one is found (and we currently have no flag)
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    foundEnemyHQ(rc, robot.getLocation());
                }

                // When an enemy slanderer is found, set it as new destination to track it down
                if (robot.type == RobotType.SLANDERER){
                    destination = robot.getLocation();
                }
            }

            // Try to move to the destination if we have one and can act
            if (rc.isReady()) {
                if (destination != null) {
                    Direction direction = pathfindToTarget(rc, destination);

                    // dont move if currently blocking enemy HQ
                    boolean blocking = false;
                    int touchRadius = 2;
                    for (RobotInfo robot : rc.senseNearbyRobots(touchRadius, enemy)) {
                        if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                            blocking = true;
                            break;
                        }
                    }

                    // if there is an empty spot between the edge and enemy HQ, go there
                    //TODO get directions of 2 tiles that both border us and hq

                    //TODO check if one tile farther away from the hq in that direction is the border

                    //TODO if it is, move there if it's empty

                    if (!blocking && rc.canMove(direction)) {
                        rc.move(direction);
                    }

                    // detect when all tiles around enemy HQ have already been taken by  units
                    // if so then switch back to scouting and ignore this order for 100 turns
                    // first check if target enemy hq is within sensor range
                    RobotInfo[] withinSensors = rc.senseNearbyRobots(senseRadius, enemy);
                    boolean targetHQWithinSenors = false;
                    for (RobotInfo robot : withinSensors){
                        if (robot.getLocation().distanceSquaredTo(destination) == 0){
                            // target HQ is within sensors
                            targetHQWithinSenors = true;
                            break;
                        }
                    }
                    if (targetHQWithinSenors){
                        MapLocation[] detectedRobots = rc.detectNearbyRobots(detectionRadius);
                        List listDetectedRobots = Arrays.asList(detectedRobots);
                        int spotsFilled = 0;

                        // check all the spots around the HQ, if they don't exist count them as filled
                        if (listDetectedRobots.contains(destination.translate(0,1))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(1,1))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(1,0))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(1,-1))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(0,-1))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(-1,-1))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(-1,0))){
                            spotsFilled++;
                        }
                        if (listDetectedRobots.contains(destination.translate(-1,1))){
                            spotsFilled++;
                        }

                        // if not blocking yourself, switch logic
                        if (spotsFilled == 8 && !blocking){
                            System.out.println("ALL SPOTS FILLED AROUND ENEMY HQ");
                            // all spots already filled, switch back to scouting AI
                            AI_level = 0;
                            destination = getRandomDestination(rc);
                            // ignore the command to go to this destination for 100 turns
                            int ownFlag = rc.getFlag(rc.getID());
                            flagsToIgnore.add(ownFlag);
                        }

                    }


                    // detect when the HQ has been taken over and give signal of it
                    for (RobotInfo robot : rc.senseNearbyRobots(touchRadius, rc.getTeam())) {
                        if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                            MapLocation loc = robot.getLocation();
                            if (loc.distanceSquaredTo(destination) == 0){
                                // base has been taken over!
                                System.out.println("Enemy base conquered!");
                                int flag = LocationToFlag(loc, Constants.ClearCode);
                                rc.setFlag(flag);

                                // reset to scouting AI
                                AI_level = 0;
                                destination = getRandomDestination(rc);
                            }
                        }
                    }
                } else {
                    // we apparently don't have a destination so go back to scouting
                    AI_level = 0;
                    destination = getRandomDestination(rc);
                }
            }
        }

        // reset ignored flags if it's been a while since last reset
        if (age % flagResetTime == 0){
            flagsToIgnore.clear();
        }
    }
}
