package viper1.robots;

import battlecode.common.*;
import viper1.utility.Constants;
import viper1.utility.ProductionItem;

import static viper1.utility.Constants.*;
import static viper1.utility.Utility.*;
import static viper1.utility.ProductionItem.*;

import java.util.ArrayList;

public class EnlightenmentCenter extends Robot {
    // production queue arraylist
    ArrayList<ProductionItem> productionQueue;

    // variables for use with the production queue
    int turnsNoEnemiesInRange;
    int slandererCountdown;
    int slandererCooldown;
    int politicianCountdown;
    int politicianCooldown;
    int muckrakerCountdown;
    int muckrakerCooldown;
    double ownPassability;
    MapLocation ownLocation;

    // list to store all known friendly robot IDs (no duplicates)
    ArrayList<Integer> friendlyRobotIDs;

    // variables for reading (part of) the friendly robot flags
    int flagReadStart = 0; //where we started reading flags last round
    int flagReadEnd = 0; //where we left off reading flags last round

    // variable to keep track of what flag to show
    int flagDisplayCounter = 0;

    public EnlightenmentCenter(RobotController rc) {
        super(rc);
    }

    @Override
    protected void initialize() {
        enemy = rc.getTeam().opponent();
        super.initialize();
        ownLocation = rc.getLocation();
        // start off with no known enemy base location
        currentEnemyHQ = null;
        neutralHQFlags = new ArrayList<Integer>();

        try {
            ownPassability = rc.sensePassability(ownLocation);
            slandererCooldown = (int) Math.round(25 / ownPassability);
        } catch (Exception e) {
            ownPassability = 0.5; // arbitrarily chosen, should never be used
            System.out.println("Error getting own passibility of " + rc.getID());
            slandererCooldown = 10;
        }
        turnsNoEnemiesInRange = 0;
        slandererCountdown = 25; // countdown at start
        politicianCooldown = (int) Math.round(30 / ownPassability);
        politicianCountdown = 100; // countdown at start
        muckrakerCooldown = (int) Math.round(30 / ownPassability);
        muckrakerCountdown = 100; // countdown at start


        // build a muckraker on every side that we can't see the border on to scout
        this.productionQueue = new ArrayList<ProductionItem>();
        // check on which side we should scout (we cant see the border on that side there)
        // there is probably a more efficient way to do this but this works
        // NORTH
        if (rc.canSenseLocation(ownLocation.translate(0, 6))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.NORTH);
            productionQueue.add(robot);
        }
        // NORTHEAST
        if (rc.canSenseLocation(ownLocation.translate(4, 4))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.NORTHEAST);
            productionQueue.add(robot);
        }
        // EAST
        if (rc.canSenseLocation(ownLocation.translate(6, 0))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.EAST);
            productionQueue.add(robot);
        }
        // SOUTHEAST
        if (rc.canSenseLocation(ownLocation.translate(4, -4))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.SOUTHEAST);
            productionQueue.add(robot);
        }
        // SOUTH
        if (rc.canSenseLocation(ownLocation.translate(0, -6))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.SOUTH);
            productionQueue.add(robot);
        }
        // SOUTHWEST
        if (rc.canSenseLocation(ownLocation.translate(-4, -4))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.SOUTHWEST);
            productionQueue.add(robot);
        }
        // WEST
        if (rc.canSenseLocation(ownLocation.translate(-6, 0))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.WEST);
            productionQueue.add(robot);
        }
        // NORTHWEST
        if (rc.canSenseLocation(ownLocation.translate(-4, 4))) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER, 1, Direction.NORTHWEST);
            productionQueue.add(robot);
        }

        // check if there is a neutral base in range that we could takeover
        for (RobotInfo robot : rc.senseNearbyRobots(RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared, Team.NEUTRAL)) {
            int conviction = robot.getConviction();
            MapLocation loc = robot.getLocation();
            int flag = LocationToFlag(loc, conviction);
            neutralHQFlags.add(flag);

            // add politician of enough strength to queue
            Direction dir = ownLocation.directionTo(loc);
            if (conviction == 0) {
                conviction = 1; // so we can keep the flag 0 as default
            }
            ProductionItem r = new ProductionItem(RobotType.POLITICIAN, conviction + 20, dir);
            productionQueue.add(r);
        }

        // create actual list to keep IDs of friendly robots
        friendlyRobotIDs = new ArrayList<Integer>();
    }

    /**
     * Run 1 round of this robot
     *
     * @throws GameActionException
     */
    @Override
    void run() throws GameActionException {
        int currentInfluence = rc.getInfluence();
        int roundNumber = rc.getRoundNum();
        int currentVotes = rc.getTeamVotes();
        boolean changedFlagThisRound = false;

        // See what the minimum influence the HQ needs to prevent politicians from taking over the HQ
        // Sum the total influence of all enemy policitians within 9 tiles
        RobotInfo[] enemyPoliticiansInRange = rc.senseNearbyRobots(9, enemy);
        for (RobotInfo robot : enemyPoliticiansInRange) {
            MapLocation robotLocation = robot.getLocation();
            int dist = ownLocation.distanceSquaredTo(robotLocation);
            if (robot.type == RobotType.POLITICIAN) {
                currentInfluence-=robot.influence;
                if(currentInfluence<0){
                    currentInfluence=0;
                }
            }
        }


        // Voting
        // extremely simple voting strategy to vote 5% of influence if we have more than 350 or it is later than turn 500
        if (currentVotes <= 750 && (currentInfluence > 350 || roundNumber > 500)) {
            int amount = (int) Math.round(currentInfluence * 0.05);
            if (amount > 0){
                rc.bid(amount);
            }
            currentInfluence -= amount; //update current influence
        }


        // Communication

        // look for our flags, until we have 10k bytecode left (arbitrarily chosen amount) or seen them all
        // if we have gone through all flags last round, reset, else go on where we left off
        if (flagReadEnd >= friendlyRobotIDs.size() - 1) {
            // we looped through entire array last time
            flagReadStart = 0;
            flagReadEnd = 0;
        } else {
            // we didn't loop through entire array last time
            flagReadStart = flagReadEnd + 1;
            flagReadEnd = flagReadEnd + 1;
        }

        int flagReadCounter = 0;

//        System.out.println(friendlyRobotIDs);

        while (Clock.getBytecodesLeft() > 10000 && flagReadEnd < friendlyRobotIDs.size()) {
            int index = flagReadStart + flagReadCounter;
            int id = friendlyRobotIDs.get(index);
//            System.out.println("Checking on " + id);
            if (rc.canGetFlag(id)) {
                int flag = rc.getFlag(id);
//                System.out.println("it has flag " + flag);
                if (flag != 0) {
                    // if we get here we found a (friendly) robot with a non-zero flag
                    int extraInformation = FlagToExtraInformation(flag);
                    if (extraInformation == Constants.EnemyHQcode) {
                        // we found an enemy HQ!
                        currentEnemyHQ = FlagToLocation(rc, flag);
                    } else if (extraInformation == ClearCode) {
                        // go through known locations, clear if the locations match
                        MapLocation loc = FlagToLocation(rc, flag);

                        // check if the clear flag is for our current enemy base, if we have one
                        if (currentEnemyHQ != null){
                            if (currentEnemyHQ.distanceSquaredTo(loc) == 0){
                                currentEnemyHQ = null;
                            }
                        }

                        //check if the clearsignal is for any of the known neutral HQs
                        for (int i = 0; i < neutralHQFlags.size(); i++){
                            MapLocation neutralLoc = FlagToLocation(rc, neutralHQFlags.get(i));
                            if (neutralLoc.distanceSquaredTo(ownLocation) == 0){
                                neutralHQFlags.remove(i);
                                break;
                            }
                        }

                        // show clear flag for units and make sure the flag doesn't get changed later this round
                        rc.setFlag(flag);
                        changedFlagThisRound = true;
                        break;
                    } else if (extraInformation <= neutralHQcap) {
                        // we found a neutral base!
                        // add it to the list if it's not a duplicate
                        if (!neutralHQFlags.contains(flag)){
                            neutralHQFlags.add(flag);
                            System.out.println("added " + flag);

                            // queue a politician that is strong enough if it's new
                            MapLocation loc = FlagToLocation(rc, flag);
                            Direction dir = ownLocation.directionTo(loc);
                            ProductionItem robot = new ProductionItem(RobotType.POLITICIAN, extraInformation + 20, dir);
                            productionQueue.add(robot);
                        }
                    }
                }
            } else {
                //TODO note dead (flag doesnt return) units from list of known IDs
                // take note of dead ones, remove them when resetting
            }

            flagReadCounter++;
            flagReadEnd++;
        }

        // display a flag, if we haven't already this round
        if (!changedFlagThisRound) {
            // cycle through current enemy base and known neutral HQs
            ArrayList<Integer> flags = new ArrayList<Integer>();

            if (currentEnemyHQ != null) {
                int enemyHQflag = LocationToFlag(currentEnemyHQ, EnemyHQcode);
                flags.add(enemyHQflag);
            }

            // add all the neutral flags to the options
            flags.addAll(neutralHQFlags);

            // check if the counter should loop over
            if (flagDisplayCounter >= flags.size()){
                flagDisplayCounter = 0;
            }

            // display a flag if we have one to display
            if (flags.size() > 0){
                rc.setFlag(flags.get(flagDisplayCounter));
                flagDisplayCounter++;
            }
        }


        // Unit building, using a queue

        // if there are no enemies nearby for a while, add a slanderer
        // if there are enemies nearby speed up politician production
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        turnsNoEnemiesInRange++;
        for (RobotInfo robot : robots) {
            MapLocation robotLocation = robot.getLocation();
            int dist = ownLocation.distanceSquaredTo(robotLocation);
            if (robot.type == RobotType.MUCKRAKER || (robot.type == RobotType.POLITICIAN && dist <= 30)) {
                turnsNoEnemiesInRange = 0;

                //hurry up politician production
                politicianCountdown -= 4;
            }
        }

        // when exponential influence gain is possible, add a politician to the front of the queue
        if (rc.getEmpowerFactor(rc.getTeam(), 10) > 2){ // empower factor after 10 turns because of 10 turn delay politician
            ProductionItem robot = new ProductionItem(RobotType.POLITICIAN);
            productionQueue.add(0, robot);
        }

        // every 250 turns increase slanderer cooldown and also decrease politician cooldown
        // amounts arbitrarily chosen
        if (roundNumber % 250 == 0 && roundNumber > 0) {
//            slandererCooldown = slandererCooldown + 2;
            politicianCooldown = politicianCooldown - 3;
        }

        // every slandererCooldown turns add a slanderer to the queue
        slandererCountdown--;
        if (slandererCountdown <= 0 && currentInfluence > 20) {
            ProductionItem robot = new ProductionItem(RobotType.SLANDERER);
            productionQueue.add(robot);
            slandererCountdown = slandererCooldown; // reset countdown
        }

        // every politicianCooldown turns add a politician to the queue
        politicianCountdown--;
        if (politicianCountdown <= 0 && currentInfluence > 20) {
            ProductionItem robot = new ProductionItem(RobotType.POLITICIAN);
            productionQueue.add(robot);
            politicianCountdown = politicianCooldown; // reset countdown
            System.out.println("Adding politician!");
        }

        // every muckrakerCooldown turns add a muckraker to the queue
        muckrakerCountdown--;
        if (muckrakerCountdown <= 0) {
            ProductionItem robot = new ProductionItem(RobotType.MUCKRAKER);
            productionQueue.add(robot);
            muckrakerCountdown = muckrakerCooldown; // reset countdown
        }

        // when there hasn't been an enemy within range for 8 turns add a slanderer to the queue
        if (turnsNoEnemiesInRange >= 8 && currentInfluence > 20) {
            ProductionItem robot = new ProductionItem(RobotType.SLANDERER);
            productionQueue.add(robot);
            turnsNoEnemiesInRange = 0; // reset counter
        }

        // if the queue is empty, build a muckraker if it's early, after turn 400 produce politicians (if we have enough influence)
        if (productionQueue.size() == 0) {
            ProductionItem robot;
            if (roundNumber < 400 || currentInfluence < 100) {
                robot = new ProductionItem(RobotType.MUCKRAKER);
            } else {
                robot = new ProductionItem(RobotType.POLITICIAN);
            }
            productionQueue.add(robot);
        }

        // try to build the first item in the queue, remove it from the list if we build it
        ProductionItem toBuild = productionQueue.get(0);
        RobotType type = toBuild.getType();

        if (rc.isReady()) {
            // try to spawn on specified spot, else any spot

            // if we can't build current item in queue, move it to the back of the queue
            // do this until we can build it, max 5 times
            // if that happens add strength 1 muckraker to front of queue instead
            int influence = 1;
            int swapCounter = 0;
            boolean swapped;
            do {
                swapped = false;
                // calc how much influence to use on unit
                if (toBuild.getInfluence() > 0) {
                    influence = toBuild.getInfluence();
                } else {
                    // influence was not set, calculate an appropriate amount based on type
                    if (type == RobotType.POLITICIAN) {
                        // politicians
                        //TODO implement better system for determining influence used here
                        influence = (int) Math.round(4 * (currentInfluence / 5.0));

                        // cap at 1000
                        if (influence > 999) {
                            influence = 1000;
                        }
                    } else if (type == RobotType.SLANDERER) {
                        // slanderers
                        //TODO implement better system for determining influence used here
                        influence = (int) Math.round(4 * (currentInfluence / 5.0));

                        // cap at 1000
                        if (influence > 999) {
                            influence = 1000;
                        }
                    } else {
                        // muckrakers
                        // 2 % of current influence
                        influence = (int) Math.floor((currentInfluence / 50.0));

                        // cap at 500
                        if (influence > 499) {
                            influence = 500;
                        }
                    }
                }

                if (currentInfluence < influence){
                    ProductionItem temp = productionQueue.get(0);
                    productionQueue.remove(0);
                    productionQueue.add(temp);
                    swapped = true;
                    swapCounter++;
                }
            } while (swapped && swapCounter < 5);
            if (swapCounter >= 5){
                // swapped 5 times, adding 1 strength muckraker
                ProductionItem muck = new ProductionItem(RobotType.MUCKRAKER, 1);
                productionQueue.add(0, muck);
            }


            Direction d = toBuild.getDirection();
            if (d != null) {
                if (rc.canBuildRobot(type, toBuild.getDirection(), influence)) {
                    rc.buildRobot(type, toBuild.getDirection(), influence);
                    productionQueue.remove(0);
                    // add newly built robot's ID to known IDs
                    RobotInfo[] nearbyFriendlyBots = rc.senseNearbyRobots(2, rc.getTeam());
                    for (RobotInfo robot : nearbyFriendlyBots) {
                        if (ownLocation.directionTo(robot.getLocation()) == d){
                            int id = robot.getID();
                            friendlyRobotIDs.add(id);
                            break;
                        }
                    }
                } else {
                    // cant build on specified spot
                    // pick an open spot to spawn on
                    for (Direction dir : DIRECTIONS) {
                        if (rc.canBuildRobot(type, dir, influence)) {
                            rc.buildRobot(type, dir, influence);
                            productionQueue.remove(0);

                            // add newly built robot's ID to known IDs
                            for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
                                if (ownLocation.directionTo(robot.getLocation()) == dir){
                                    int id = robot.getID();
                                    friendlyRobotIDs.add(id);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            } else {
                // no direction specified
                // pick an open spot to spawn on
                for (Direction dir : DIRECTIONS) {
                    if (rc.canBuildRobot(type, dir, influence)) {
                        rc.buildRobot(type, dir, influence);
                        productionQueue.remove(0);

                        // add newly built robot's ID to known IDs
                        for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
                            if (ownLocation.directionTo(robot.getLocation()) == dir){
                                int id = robot.getID();
                                friendlyRobotIDs.add(id);
                                break;
                            }
                        }
                        break;
                    }
                }
            }

        }

        // Do not allow more than 5 of each unit in the queue after building
        //TODO implement actual solution, currently just removes the last items when array gets too long
        while (productionQueue.size() > 15) {
            int lastIndex = productionQueue.size() - 1;
            productionQueue.remove(lastIndex);
        }

        //logging production queue
        for (int i = 0; i < productionQueue.size(); i++){
            ProductionItem b = productionQueue.get(i);
            RobotType typ = b.getType();
            int inf = b.getInfluence();
            Direction d = b.getDirection();
            System.out.println(i + " building " + typ + " at " + inf + " inf at " + d);
        }


//        System.out.println("Printing friendly IDs! for " + rc.getID());
//        for (Integer i : friendlyRobotIDs) {
//            if (rc.canGetFlag(i)){
//                System.out.println("Robot with ID: " + i + " has flag " + rc.getFlag(i));
//            } else {
//                System.out.println("Robot with ID: " + i + " has died");
//            }
//        }
    }
}
