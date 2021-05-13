package viper1.robots;

import battlecode.common.*;

import java.util.ArrayList;

public abstract class Robot {

    /**
     * Number of rounds this robot has lived for / performed
     */
    int age = 0;

    // Id of robot who built this robot (for units)
    int IDHomeHQ;

    // variable to hold the enemy team
    Team enemy;

    // variables for handling multiple flags and locations
    MapLocation currentEnemyHQ;
    ArrayList<Integer> neutralHQFlags; //conviction base + location

    /**
     * This is the RobotController object. You use it to perform actions from this robot,
     * and to get information on its current status.
     */
    RobotController rc;

    /**
     * When a robot is created, it will run forever (else it dies)
     *
     * @param rc RobotController game controller object
     */
    public Robot(RobotController rc) {
        this.rc = rc;
        System.out.println("Hello world! Initializing.");
        this.initialize();
        System.out.println("Entering main loop.");
        // Try/catch block (attempts to) stop(s) exceptions from making the bot freeze
        while (true) {
            try {
                // Run 1 round of actions for this robot
                this._run();

                // Make the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function called when the robot is spawned
     */
    protected void initialize() {

    }

    /**
     * Run 1 round, including pre/post actions performed by this abstract class
     *
     * @throws GameActionException
     */
    private void _run() throws GameActionException {
        age++;
        this.run();
    }

    /**
     * Run 1 round of this robot
     *
     * @throws GameActionException
     */
    abstract void run() throws GameActionException;

}
